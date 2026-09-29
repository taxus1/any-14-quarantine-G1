package com.somepro.interfaces.rest.eartag.dto;

import java.time.LocalDateTime;

/**
 * 耳标修改请求：所有字段可选。
 * - farmId：改挂场，种类随新场自动带出；
 * - status：ISSUED/USED/LOST/DISABLED；
 * - wornAt：佩戴时刻，配合 status=USED；不传 status 只传 wornAt 时视为登记佩戴。
 */
public record UpdateEarTagRequest(
        Long farmId,

        LocalDateTime issuedAt,

        String status,

        LocalDateTime wornAt) {
}
