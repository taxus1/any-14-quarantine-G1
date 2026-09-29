package com.somepro.domain.eartag.model;

import com.somepro.common.exception.BizException;
import com.somepro.domain.shared.model.BaseEntity;
import com.somepro.domain.shared.model.Species;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 畜禽耳标聚合根（eartag 上下文）。
 *
 * 纯领域对象：只描述业务与不变量，不带任何持久化注解（表映射在基础设施层的 EarTagPO）。
 *
 * 不变量：
 * - 一个耳标一条记录，耳标号（tagNo，形如 ET-2026-000001）全局唯一，由仓储侧按年份顺序取号；
 * - 耳标必须挂在一家场下，种类不另行登记，一律随所属场的种类带出，保证场里场外对得上；
 * - 新发耳标默认 ISSUED 已发放待佩戴。
 */
@Getter
@Setter
public class EarTag extends BaseEntity {

    private Long id;

    /** 耳标号，全局唯一，仓储侧生成 */
    private String tagNo;

    /** 所属养殖场 id */
    private Long farmId;

    /** 畜禽种类，随所属养殖场带出，不由调用方另填 */
    private Species species;

    /** 发放时刻 */
    private LocalDateTime issuedAt;

    /** 佩戴时刻 */
    private LocalDateTime wornAt;

    /** 耳标状态 */
    private EarTagStatus status;

    /**
     * 工厂方法：新发一枚耳标。
     *
     * @param farmId   挂在哪家场（必填）
     * @param species  该场的种类（由应用层从养殖场档案带出）
     * @param issuedAt 发放时刻，为空则由应用层补登记时刻
     */
    public static EarTag issue(Long farmId, Species species, LocalDateTime issuedAt) {
        EarTag tag = new EarTag();
        tag.farmId = farmId;
        tag.species = species;
        tag.issuedAt = issuedAt;
        tag.wornAt = null;
        tag.status = EarTagStatus.ISSUED;
        tag.normalize();
        return tag;
    }

    /**
     * 领域行为：改挂到另一家场 —— 种类同步换成那家场的种类。
     * 只改归属，发放/佩戴时刻与状态维持原样（状态另有 {@link #changeStatus} 管）。
     */
    public void reattach(Long farmId, Species species) {
        this.farmId = farmId;
        this.species = species;
        this.normalize();
    }

    /**
     * 领域行为：变更状态；置为 USED（已佩戴）且还没有佩戴时刻时，补登记时刻。
     */
    public void changeStatus(EarTagStatus status, LocalDateTime wornAt) {
        if (status == null) {
            throw new BizException("耳标状态不能为空");
        }
        this.status = status;
        if (wornAt != null) {
            this.wornAt = wornAt;
        }
        if (status == EarTagStatus.USED && this.wornAt == null) {
            this.wornAt = LocalDateTime.now();
        }
        normalize();
    }

    /** 领域行为：改发放/佩戴时刻（时刻录错时更正用）。 */
    public void updateTimes(LocalDateTime issuedAt, LocalDateTime wornAt) {
        if (issuedAt != null) {
            this.issuedAt = issuedAt;
        }
        if (wornAt != null) {
            this.wornAt = wornAt;
        }
        normalize();
    }

    /** 集中校验不变量。 */
    private void normalize() {
        if (farmId == null) {
            throw new BizException("耳标必须归属一家养殖场");
        }
        if (species == null) {
            throw new BizException("耳标种类缺失（应随所属养殖场带出）");
        }
        if (status == null) {
            throw new BizException("耳标状态不能为空");
        }
    }
}
