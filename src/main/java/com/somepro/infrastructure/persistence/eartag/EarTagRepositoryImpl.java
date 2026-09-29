package com.somepro.infrastructure.persistence.eartag;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.somepro.domain.eartag.model.EarTag;
import com.somepro.domain.eartag.model.EarTagQuery;
import com.somepro.domain.eartag.repository.EarTagRepository;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.infrastructure.persistence.base.AbstractBlockingRepository;
import com.somepro.infrastructure.persistence.eartag.converter.EarTagPoConverter;
import com.somepro.infrastructure.persistence.eartag.po.EarTagPO;
import com.somepro.infrastructure.persistence.farm.FarmMapper;
import com.somepro.infrastructure.persistence.farm.po.FarmPO;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 耳标仓储适配器（基础设施层）。
 *
 * 跨聚合取数说明：耳标名单每行要带场编号（farmNo），而 t_ear_tag 表只有 farm_id。
 * 分页拿到本页耳标后，按本页 farm_id 一次性回查 t_farm 回填，避免 N+1。
 * 这里直接注入 {@link FarmMapper} 属于基础设施层内部的只读关联，不破坏领域分层
 * （领域层只认 farmId，回填的 farmNo 仅为展示对号）。
 */
@Repository
public class EarTagRepositoryImpl extends AbstractBlockingRepository implements EarTagRepository {

    private final EarTagMapper earTagMapper;
    private final FarmMapper farmMapper;

    public EarTagRepositoryImpl(EarTagMapper earTagMapper, FarmMapper farmMapper) {
        this.earTagMapper = earTagMapper;
        this.farmMapper = farmMapper;
    }

    @Override
    public Mono<EarTag> save(EarTag earTag) {
        return blocking(() -> {
            EarTagPO po = EarTagPoConverter.toPo(earTag);
            if (po.getId() == null) {
                po.setId(IdUtil.getSnowflakeNextId());
                earTagMapper.insert(po);
            } else {
                earTagMapper.updateById(po);
            }
            return EarTagPoConverter.toDomain(po);
        });
    }

    @Override
    public Mono<EarTag> findById(Long id) {
        return blocking(() -> {
            EarTagPO po = earTagMapper.selectById(id);
            if (po == null) {
                return null;
            }
            EarTag domain = EarTagPoConverter.toDomain(po);
            domain.setFarmNo(loadFarmNo(po.getFarmId()));
            return domain;
        });
    }

    @Override
    public Mono<PageResult<EarTag>> page(int pageNum, int pageSize, EarTagQuery query) {
        return this.<PageResult<EarTag>>blocking(() -> {
            try {
                PageHelper.startPage(pageNum, pageSize);
                LambdaQueryWrapper<EarTagPO> wrapper = Wrappers.<EarTagPO>lambdaQuery()
                        .orderByAsc(EarTagPO::getId);
                if (query != null) {
                    wrapper.eq(query.tagNo() != null && !query.tagNo().isBlank(),
                                    EarTagPO::getTagNo, query.tagNo())
                            .eq(query.farmId() != null, EarTagPO::getFarmId, query.farmId())
                            .eq(query.species() != null, EarTagPO::getSpecies,
                                    query.species() == null ? null : query.species().name())
                            .eq(query.status() != null, EarTagPO::getStatus,
                                    query.status() == null ? null : query.status().name());
                }
                List<EarTagPO> rows = earTagMapper.selectList(wrapper);
                long total = rows instanceof Page ? ((Page<?>) rows).getTotal() : rows.size();

                Map<Long, String> farmNoMap = loadFarmNos(rows);
                List<EarTag> content = rows.stream()
                        .map(EarTagPoConverter::toDomain)
                        .peek(tag -> tag.setFarmNo(farmNoMap.get(tag.getFarmId())))
                        .collect(Collectors.toList());
                return new PageResult<>(content, total, pageNum, pageSize);
            } finally {
                PageHelper.clearPage();
            }
        });
    }

    @Override
    public Mono<Void> softDelete(Long id) {
        return blocking(() -> {
            // @TableLogic 翻译成 UPDATE t_ear_tag SET del_flag = 1 WHERE id = ? AND del_flag = 0
            earTagMapper.deleteById(id);
            return Boolean.TRUE;
        }).then();
    }

    @Override
    public Mono<String> findMaxTagNo(int year) {
        String prefix = EarTagRepository.TAG_NO_PREFIX + "-" + year + "-";
        return blocking(() -> {
            String max = earTagMapper.selectMaxTagNo(prefix + "%");
            return (max == null || max.isBlank()) ? null : max;
        });
    }

    private String loadFarmNo(Long farmId) {
        if (farmId == null) {
            return null;
        }
        FarmPO farm = farmMapper.selectById(farmId);
        return farm == null ? null : farm.getFarmNo();
    }

    /** 本页耳标涉及的场只查一次；@TableLogic 自动只取未删场，已删场回填 null。 */
    private Map<Long, String> loadFarmNos(List<EarTagPO> rows) {
        Set<Long> farmIds = rows.stream()
                .map(EarTagPO::getFarmId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (farmIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> result = new HashMap<>();
        for (FarmPO farm : farmMapper.selectByIds(farmIds)) {
            result.put(farm.getId(), farm.getFarmNo());
        }
        return result;
    }
}
