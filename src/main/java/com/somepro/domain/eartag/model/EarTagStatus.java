package com.somepro.domain.eartag.model;

import com.somepro.common.exception.BizException;

/**
 * 耳标状态。
 * ISSUED 已发放待佩戴 / USED 已佩戴 / LOST 遗失 / DISABLED 停用。
 */
public enum EarTagStatus {

    /** 已发放待佩戴（新发耳标默认） */
    ISSUED,
    /** 已佩戴 */
    USED,
    /** 遗失 */
    LOST,
    /** 停用 */
    DISABLED;

    /**
     * 解析外部传入的状态码；{@code null} 原样返回（表示不按状态筛选），
     * 非法值抛业务异常。
     */
    public static EarTagStatus of(String code) {
        if (code == null) {
            return null;
        }
        try {
            return EarTagStatus.valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BizException("非法的耳标状态：" + code);
        }
    }
}
