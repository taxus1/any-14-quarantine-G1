package com.somepro.interfaces.rest.eartag.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 发放（登记）耳标请求体（用户接口层）—— 不可变 record。
 *
 * 只收归属场：种类由后端从养殖场档案带出，调用方不用也不能另填；
 * 耳标号后端按年自动取号；状态默认「已发放待佩戴」；
 * 发放时刻不传就取登记时刻。
 */
public record EarTagCreateRequest(
        @NotNull(message = "耳标必须归属一家养殖场（farmId 不能为空）")
        @Positive(message = "farmId 非法")
        Long farmId,

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        LocalDateTime issuedAt) implements Serializable {
}
