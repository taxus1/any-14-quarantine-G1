package com.somepro.domain.farm.model;

import com.somepro.common.exception.BizException;
import com.somepro.domain.shared.model.BaseEntity;
import com.somepro.domain.shared.model.Species;
import lombok.Getter;
import lombok.Setter;

/**
 * 养殖场档案聚合根（farm 上下文）。
 *
 * 纯领域对象：只描述业务与不变量，不带任何持久化注解（表映射在基础设施层的 FarmPO）。
 *
 * 不变量：
 * - 一个场一条记录，场编号（farmNo，形如 FM-2026-0001）全局唯一，由仓储侧按年份顺序取号，
 *   业务代码不手填；
 * - 场名、种类必填；存栏数不能为负；
 * - 新立的场默认在用 ACTIVE；
 * - 注销有两层含义：业务状态 CLOSED（记录仍在册，可按状态查）与档案销户（软删除，名单不再翻出），
 *   后者由仓储的 softDelete 经 @TableLogic 完成。
 */
@Getter
@Setter
public class Farm extends BaseEntity {

    private Long id;

    /** 养殖场编号，全局唯一，仓储侧生成 */
    private String farmNo;

    /** 养殖场名称 */
    private String farmName;

    /** 负责人 */
    private String ownerName;

    /** 联系电话 */
    private String phone;

    /** 场址 */
    private String address;

    /** 养殖种类 */
    private Species species;

    /** 当前存栏数 */
    private Integer stockQty;

    /** 业务状态 */
    private FarmStatus status;

    /** 工厂方法：新立一个场。编号由仓储侧分配，这里先置空；状态默认在用。 */
    public static Farm create(String farmName, String ownerName, String phone, String address,
                              Species species, Integer stockQty) {
        Farm farm = new Farm();
        farm.farmName = farmName;
        farm.ownerName = trimToNull(ownerName);
        farm.phone = trimToNull(phone);
        farm.address = trimToNull(address);
        farm.species = species;
        farm.stockQty = stockQty == null ? 0 : stockQty;
        farm.status = FarmStatus.ACTIVE;
        farm.normalize();
        return farm;
    }

    /** 领域行为：改正档案信息（负责人、电话、场址、存栏等写错了都走这里；编号不可改）。 */
    public void updateProfile(String farmName, String ownerName, String phone, String address,
                              Species species, Integer stockQty) {
        this.farmName = farmName;
        this.ownerName = trimToNull(ownerName);
        this.phone = trimToNull(phone);
        this.address = trimToNull(address);
        this.species = species;
        this.stockQty = stockQty == null ? 0 : stockQty;
        this.normalize();
    }

    /** 领域行为：变更业务状态（在用 / 停业 / 注销）。 */
    public void changeStatus(FarmStatus status) {
        if (status == null) {
            throw new BizException("养殖场状态不能为空");
        }
        this.status = status;
    }

    /** 集中校验/规整不变量。 */
    private void normalize() {
        if (farmName == null || farmName.isBlank()) {
            throw new BizException("养殖场名称不能为空");
        }
        this.farmName = farmName.trim();
        if (species == null) {
            throw new BizException("养殖种类不能为空");
        }
        if (stockQty != null && stockQty < 0) {
            throw new BizException("当前存栏数不能为负");
        }
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
