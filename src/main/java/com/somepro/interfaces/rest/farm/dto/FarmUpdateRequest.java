package com.somepro.interfaces.rest.farm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

/**
 * 改正养殖场档案请求体（用户接口层）—— 不可变 record。
 *
 * 场编号不在此列：编号是台账根，一经登记不可改。
 * 状态也不放这里：状态走单独的变更接口，避免改正文字时误改在用/停业状态。
 */
public record FarmUpdateRequest(
        @NotBlank(message = "养殖场名称不能为空")
        @Size(max = 64, message = "养殖场名称最长 64 字")
        String farmName,

        @Size(max = 64, message = "负责人最长 64 字")
        String ownerName,

        @Size(max = 20, message = "联系电话最长 20 字")
        String phone,

        @Size(max = 255, message = "场址最长 255 字")
        String address,

        @NotBlank(message = "养殖种类不能为空（PIG/CATTLE/SHEEP/POULTRY 四选一）")
        @Pattern(regexp = "PIG|CATTLE|SHEEP|POULTRY",
                flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "养殖种类只能是 PIG/CATTLE/SHEEP/POULTRY")
        String species,

        @NotNull(message = "当前存栏数不能为空（没有就填 0）")
        @PositiveOrZero(message = "当前存栏数不能为负")
        Integer stockQty) implements Serializable {
}
