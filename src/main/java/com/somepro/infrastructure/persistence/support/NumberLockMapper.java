package com.somepro.infrastructure.persistence.support;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 数据库命名锁 Mapper（基础设施层）。
 *
 * 仅用于编号取号的跨连接互斥，不对应任何业务表：
 * - GET_LOCK(name, timeout)：1 拿到锁 / 0 等待超时 / null 出错；
 * - RELEASE_LOCK(name)：1 已释放 / 0 本连接没持有。
 * MySQL 与 MariaDB 均内置，无需建表、不动业务表结构。
 */
public interface NumberLockMapper {

    @Select("SELECT GET_LOCK(#{name}, #{timeout})")
    Integer getLock(@Param("name") String name, @Param("timeout") int timeoutSeconds);

    @Select("SELECT RELEASE_LOCK(#{name})")
    Integer releaseLock(@Param("name") String name);
}
