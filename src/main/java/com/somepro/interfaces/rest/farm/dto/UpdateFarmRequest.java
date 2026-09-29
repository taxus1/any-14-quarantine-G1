package com.somepro.interfaces.rest.farm.dto;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * 养殖场档案修改请求：所有字段可选，只改传了的（种类不可改，故不在此出现）。
 */
public record UpdateFarmRequest(
        @Size(max = 64, message = "养殖场名称最长 64 个字符")
        String farmName,

        @Size(max = 64, message = "负责人姓名最长 64 个字符")
        String ownerName,

        @Size(max = 20, message = "联系电话最长 20 个字符")
        String phone,

        @Size(max = 255, message = "场址最长 255 个字符")
        String address,

        @PositiveOrZero(message = "当前存栏数不能为负数")
        Integer stockQty) {
}
