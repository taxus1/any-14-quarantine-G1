package com.somepro.interfaces.rest.farm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * 养殖场登记请求。
 *
 * farmNo / status 不在请求里：编号由系统分配，新场默认在用。
 * 种类传码值 PIG/CATTLE/SHEEP/POULTRY，由应用层解析校验。
 */
public record CreateFarmRequest(
        @NotBlank(message = "养殖场名称不能为空")
        @Size(max = 64, message = "养殖场名称最长 64 个字符")
        String farmName,

        @Size(max = 64, message = "负责人姓名最长 64 个字符")
        String ownerName,

        @Size(max = 20, message = "联系电话最长 20 个字符")
        String phone,

        @Size(max = 255, message = "场址最长 255 个字符")
        String address,

        @NotBlank(message = "养殖种类不能为空（PIG/CATTLE/SHEEP/POULTRY）")
        String species,

        @PositiveOrZero(message = "当前存栏数不能为负数")
        Integer stockQty) {
}
