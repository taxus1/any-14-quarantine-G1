package com.somepro.domain.eartag.model;

import com.somepro.domain.shared.model.Species;

/**
 * 耳标名单查询条件（领域值对象）。
 *
 * 任意组合都允许，全为 null 时翻整份名单：
 * - tagNo：按耳标号精确查
 * - farmId：按所属场查
 * - species：按种类查（种类随场，查“猪的耳标”用）
 * - status：按状态查
 */
public record EarTagQuery(String tagNo, Long farmId, Species species, EarTagStatus status) {
}
