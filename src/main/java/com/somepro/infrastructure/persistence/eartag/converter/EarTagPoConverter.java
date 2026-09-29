package com.somepro.infrastructure.persistence.eartag.converter;

import com.somepro.domain.eartag.model.EarTag;
import com.somepro.domain.shared.model.Species;
import com.somepro.infrastructure.persistence.eartag.po.EarTagPO;

/**
 * EarTagPO（表）↔ EarTag（领域）转换器（基础设施层），PO 与领域之间唯一的转换入口。
 *
 * 种类在表里是字符串、在领域里是枚举，转换集中在这里；审计字段与 delFlag 一并搬运。
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
        po.setStatus(domain.getStatus());
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
        domain.setSpecies(parseSpecies(po.getSpecies(), po.getTagNo()));
        domain.setIssuedAt(po.getIssuedAt());
        domain.setWornAt(po.getWornAt());
        domain.setStatus(po.getStatus());
        domain.setDelFlag(po.getDelFlag());
        domain.setCreateBy(po.getCreateBy());
        domain.setCreateTime(po.getCreateTime());
        domain.setUpdateBy(po.getUpdateBy());
        domain.setUpdateTime(po.getUpdateTime());
        return domain;
    }

    private static Species parseSpecies(String code, String tagNo) {
        if (code == null) {
            return null;
        }
        try {
            return Species.valueOf(code);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "耳标档案出现未支持的种类码 code=" + code + " tagNo=" + tagNo);
        }
    }
}
