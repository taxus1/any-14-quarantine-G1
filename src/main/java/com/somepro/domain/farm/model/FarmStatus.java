package com.somepro.domain.farm.model;

import com.somepro.common.exception.BizException;

/**
 * 养殖场状态。
 * ACTIVE 在用 / SUSPENDED 停业 / CLOSED 注销（业务状态，记录仍在名单里，可按状态筛选）。
 */
public enum FarmStatus {

    /** 在用（新立场默认） */
    ACTIVE,
    /** 停业 */
    SUSPENDED,
    /** 注销 */
    CLOSED;

    /**
     * 解析外部传入的状态码；{@code null} 原样返回（表示不按状态筛选），
     * 非法值抛业务异常。
     */
    public static FarmStatus of(String code) {
        if (code == null) {
            return null;
        }
        try {
            return FarmStatus.valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BizException("非法的养殖场状态：" + code);
        }
    }
}
