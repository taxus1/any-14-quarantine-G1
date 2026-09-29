package com.somepro.interfaces.rest.farm.vo;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 养殖场档案对外对象（VO）—— 不可变 record。
 *
 * 每行带场编号 farmNo，方便和纸质台账对号。
 * 刻意不含 delFlag / createBy / updateBy / updateTime 等内部字段。
 */
public record FarmVO(Long id,
                     String farmNo,
                     String farmName,
                     String ownerName,
                     String phone,
                     String address,
                     String species,
                     String speciesLabel,
                     Integer stockQty,
                     String status,
                     String statusLabel,
                     LocalDateTime createTime) implements Serializable {
}
