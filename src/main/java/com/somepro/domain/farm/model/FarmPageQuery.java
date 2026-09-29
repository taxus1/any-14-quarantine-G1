package com.somepro.domain.farm.model;

import com.somepro.domain.shared.model.Species;

/**
 * 养殖场名单查询条件（领域值对象）。
 *
 * 各字段都可空：一个都不填即查整份名单；任意组合都允许。
 * - farmNo / farmName：模糊匹配，方便按编号、名称片段对账；
 * - species / status：精确匹配。
 */
public record FarmPageQuery(String farmNo, String farmName, Species species, FarmStatus status) {
}
