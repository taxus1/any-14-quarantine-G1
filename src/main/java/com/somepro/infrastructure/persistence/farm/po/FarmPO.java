package com.somepro.infrastructure.persistence.farm.po;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.somepro.domain.farm.model.FarmStatus;
import com.somepro.infrastructure.persistence.base.BasePO;
import lombok.Getter;
import lombok.Setter;

/**
 * t_farm 表的持久化对象（PO，基础设施层）。
 *
 * 只描述「表长什么样」：字段与列一一对应，不放业务规则（规则在领域对象 Farm）。
 * 与领域对象的互转见 FarmPoConverter。
 *
 * owner_name / phone / address 是可空列，更新档案时允许「原来有值、改后清空」：
 * 默认的非空更新策略会跳过 null 字段、清不掉，所以这三列显式标 {@link FieldStrategy#IGNORED}
 * （按实体实际值拼 SQL）。非空字段仍走默认策略。
 *
 * status 以枚举名（STRING）落库，MyBatis-Plus 默认即按枚举名存取，无需额外注解。
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

    @TableField(value = "owner_name", updateStrategy = FieldStrategy.IGNORED)
    private String ownerName;

    @TableField(value = "phone", updateStrategy = FieldStrategy.IGNORED)
    private String phone;

    @TableField(value = "address", updateStrategy = FieldStrategy.IGNORED)
    private String address;

    @TableField("species")
    private String species;

    @TableField("stock_qty")
    private Integer stockQty;

    @TableField("status")
    private FarmStatus status;
}
