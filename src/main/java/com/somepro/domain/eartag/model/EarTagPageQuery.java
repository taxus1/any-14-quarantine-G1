package com.somepro.domain.eartag.model;

import com.somepro.domain.shared.model.Species;

/**
 * 耳标名单查询条件（领域值对象）。
 *
 * 各字段都可空：一个都不填即查整份名单；任意组合都允许。
 * - farmId / status：精确匹配；
 * - species：按所属场带出的种类精确匹配。
 */
public record EarTagPageQuery(Long farmId, Species species, EarTagStatus status) {
}
