package com.somepro.domain.eartag.model;

import com.somepro.common.exception.BizException;

/**
 * 耳标状态（领域枚举）。
 * ISSUED 已发放待佩戴 / USED 已佩戴 / LOST 遗失 / DISABLED 停用；新发耳标默认 ISSUED。
 */
public enum EarTagStatus {

    ISSUED("已发放待佩戴"),
    USED("已佩戴"),
    LOST("遗失"),
    DISABLED("停用");

    private final String label;

    EarTagStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static EarTagStatus fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new BizException("耳标状态不能为空");
        }
        try {
            return valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BizException("无效的耳标状态：" + code + "（可选 ISSUED/USED/LOST/DISABLED）");
        }
    }
}
