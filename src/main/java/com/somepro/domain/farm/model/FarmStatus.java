package com.somepro.domain.farm.model;

import com.somepro.common.exception.BizException;

/**
 * 养殖场状态（领域枚举）。
 * ACTIVE 在用 / SUSPENDED 停业 / CLOSED 注销；新立档案默认 ACTIVE。
 *
 * 注意：CLOSED（注销状态）是档案的一种业务状态，仍会出现在名单里；
 * “这家场不干了、销掉后不再出现在名单”走仓储软删除（del_flag=1），不是改这个状态。
 */
public enum FarmStatus {

    ACTIVE("在用"),
    SUSPENDED("停业"),
    CLOSED("注销");

    private final String label;

    FarmStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static FarmStatus fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new BizException("养殖场状态不能为空");
        }
        try {
            return valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BizException("无效的养殖场状态：" + code + "（可选 ACTIVE/SUSPENDED/CLOSED）");
        }
    }
}
