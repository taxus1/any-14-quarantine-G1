package com.somepro.domain.farm.model;

import com.somepro.common.exception.BizException;
import com.somepro.domain.shared.model.BaseEntity;
import com.somepro.domain.shared.model.Species;
import lombok.Getter;
import lombok.Setter;

/**
 * 养殖场档案聚合根（领域层）。
 *
 * 纯领域对象：只描述业务与不变量，不带任何持久化注解（表映射在基础设施层的 FarmPO）。
 *
 * 不变量：
 * - 场编号 farmNo 全局唯一（如 FM-2026-0001），由应用层按「前缀+年份+4 位顺序号」分配；
 * - 场名必填；养殖种类登记后固定，不允许改（耳标的种类照着场走，改了会场里场外对不上）；
 * - 存栏数不能为负；
 * - 新立档案默认 ACTIVE；注销状态（CLOSED）与软删除是两回事，软删除后名单不再翻得出。
 */
@Getter
@Setter
public class Farm extends BaseEntity {

    private Long id;

    /** 养殖场编号，全局唯一（FM-2026-0001）。 */
    private String farmNo;

    /** 养殖场名称。 */
    private String farmName;

    /** 负责人。 */
    private String ownerName;

    /** 联系电话。 */
    private String phone;

    /** 场址。 */
    private String address;

    /** 养殖种类：登记后固定。 */
    private Species species;

    /** 当前存栏数。 */
    private Integer stockQty;

    /** 场状态。 */
    private FarmStatus status;

    /**
     * 工厂方法：登记新场。编号由应用层算好传入（需要查库里现有最大序号），
     * 状态默认在用，存栏默认 0。
     */
    public static Farm register(String farmNo, String farmName, String ownerName, String phone,
                                String address, Species species, Integer stockQty) {
        Farm farm = new Farm();
        farm.setFarmNo(farmNo);
        farm.changeInfo(farmName, ownerName, phone, address);
        farm.species = requireSpecies(species);
        farm.changeStock(stockQty == null ? 0 : stockQty);
        farm.status = FarmStatus.ACTIVE;
        return farm;
    }

    /**
     * 领域行为：改档案信息（场名、负责人、电话、场址）。
     * 刻意不含种类 —— 种类登记后不可改。
     */
    public void changeInfo(String farmName, String ownerName, String phone, String address) {
        if (farmName == null || farmName.isBlank()) {
            throw new BizException("养殖场名称不能为空");
        }
        this.farmName = farmName.trim();
        this.ownerName = trimToNull(ownerName);
        this.phone = trimToNull(phone);
        this.address = trimToNull(address);
    }

    /** 领域行为：更新当前存栏数（不能为负）。 */
    public void changeStock(int stockQty) {
        if (stockQty < 0) {
            throw new BizException("当前存栏数不能为负数");
        }
        this.stockQty = stockQty;
    }

    /** 领域行为：变更场状态（停业/复业/注销）。 */
    public void changeStatus(FarmStatus status) {
        if (status == null) {
            throw new BizException("养殖场状态不能为空");
        }
        this.status = status;
    }

    private static Species requireSpecies(Species species) {
        if (species == null) {
            throw new BizException("养殖种类不能为空");
        }
        return species;
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
