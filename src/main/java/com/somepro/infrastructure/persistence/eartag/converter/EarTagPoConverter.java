package com.somepro.infrastructure.persistence.eartag.converter;

import com.somepro.domain.eartag.model.EarTag;
import com.somepro.domain.eartag.model.EarTagStatus;
import com.somepro.domain.shared.model.Species;
import com.somepro.infrastructure.persistence.eartag.po.EarTagPO;

/**
 * EarTagPO（表）↔ EarTag（领域）转换器（基础设施层）。
 *
 * farmNo 不是表字段：toPo 时不带走，toDomain 时留空，由仓储在查名单后按 farm_id 批量回填。
 */
public final class EarTagPoConverter {

    private EarTagPoConverter() {
    }

    public static EarTagPO toPo(EarTag domain) {
        EarTagPO po = new EarTagPO();
        po.setId(domain.getId());
        po.setTagNo(domain.getTagNo());
        po.setFarmId(domain.getFarmId());
        po.setSpecies(domain.getSpecies() == null ? null : domain.getSpecies().name());
        po.setIssuedAt(domain.getIssuedAt());
        po.setWornAt(domain.getWornAt());
        po.setStatus(domain.getStatus() == null ? null : domain.getStatus().name());
        po.setDelFlag(domain.getDelFlag());
        po.setCreateBy(domain.getCreateBy());
        po.setCreateTime(domain.getCreateTime());
        po.setUpdateBy(domain.getUpdateBy());
        po.setUpdateTime(domain.getUpdateTime());
        return po;
    }

    public static EarTag toDomain(EarTagPO po) {
        EarTag domain = new EarTag();
        domain.setId(po.getId());
        domain.setTagNo(po.getTagNo());
        domain.setFarmId(po.getFarmId());
        domain.setSpecies(readEnum(Species.class, po.getSpecies()));
        domain.setIssuedAt(po.getIssuedAt());
        domain.setWornAt(po.getWornAt());
        domain.setStatus(readEnum(EarTagStatus.class, po.getStatus()));
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
