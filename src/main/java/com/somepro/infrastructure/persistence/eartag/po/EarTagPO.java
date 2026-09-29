package com.somepro.infrastructure.persistence.eartag.po;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.somepro.infrastructure.persistence.base.BasePO;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * t_ear_tag 表的持久化对象（PO，基础设施层）。
 *
 * 只描述「表长什么样」。种类/状态存字符串码；farmNo 不是表字段（表里只有 farm_id），
 * 它只存在于领域对象上、由仓储查询时回填，所以这里没有对应列。
 */
@Getter
@Setter
@TableName("t_ear_tag")
public class EarTagPO extends BasePO {

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    @TableField("tag_no")
    private String tagNo;

    @TableField("farm_id")
    private Long farmId;

    @TableField("species")
    private String species;

    @TableField("issued_at")
    private LocalDateTime issuedAt;

    @TableField(value = "worn_at", updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime wornAt;

    @TableField("status")
    private String status;
}
