package com.somepro.infrastructure.persistence.farm.converter;

import com.somepro.domain.farm.model.Farm;
import com.somepro.domain.farm.model.FarmStatus;
import com.somepro.domain.shared.model.Species;
import com.somepro.infrastructure.persistence.farm.po.FarmPO;

/**
 * FarmPO（表）↔ Farm（领域）转换器（基础设施层）。
 *
 * 枚举按字符串码搬运；库里是早先手工/历史录入的数据，可能出现代码不认识的码，
 * 读取时宽容处理成 null（字段仍在库里，不影响翻名单），写入时由领域层严格校验。
 */
public final class FarmPoConverter {

    private FarmPoConverter() {
    }

    public static FarmPO toPo(Farm domain) {
        FarmPO po = new FarmPO();
        po.setId(domain.getId());
        po.setFarmNo(domain.getFarmNo());
        po.setFarmName(domain.getFarmName());
        po.setOwnerName(domain.getOwnerName());
        po.setPhone(domain.getPhone());
        po.setAddress(domain.getAddress());
        po.setSpecies(domain.getSpecies() == null ? null : domain.getSpecies().name());
        po.setStockQty(domain.getStockQty());
        po.setStatus(domain.getStatus() == null ? null : domain.getStatus().name());
        po.setDelFlag(domain.getDelFlag());
        po.setCreateBy(domain.getCreateBy());
        po.setCreateTime(domain.getCreateTime());
        po.setUpdateBy(domain.getUpdateBy());
        po.setUpdateTime(domain.getUpdateTime());
        return po;
    }

    public static Farm toDomain(FarmPO po) {
        Farm domain = new Farm();
        domain.setId(po.getId());
        domain.setFarmNo(po.getFarmNo());
        domain.setFarmName(po.getFarmName());
        domain.setOwnerName(po.getOwnerName());
        domain.setPhone(po.getPhone());
        domain.setAddress(po.getAddress());
        domain.setSpecies(readEnum(Species.class, po.getSpecies()));
        domain.setStockQty(po.getStockQty());
        domain.setStatus(readEnum(FarmStatus.class, po.getStatus()));
        domain.setDelFlag(po.getDelFlag());
        domain.setCreateBy(po.getCreateBy());
        domain.setCreateTime(po.getCreateTime());
        domain.setUpdateBy(po.getUpdateBy());
        domain.setUpdateTime(po.getUpdateTime());
        return domain;
    }

    private static <E extends Enum<E>> E readEnum(Class<E> type, String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, code.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            // 历史数据出现未知码时不炸整份名单：当 null 读出，原列值仍在库里
            return null;
        }
    }
}
