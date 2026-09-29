package com.somepro.domain.farm.model;

import com.somepro.domain.shared.model.Species;

/**
 * 养殖场名单查询条件（领域值对象）。
 *
 * 任意组合都允许，全为 null 时翻整份名单：
 * - farmNo：按编号精确查
 * - farmName：按场名模糊查
 * - species：按种类精确查
 * - status：按状态精确查
 */
public record FarmQuery(String farmNo, String farmName, Species species, FarmStatus status) {
}
