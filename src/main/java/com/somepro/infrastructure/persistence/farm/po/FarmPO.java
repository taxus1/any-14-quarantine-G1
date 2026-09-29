package com.somepro.infrastructure.persistence.farm.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.somepro.infrastructure.persistence.base.BasePO;
import lombok.Getter;
import lombok.Setter;

/**
 * t_farm 表的持久化对象（PO，基础设施层）。
 *
 * 只描述「表长什么样」：字段与列一一对应，不放业务规则（规则在领域对象 Farm）。
 * 种类/状态在领域层是枚举，这里只存列注释约定的字符串码（PIG/ACTIVE...），
 * 与领域枚举的互转在 FarmPoConverter 完成。
 */
@Getter
@Setter
@TableName("t_farm")
public class FarmPO extends BasePO {

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    @TableField("farm_no")
    private String farmNo;

    @TableField("farm_name")
    private String farmName;

    @TableField("owner_name")
    private String ownerName;

    @TableField("phone")
    private String phone;

    @TableField("address")
    private String address;

    @TableField("species")
    private String species;

    @TableField("stock_qty")
    private Integer stockQty;

    @TableField("status")
    private String status;
}
