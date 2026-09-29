package com.somepro.infrastructure.persistence.farm;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.somepro.domain.farm.model.Farm;
import com.somepro.domain.farm.model.FarmQuery;
import com.somepro.domain.farm.repository.FarmRepository;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.infrastructure.persistence.base.AbstractBlockingRepository;
import com.somepro.infrastructure.persistence.farm.converter.FarmPoConverter;
import com.somepro.infrastructure.persistence.farm.po.FarmPO;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 养殖场档案仓储适配器（基础设施层）。
 *
 * - Mapper 只在 blocking(...) 里调用（见 AbstractBlockingRepository）；
 * - 软删除/过滤交给 @TableLogic，不手写 del_flag；
 * - 分页统一 PageHelper，按 id 升序保证翻页稳定（两页之间不重样）；
 * - 编号生成查 MAX 时连软删历史行一起看，编号永不回收。
 */
@Repository
public class FarmRepositoryImpl extends AbstractBlockingRepository implements FarmRepository {

    private final FarmMapper farmMapper;

    public FarmRepositoryImpl(FarmMapper farmMapper) {
        this.farmMapper = farmMapper;
    }

    @Override
    public Mono<Farm> save(Farm farm) {
        return blocking(() -> {
            FarmPO po = FarmPoConverter.toPo(farm);
            if (po.getId() == null) {
                po.setId(IdUtil.getSnowflakeNextId());
                farmMapper.insert(po);
            } else {
                farmMapper.updateById(po);
            }
            // insert/update 后 id 与审计字段已回填，转回领域对象一并返回
            return FarmPoConverter.toDomain(po);
        });
    }

    @Override
    public Mono<Farm> findById(Long id) {
        return blocking(() -> {
            FarmPO po = farmMapper.selectById(id);
            return po == null ? null : FarmPoConverter.toDomain(po);
        });
    }

    @Override
    public Mono<PageResult<Farm>> page(int pageNum, int pageSize, FarmQuery query) {
        return this.<PageResult<Farm>>blocking(() -> {
            try {
                PageHelper.startPage(pageNum, pageSize);
                LambdaQueryWrapper<FarmPO> wrapper = Wrappers.<FarmPO>lambdaQuery()
                        // 稳定排序：分页逐页往后翻不能重行
                        .orderByAsc(FarmPO::getId);
                if (query != null) {
                    wrapper.eq(query.farmNo() != null && !query.farmNo().isBlank(),
                                    FarmPO::getFarmNo, query.farmNo())
                            .like(query.farmName() != null && !query.farmName().isBlank(),
                                    FarmPO::getFarmName, query.farmName())
                            .eq(query.species() != null, FarmPO::getSpecies,
                                    query.species() == null ? null : query.species().name())
                            .eq(query.status() != null, FarmPO::getStatus,
                                    query.status() == null ? null : query.status().name());
                }
                List<FarmPO> rows = farmMapper.selectList(wrapper);
                long total = rows instanceof Page ? ((Page<?>) rows).getTotal() : rows.size();
                List<Farm> content = rows.stream()
                        .map(FarmPoConverter::toDomain)
                        .collect(Collectors.toList());
                return new PageResult<>(content, total, pageNum, pageSize);
            } finally {
                // ThreadLocal 分页参数必须清理，否则污染线程池里的下一次调用
                PageHelper.clearPage();
            }
        });
    }

    @Override
    public Mono<Void> softDelete(Long id) {
        return blocking(() -> {
            // @TableLogic 翻译成 UPDATE t_farm SET del_flag = 1 WHERE id = ? AND del_flag = 0
            farmMapper.deleteById(id);
            return Boolean.TRUE;
        }).then();
    }

    @Override
    public Mono<String> findMaxFarmNo(int year) {
        String prefix = FarmRepository.FARM_NO_PREFIX + "-" + year + "-";
        return blocking(() -> {
            String max = farmMapper.selectMaxFarmNo(prefix + "%");
            // 该年一行都没有时 MAX() 返回 null；历史脏数据若给空串也按“没有”处理
            return (max == null || max.isBlank()) ? null : max;
        });
    }
}
