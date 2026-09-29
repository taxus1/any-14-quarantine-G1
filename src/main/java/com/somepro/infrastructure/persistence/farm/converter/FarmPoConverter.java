package com.somepro.infrastructure.persistence.farm.converter;

import com.somepro.domain.farm.model.Farm;
import com.somepro.domain.shared.model.Species;
import com.somepro.infrastructure.persistence.farm.po.FarmPO;

/**
 * FarmPO（表）↔ Farm（领域）转换器（基础设施层），PO 与领域之间唯一的转换入口。
 *
 * 种类在表里是字符串，在领域里是枚举，转换集中在这里；
 * 审计字段与 delFlag 也一并搬运（新建时由 MetaObjectHandler 填充后回写领域对象）。
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
        po.setStatus(domain.getStatus());
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
        // 库里是早先录入的数据，种类码理论上都在枚举内；万一出现未知码，原样报出来便于对账
        domain.setSpecies(parseSpecies(po.getSpecies(), po.getFarmNo()));
        domain.setStockQty(po.getStockQty());
        domain.setStatus(po.getStatus());
        domain.setDelFlag(po.getDelFlag());
        domain.setCreateBy(po.getCreateBy());
        domain.setCreateTime(po.getCreateTime());
        domain.setUpdateBy(po.getUpdateBy());
        domain.setUpdateTime(po.getUpdateTime());
        return domain;
    }

    private static Species parseSpecies(String code, String farmNo) {
        if (code == null) {
            return null;
        }
        try {
            return Species.valueOf(code);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "养殖场档案出现未支持的种类码 code=" + code + " farmNo=" + farmNo);
        }
    }
}
