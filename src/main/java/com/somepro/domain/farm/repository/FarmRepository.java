package com.somepro.domain.farm.repository;

import com.somepro.domain.farm.model.Farm;
import com.somepro.domain.farm.model.FarmPageQuery;
import com.somepro.domain.shared.model.PageResult;
import reactor.core.publisher.Mono;

/**
 * 养殖场聚合的仓储端口（领域层定义，基础设施层实现）。
 *
 * 约定：
 * - 编号（farmNo）由仓储侧按「前缀 + 年内顺序号」生成并保证不撞，save 新场时自动取号；
 * - 查询自动过滤软删除记录（@TableLogic），销户的场不会再从名单里翻出来；
 * - 分页返回领域值对象 {@link PageResult}，不引入任何框架分页类型。
 */
public interface FarmRepository {

    Mono<Farm> save(Farm farm);

    Mono<Farm> findById(Long id);

    Mono<PageResult<Farm>> page(int pageNum, int pageSize, FarmPageQuery query);

    Mono<Void> softDelete(Long id);
}
