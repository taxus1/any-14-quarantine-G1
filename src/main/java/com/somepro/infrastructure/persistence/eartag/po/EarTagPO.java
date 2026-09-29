package com.somepro.infrastructure.persistence.eartag.po;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.somepro.domain.eartag.model.EarTagStatus;
import com.somepro.infrastructure.persistence.base.BasePO;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * t_ear_tag 表的持久化对象（PO，基础设施层）。
 *
 * 只描述「表长什么样」，不放业务规则（规则在领域对象 EarTag）。
 *
 * issued_at / worn_at 是可空列，更新时允许把时刻清空（如误录的佩戴时刻），
 * 故标 {@link FieldStrategy#IGNORED} 按实体实际值拼 SQL，不走默认的非空跳过。
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

    @TableField(value = "issued_at", updateStrategy = FieldStrategy.IGNORED)
    private LocalDateTime issuedAt;

    @TableField(value = "worn_at", updateStrategy = FieldStrategy.IGNORED)
    private LocalDateTime wornAt;

    @TableField("status")
    private EarTagStatus status;
}
