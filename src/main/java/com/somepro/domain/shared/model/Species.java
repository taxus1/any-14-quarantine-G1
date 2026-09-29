package com.somepro.domain.shared.model;

import com.somepro.common.exception.BizException;

/**
 * 畜禽种类（领域共享枚举）。
 *
 * 养殖场档案按种类划分，耳标种类不另行登记，一律随所属养殖场带出，因此放在共享领域层，
 * 供 farm / eartag 两个上下文复用，避免各写一套常量对不上。
 */
public enum Species {

    /** 生猪 */
    PIG,
    /** 牛 */
    CATTLE,
    /** 羊 */
    SHEEP,
    /** 禽 */
    POULTRY;

    /**
     * 解析外部传入的种类码：{@code null} 表示「未按种类筛选」，由调用方自行决定；
     * 非空但不是合法枚举值时按业务异常抛出，而不是让框架抛难懂的转换错误。
     */
    public static Species of(String code) {
        if (code == null) {
            return null;
        }
        try {
            return Species.valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BizException("非法的养殖种类：" + code);
        }
    }
}
