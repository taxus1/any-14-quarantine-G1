package com.somepro.domain.eartag.repository;

import com.somepro.domain.eartag.model.EarTag;
import com.somepro.domain.eartag.model.EarTagQuery;
import com.somepro.domain.shared.model.PageResult;
import reactor.core.publisher.Mono;

/**
 * 耳标仓储端口（领域层定义，基础设施层实现）。
 */
public interface EarTagRepository {

    /** 耳标号前缀，完整规则 ET-{年份}-{6 位顺序号}，如 ET-2026-000001。 */
    String TAG_NO_PREFIX = "ET";

    Mono<EarTag> save(EarTag earTag);

    Mono<EarTag> findById(Long id);

    /** 分页结果里每条耳标的 farmNo 由仓储一并回填，方便和场名单/纸质台账对号。 */
    Mono<PageResult<EarTag>> page(int pageNum, int pageSize, EarTagQuery query);

    Mono<Void> softDelete(Long id);

    /**
     * 取某一年份下已占用的最大耳标号（含历史已软删记录，避免编号回收复用）。
     * 返回形如 ET-2026-000001 的完整编号；该年还没有记录时返回空 Mono。
     */
    Mono<String> findMaxTagNo(int year);
}
