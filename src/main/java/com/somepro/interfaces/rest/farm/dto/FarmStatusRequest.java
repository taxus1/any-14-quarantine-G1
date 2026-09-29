package com.somepro.interfaces.rest.farm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.io.Serializable;

/**
 * 养殖场状态变更请求体：ACTIVE 在用 / SUSPENDED 停业 / CLOSED 注销。 */
public record FarmStatusRequest(
        @NotBlank(message = "状态不能为空（ACTIVE/SUSPENDED/CLOSED）")
        @Pattern(regexp = "ACTIVE|SUSPENDED|CLOSED",
                flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "状态只能是 ACTIVE/SUSPENDED/CLOSED")
        String status) implements Serializable {
}
