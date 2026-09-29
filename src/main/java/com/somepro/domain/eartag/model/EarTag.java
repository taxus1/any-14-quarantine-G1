package com.somepro.domain.eartag.model;

import com.somepro.common.exception.BizException;
import com.somepro.domain.shared.model.BaseEntity;
import com.somepro.domain.shared.model.Species;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 畜禽耳标聚合根（领域层）。
 *
 * 不变量：
 * - 耳标号 tagNo 全局唯一（如 ET-2026-000001），由应用层按「前缀+年份+6 位顺序号」分配；
 * - 必须挂在一家已存在的场（farmId）；种类不另填，照那家场的种类走（species 在场建档时已定死）；
 * - 新发默认 ISSUED（已发放待佩戴）；登记时若已给佩戴时刻，则直接落 USED；
 * - 已佩戴（USED）必须有佩戴时刻 wornAt；非 USED 状态不应有佩戴时刻；
 * - 发放时刻 issuedAt 登记时缺省取当前时刻。
 *
 * farmNo 是查询时为了和纸质台账/场名单对号而由仓储回填的展示字段，不落 t_ear_tag 表。
 */
@Getter
@Setter
public class EarTag extends BaseEntity {

    private Long id;

    /** 耳标号，全局唯一（ET-2026-000001）。 */
    private String tagNo;

    /** 所属养殖场 id。 */
    private Long farmId;

    /** 所属场编号（仓储查询时回填，仅用于展示对号，非表字段）。 */
    private String farmNo;

    /** 种类：随养殖场带出，登记时固定。 */
    private Species species;

    /** 发放时刻。 */
    private LocalDateTime issuedAt;

    /** 佩戴时刻。 */
    private LocalDateTime wornAt;

    /** 耳标状态。 */
    private EarTagStatus status;

    /**
     * 工厂方法：新发耳标。
     *
     * @param tagNo    应用层分配的耳标号
     * @param farmId   挂到哪家场（应用层已校验场存在）
     * @param species  那家场的种类 —— 由应用层从场档案带出，不允许调用方另填
     * @param issuedAt 发放时刻，null 取当前时刻
     * @param wornAt   佩戴时刻；非空表示登记时即已佩戴，状态落 USED，否则落 ISSUED
     */
    public static EarTag issue(String tagNo, Long farmId, Species species,
                               LocalDateTime issuedAt, LocalDateTime wornAt) {
        EarTag tag = new EarTag();
        tag.setTagNo(tagNo);
        tag.attachTo(farmId, species);
        tag.issuedAt = issuedAt != null ? issuedAt : LocalDateTime.now();
        if (wornAt != null) {
            tag.markWorn(wornAt);
        } else {
            tag.status = EarTagStatus.ISSUED;
            tag.wornAt = null;
        }
        return tag;
    }

    /**
     * 领域行为：改挂到另一家场 —— 种类随新场走，不接受外部传种类。
     */
    public void attachTo(Long farmId, Species farmSpecies) {
        if (farmId == null) {
            throw new BizException("耳标必须挂在一家养殖场");
        }
        if (farmSpecies == null) {
            throw new BizException("所属养殖场种类缺失，无法带出耳标种类");
        }
        this.farmId = farmId;
        this.species = farmSpecies;
    }

    /** 领域行为：登记佩戴，状态转 USED 并记佩戴时刻。 */
    public void markWorn(LocalDateTime wornAt) {
        if (wornAt == null) {
            throw new BizException("转为已佩戴时必须填写佩戴时刻");
        }
        this.wornAt = wornAt;
        this.status = EarTagStatus.USED;
    }

    /**
     * 领域行为：变更状态（遗失/停用/找回待戴等），并维护与佩戴时刻的一致性：
     * - 转 USED 必须带佩戴时刻（没带就补发当前时刻）；
     * - 离开 USED（ISSUED/LOST/DISABLED）时清掉佩戴时刻。
     */
    public void changeStatus(EarTagStatus status, LocalDateTime wornAt) {
        if (status == null) {
            throw new BizException("耳标状态不能为空");
        }
        if (status == EarTagStatus.USED) {
            this.wornAt = wornAt != null ? wornAt
                    : (this.wornAt != null ? this.wornAt : LocalDateTime.now());
        } else {
            this.wornAt = null;
        }
        this.status = status;
    }

    /** 改发放时刻。 */
    public void changeIssuedAt(LocalDateTime issuedAt) {
        if (issuedAt == null) {
            throw new BizException("发放时刻不能为空");
        }
        this.issuedAt = issuedAt;
    }
}
