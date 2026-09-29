package com.somepro.domain.eartag.repository;

import com.somepro.domain.eartag.model.EarTag;
import com.somepro.domain.eartag.model.EarTagPageQuery;
import com.somepro.domain.shared.model.PageResult;
import reactor.core.publisher.Mono;

/**
 * 耳标聚合的仓储端口（领域层定义，基础设施层实现）。
 *
 * 约定：
 * - 耳标号（tagNo）由仓储侧按「前缀 + 年内顺序号」生成并保证不撞，save 新耳标时自动取号；
 * - 查询自动过滤软删除记录（@TableLogic）；
 * - 分页返回领域值对象 {@link PageResult}。
 */
public interface EarTagRepository {

    Mono<EarTag> save(EarTag earTag);

    Mono<EarTag> findById(Long id);

    Mono<PageResult<EarTag>> page(int pageNum, int pageSize, EarTagPageQuery query);

    Mono<Void> softDelete(Long id);
}
