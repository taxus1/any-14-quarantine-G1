package com.somepro.domain.shared.model;

import com.somepro.common.exception.BizException;

/**
 * 养殖种类（领域枚举）。
 *
 * 纯领域定义，不带任何框架注解；落库时存 {@link #name()}（PIG/CATTLE/SHEEP/POULTRY），
 * 与 t_farm.species / t_ear_tag.species 的列注释一致。
 *
 * 耳标的种类不另立来源：耳标挂到哪家场，就照那家场的种类走（见 EarTag 聚合与应用层）。
 */
public enum Species {

    PIG("生猪"),
    CATTLE("牛"),
    SHEEP("羊"),
    POULTRY("禽");

    private final String label;

    Species(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** 严格解析外部入参（登记/查询条件）：空白或无法识别直接报业务错，不静默吞成 null。 */
    public static Species fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new BizException("养殖种类不能为空");
        }
        try {
            return valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BizException("无效的养殖种类：" + code + "（可选 PIG/CATTLE/SHEEP/POULTRY）");
        }
    }
}
