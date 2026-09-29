package com.somepro.interfaces.rest.eartag.vo;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 耳标对外对象（VO）—— 不可变 record。
 *
 * 每行带耳标号 tagNo，同时带所属场编号 farmNo（仓储查询时回填），方便对号。
 * 刻意不含 delFlag / 审计人等内部字段。
 */
public record EarTagVO(Long id,
                       String tagNo,
                       Long farmId,
                       String farmNo,
                       String species,
                       String speciesLabel,
                       LocalDateTime issuedAt,
                       @JsonInclude(JsonInclude.Include.NON_NULL)
                       LocalDateTime wornAt,
                       String status,
                       String statusLabel,
                       LocalDateTime createTime) implements Serializable {
}
