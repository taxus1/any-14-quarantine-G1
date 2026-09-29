package com.somepro.infrastructure.persistence.farm;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.pagehelper.PageHelper;
import com.somepro.domain.farm.model.Farm;
import com.somepro.domain.farm.model.FarmPageQuery;
import com.somepro.domain.farm.repository.FarmRepository;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.infrastructure.persistence.farm.converter.FarmPoConverter;
import com.somepro.infrastructure.persistence.farm.po.FarmPO;
import com.somepro.infrastructure.persistence.support.BusinessNumberAllocator;
import com.somepro.infrastructure.persistence.support.JdbcScheduler;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 养殖场仓储适配器：用 MyBatis-Plus 实现领域仓储端口（基础设施层）。
 *
 * 关键约定：
 * - 所有 DB 调用经 {@link JdbcScheduler#blocking} 切到 boundedElastic，绝不在 event-loop 上跑 JDBC；
 * - ID 应用侧雪花分配（IdType.INPUT）；
 * - 场编号 FM-yyyy-#### 在本层按年取号，含已销户记录一起计数，号不回收；撞唯一键时换号重试；
 * - 软删除交给 @TableLogic，不手写 del_flag 条件；
 * - 分页用 PageHelper，finally 里 clearPage；名单按 id 升序，翻页稳定不重样。
 */
@Repository
public class FarmRepositoryImpl implements FarmRepository {

    /** 编号前缀/位数 */
    private static final String NO_PREFIX = "FM-";
    private static final int NO_SEQ_WIDTH = 4;
    /** 取号命名锁前缀（与年份拼在一起，不同年互不阻塞） */
    private static final String NO_LOCK_PREFIX = "farm-no-";

    private final FarmMapper farmMapper;
    private final BusinessNumberAllocator numberAllocator;

    public FarmRepositoryImpl(FarmMapper farmMapper, BusinessNumberAllocator numberAllocator) {
        this.farmMapper = farmMapper;
        this.numberAllocator = numberAllocator;
    }

    @Override
    public Mono<Farm> save(Farm farm) {
        if (farm.getId() == null) {
            // 新建：取号 + 插入放在「短事务 + 命名锁」内，并发下不撞号
            return JdbcScheduler.blocking(() -> {
                int year = LocalDateTime.now().getYear();
                String lockName = NO_LOCK_PREFIX + year;
                return numberAllocator.allocate(lockName, () -> {
                    FarmPO po = FarmPoConverter.toPo(farm);
                    po.setId(IdUtil.getSnowflakeNextId());
                    po.setFarmNo(nextFarmNo(year));
                    farm.setId(po.getId());
                    farm.setFarmNo(po.getFarmNo());
                    farmMapper.insert(po);
                    return FarmPoConverter.toDomain(po);
                });
            });
        }
        return JdbcScheduler.blocking(() -> {
            FarmPO po = FarmPoConverter.toPo(farm);
            farmMapper.updateById(po);
            return FarmPoConverter.toDomain(po);
        });
    }

    @Override
    public Mono<Farm> findById(Long id) {
        return JdbcScheduler.blocking(() -> {
            FarmPO po = farmMapper.selectById(id);
            return po == null ? null : FarmPoConverter.toDomain(po);
        });
    }

    @Override
    public Mono<PageResult<Farm>> page(int pageNum, int pageSize, FarmPageQuery query) {
        return JdbcScheduler.blocking(() -> {
            try {
                // 按 id 升序给分页一个稳定次序，两页之间不重样
                PageHelper.startPage(pageNum, pageSize);
                LambdaQueryWrapper<FarmPO> wrapper = Wrappers.<FarmPO>lambdaQuery()
                        .orderByAsc(FarmPO::getId);
                if (query.farmNo() != null && !query.farmNo().isBlank()) {
                    wrapper.like(FarmPO::getFarmNo, query.farmNo());
                }
                if (query.farmName() != null && !query.farmName().isBlank()) {
                    wrapper.like(FarmPO::getFarmName, query.farmName());
                }
                if (query.species() != null) {
                    wrapper.eq(FarmPO::getSpecies, query.species().name());
                }
                if (query.status() != null) {
                    wrapper.eq(FarmPO::getStatus, query.status());
                }
                List<FarmPO> rows = farmMapper.selectList(wrapper);
                long total = rows instanceof com.github.pagehelper.Page
                        ? ((com.github.pagehelper.Page<?>) rows).getTotal()
                        : rows.size();
                List<Farm> content = rows.stream()
                        .map(FarmPoConverter::toDomain)
                        .collect(Collectors.toList());
                return new PageResult<>(content, total, pageNum, pageSize);
            } finally {
                // ThreadLocal 分页参数必须清，否则污染线程池下一次调用
                PageHelper.clearPage();
            }
        });
    }

    @Override
    public Mono<Void> softDelete(Long id) {
        // @TableLogic 翻译成 UPDATE t_farm SET del_flag = 1 WHERE id = ? AND del_flag = 0
        return JdbcScheduler.blocking((Supplier<Boolean>) () -> {
            farmMapper.deleteById(id);
            return Boolean.TRUE;
        }).then();
    }

    /**
     * 取下一个场编号：FM-当年-####（4 位年内顺序号）。
     * 统计走 {@link FarmMapper#selectMaxFarmNo}，不带 del_flag 过滤，
     * 销户场的号照样占位，保证一个号只归一个场、永不复用。
     * 调用方须已持有该年份的取号锁。
     */
    private String nextFarmNo(int year) {
        String prefix = NO_PREFIX + year + "-";
        String maxNo = farmMapper.selectMaxFarmNo(prefix);
        long next = 1L;
        if (maxNo != null && maxNo.startsWith(prefix)) {
            try {
                next = Long.parseLong(maxNo.substring(prefix.length())) + 1;
            } catch (NumberFormatException ignore) {
                // 库里存在非数字尾巴的历史编号，回退从 1 起，交给唯一键兜底
                next = 1L;
            }
        }
        return prefix + String.format("%0" + NO_SEQ_WIDTH + "d", next);
    }
}
