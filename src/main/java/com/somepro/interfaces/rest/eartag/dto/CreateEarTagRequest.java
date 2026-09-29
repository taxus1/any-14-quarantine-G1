package com.somepro.interfaces.rest.eartag.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * 耳标登记请求。
 *
 * 刻意没有 tagNo（系统分配）和 species（种类照所属场走，不另填）。
 * issuedAt 不传取发放登记当前时刻；wornAt 非空表示登记时已佩戴（状态 USED），否则默认 ISSUED。
 */
public record CreateEarTagRequest(
        @NotNull(message = "必须指定挂在哪家场（farmId）")
        Long farmId,

        LocalDateTime issuedAt,

        LocalDateTime wornAt) {
}
