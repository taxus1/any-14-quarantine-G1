package com.somepro.infrastructure.persistence.support;

import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

/**
 * 编号取号器（基础设施层共享）。
 *
 * 为什么要它：直接「SELECT MAX(no) + 1」在并发下会给不同连接算出同一个号；
 * 靠唯一键 + 重试也救不回来 —— InnoDB（默认 REPEATABLE READ）同一事务内的一致性快照
 * 会让重试里的 MAX() 仍是旧值，于是反复撞同一个号。
 *
 * 方案：用数据库的<b>命名锁</b>（MySQL/MariaDB 内置的 GET_LOCK，无需建表、不动业务表结构）
 * 把「查最大号 → 插入」串行化，并放进<b>同一个短事务</b>里：
 * <pre>
 *   开事务（绑定一条连接）
 *     GET_LOCK('farm-no-2026', 10)   -- 跨连接互斥
 *     SELECT MAX(no) ...             -- 此时一定是最新值
 *     INSERT ...                     -- 新号落库
 *   提交                              -- 新号对后来者可见
 *     RELEASE_LOCK(...)              -- 放行下一个取号者
 * </pre>
 * 关键是 GET_LOCK/MAX/INSERT 必须在<b>同一条连接</b>上（Spring 事务把连接绑到当前线程，
 * MyBatis 的每次操作都会复用它）；锁在提交之后才释放，保证后来者 MAX 得到的是已提交的新号。
 *
 * 命名锁保证号不撞；业务行唯一键仍是最终防线，二者叠加。
 */
@Component
public final class BusinessNumberAllocator {

    /** 等锁最多 10 秒，避免异常情况下无限挂起。 */
    private static final int LOCK_TIMEOUT_SECONDS = 10;

    private final NumberLockMapper numberLockMapper;
    private final TransactionTemplate transactionTemplate;

    public BusinessNumberAllocator(NumberLockMapper numberLockMapper,
                                   PlatformTransactionManager transactionManager) {
        this.numberLockMapper = numberLockMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        // 取号事务用 READ COMMITTED：拿到命名锁后 SELECT MAX 必须读到前者已提交的新号。
        // 默认 REPEATABLE READ 的一致性快照在事务开启时建立，等待锁的后来者会读到旧值而撞号。
        this.transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    }

    /**
     * 在「短事务 + 命名锁」保护下完成取号并落库。
     *
     * @param lockName 锁名（按业务 + 年份区分，如 farm-no-2026）
     * @param action   已在锁内的动作：读当前最大号、生成新号、插入并返回结果
     */
    public <T> T allocate(String lockName, Supplier<T> action) {
        // 事务边界：开始即向连接池借一条连接并绑定到当前线程，结束归还
        return transactionTemplate.execute(status -> {
            Integer acquired = numberLockMapper.getLock(lockName, LOCK_TIMEOUT_SECONDS);
            if (!Integer.valueOf(1).equals(acquired)) {
                throw new IllegalStateException("取号锁等待超时/失败：" + lockName + "（返回 " + acquired + "）");
            }
            try {
                // MAX() 与 insert 都在当前事务的同一条连接上
                return action.get();
            } finally {
                // 还在事务里释放锁；随后 execute 返回时事务才提交，新号对后来者可见
                numberLockMapper.releaseLock(lockName);
            }
        });
    }
}
