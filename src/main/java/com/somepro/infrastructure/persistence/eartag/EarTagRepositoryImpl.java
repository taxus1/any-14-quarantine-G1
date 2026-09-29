package com.somepro.infrastructure.persistence.eartag;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.pagehelper.PageHelper;
import com.somepro.domain.eartag.model.EarTag;
import com.somepro.domain.eartag.model.EarTagPageQuery;
import com.somepro.domain.eartag.repository.EarTagRepository;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.infrastructure.persistence.eartag.converter.EarTagPoConverter;
import com.somepro.infrastructure.persistence.eartag.po.EarTagPO;
import com.somepro.infrastructure.persistence.support.BusinessNumberAllocator;
import com.somepro.infrastructure.persistence.support.JdbcScheduler;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 耳标仓储适配器：用 MyBatis-Plus 实现领域仓储端口（基础设施层）。
 *
 * - 所有 DB 调用经 {@link JdbcScheduler#blocking} 切到 boundedElastic；
 * - ID 应用侧雪花分配；耳标号 ET-yyyy-###### 在本层按年取号，含已核销记录一起计数，号不回收；
 * - 软删除交给 @TableLogic；分页用 PageHelper，名单按 id 升序，翻页稳定不重样。
 */
@Repository
public class EarTagRepositoryImpl implements EarTagRepository {

    /** 编号前缀/位数 */
    private static final String NO_PREFIX = "ET-";
    private static final int NO_SEQ_WIDTH = 6;
    /** 取号命名锁前缀（与年份拼在一起，不同年互不阻塞） */
    private static final String NO_LOCK_PREFIX = "ear-tag-no-";

    private final EarTagMapper earTagMapper;
    private final BusinessNumberAllocator numberAllocator;

    public EarTagRepositoryImpl(EarTagMapper earTagMapper, BusinessNumberAllocator numberAllocator) {
        this.earTagMapper = earTagMapper;
        this.numberAllocator = numberAllocator;
    }

    @Override
    public Mono<EarTag> save(EarTag earTag) {
        if (earTag.getId() == null) {
            // 新建：取号 + 插入放在「短事务 + 命名锁」内，并发下不撞号
            return JdbcScheduler.blocking(() -> {
                int year = LocalDateTime.now().getYear();
                String lockName = NO_LOCK_PREFIX + year;
                return numberAllocator.allocate(lockName, () -> {
                    EarTagPO po = EarTagPoConverter.toPo(earTag);
                    po.setId(IdUtil.getSnowflakeNextId());
                    po.setTagNo(nextTagNo(year));
                    earTag.setId(po.getId());
                    earTag.setTagNo(po.getTagNo());
                    earTagMapper.insert(po);
                    return EarTagPoConverter.toDomain(po);
                });
            });
        }
        return JdbcScheduler.blocking(() -> {
            EarTagPO po = EarTagPoConverter.toPo(earTag);
            earTagMapper.updateById(po);
            return EarTagPoConverter.toDomain(po);
        });
    }

    @Override
    public Mono<EarTag> findById(Long id) {
        return JdbcScheduler.blocking(() -> {
            EarTagPO po = earTagMapper.selectById(id);
            return po == null ? null : EarTagPoConverter.toDomain(po);
        });
    }

    @Override
    public Mono<PageResult<EarTag>> page(int pageNum, int pageSize, EarTagPageQuery query) {
        return JdbcScheduler.blocking(() -> {
            try {
                PageHelper.startPage(pageNum, pageSize);
                LambdaQueryWrapper<EarTagPO> wrapper = Wrappers.<EarTagPO>lambdaQuery()
                        .orderByAsc(EarTagPO::getId);
                if (query.farmId() != null) {
                    wrapper.eq(EarTagPO::getFarmId, query.farmId());
                }
                if (query.species() != null) {
                    wrapper.eq(EarTagPO::getSpecies, query.species().name());
                }
                if (query.status() != null) {
                    wrapper.eq(EarTagPO::getStatus, query.status());
                }
                List<EarTagPO> rows = earTagMapper.selectList(wrapper);
                long total = rows instanceof com.github.pagehelper.Page
                        ? ((com.github.pagehelper.Page<?>) rows).getTotal()
                        : rows.size();
                List<EarTag> content = rows.stream()
                        .map(EarTagPoConverter::toDomain)
                        .collect(Collectors.toList());
                return new PageResult<>(content, total, pageNum, pageSize);
            } finally {
                PageHelper.clearPage();
            }
        });
    }

    @Override
    public Mono<Void> softDelete(Long id) {
        // @TableLogic 翻译成 UPDATE t_ear_tag SET del_flag = 1 WHERE id = ? AND del_flag = 0
        return JdbcScheduler.blocking((Supplier<Boolean>) () -> {
            earTagMapper.deleteById(id);
            return Boolean.TRUE;
        }).then();
    }

    /**
     * 取下一个耳标号：ET-当年-######（6 位年内顺序号）。
     * 统计不带 del_flag 过滤，已核销耳标的号照样占位，永不复用。
     * 调用方须已持有该年份的取号锁。
     */
    private String nextTagNo(int year) {
        String prefix = NO_PREFIX + year + "-";
        String maxNo = earTagMapper.selectMaxTagNo(prefix);
        long next = 1L;
        if (maxNo != null && maxNo.startsWith(prefix)) {
            try {
                next = Long.parseLong(maxNo.substring(prefix.length())) + 1;
            } catch (NumberFormatException ignore) {
                next = 1L;
            }
        }
        return prefix + String.format("%0" + NO_SEQ_WIDTH + "d", next);
    }
}
