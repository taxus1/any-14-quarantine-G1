package com.somepro.interfaces.rest.farm.vo;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 养殖场档案对外返回对象（VO，用户接口层）—— 不可变 record。
 *
 * 每行都带场编号 farmNo，方便和纸质台账对号。
 * 刻意不含 delFlag / createBy / updateBy 等内部字段（它们留在领域与 PO 里，不进 API 契约）。
 */
public record FarmVO(
        Long id,
        String farmNo,
        String farmName,
        String ownerName,
        String phone,
        String address,
        String species,
        Integer stockQty,
        String status,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        LocalDateTime createTime) implements Serializable {
}
