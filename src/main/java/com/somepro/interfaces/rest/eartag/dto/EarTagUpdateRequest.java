package com.somepro.interfaces.rest.eartag.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 改正耳标请求体（用户接口层）—— 不可变 record。
 *
 * 所有字段可空：空表示该项不动。
 * - farmId：改挂到另一家场，种类后端随新场自动换；
 * - issuedAt / wornAt：更正发放/佩戴时刻；
 * - status：改状态；置为 USED 且 wornAt 没给时，后端补登记时刻。
 */
public record EarTagUpdateRequest(
        @Positive(message = "farmId 非法")
        Long farmId,

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        LocalDateTime issuedAt,

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        LocalDateTime wornAt,

        @Pattern(regexp = "ISSUED|USED|LOST|DISABLED",
                flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "状态只能是 ISSUED/USED/LOST/DISABLED")
        String status) implements Serializable {
}
