package com.somepro.interfaces.rest.farm.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 养殖场状态变更请求：ACTIVE 在用 / SUSPENDED 停业 / CLOSED 注销。
 */
public record UpdateFarmStatusRequest(
        @NotBlank(message = "状态不能为空（ACTIVE/SUSPENDED/CLOSED）")
        String status) {
}
