package com.somepro.domain.farm.repository;

import com.somepro.domain.farm.model.Farm;
import com.somepro.domain.farm.model.FarmQuery;
import com.somepro.domain.shared.model.PageResult;
import reactor.core.publisher.Mono;

/**
 * 养殖场档案仓储端口（领域层定义，基础设施层实现）。
 */
public interface FarmRepository {

    /** 场编号前缀，完整规则 FM-{年份}-{4 位顺序号}，如 FM-2026-0001。 */
    String FARM_NO_PREFIX = "FM";

    Mono<Farm> save(Farm farm);

    Mono<Farm> findById(Long id);

    Mono<PageResult<Farm>> page(int pageNum, int pageSize, FarmQuery query);

    Mono<Void> softDelete(Long id);

    /**
     * 取某一年份下已占用的最大场编号（含历史已软删记录，避免编号回收复用）。
     * 返回的是形如 FM-2026-0001 的完整编号；该年还没有记录时返回空 Mono。
     */
    Mono<String> findMaxFarmNo(int year);
}
