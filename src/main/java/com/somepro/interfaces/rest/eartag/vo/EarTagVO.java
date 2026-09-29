package com.somepro.interfaces.rest.eartag.vo;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 耳标对外返回对象（VO，用户接口层）—— 不可变 record。
 *
 * 每行都带耳标号 tagNo，方便和纸质台账对号；种类字段即所属养殖场的种类。
 * 刻意不含 delFlag / 审计人等内部字段。
 */
public record EarTagVO(
        Long id,
        String tagNo,
        Long farmId,
        String species,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        LocalDateTime issuedAt,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        LocalDateTime wornAt,
        String status,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        LocalDateTime createTime) implements Serializable {
}
