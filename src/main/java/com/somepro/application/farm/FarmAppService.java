package com.somepro.application.farm;

import com.somepro.common.exception.BizException;
import com.somepro.domain.farm.model.Farm;
import com.somepro.domain.farm.model.FarmPageQuery;
import com.somepro.domain.farm.model.FarmStatus;
import com.somepro.domain.farm.repository.FarmRepository;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.domain.shared.model.Species;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * 养殖场档案应用服务：编排用例，不写业务规则（规则在领域聚合 Farm）。
 *
 * 出入参都是领域对象，不认识 PO / VO。场编号在仓储落库时自动取号，这里不接收编号。
 */
@Service
public class FarmAppService {

    private final FarmRepository farmRepository;

    public FarmAppService(FarmRepository farmRepository) {
        this.farmRepository = farmRepository;
    }

    /** 登记新场：种类码解析成枚举，状态由聚合默认置为在用。 */
    public Mono<Farm> createFarm(String farmName, String ownerName, String phone, String address,
                                 String speciesCode, Integer stockQty) {
        Species species = Species.of(speciesCode);
        Farm farm = Farm.create(farmName, ownerName, phone, address, species, stockQty);
        return farmRepository.save(farm);
    }

    /** 改正档案信息（场名、负责人等写错了用它；场编号不可改）。 */
    public Mono<Farm> updateFarm(Long id, String farmName, String ownerName, String phone, String address,
                                 String speciesCode, Integer stockQty) {
        return requireFarm(id).flatMap(farm -> {
            farm.updateProfile(farmName, ownerName, phone, address, Species.of(speciesCode), stockQty);
            return farmRepository.save(farm);
        });
    }

    /** 变更业务状态：在用 / 停业 / 注销（记录仍在册，可按状态查）。 */
    public Mono<Farm> updateFarmStatus(Long id, String statusCode) {
        return requireFarm(id).flatMap(farm -> {
            farm.changeStatus(FarmStatus.of(statusCode));
            return farmRepository.save(farm);
        });
    }

    public Mono<Farm> getFarm(Long id) {
        return requireFarm(id);
    }

    /**
     * 翻名单：场名、编号、种类、状态任意组合；一个都不填就整份名单分页翻。
     * 种类/状态码非法时在拼条件前就报业务异常。
     */
    public Mono<PageResult<Farm>> pageFarms(int pageNum, int pageSize,
                                            String farmNo, String farmName,
                                            String speciesCode, String statusCode) {
        FarmPageQuery query = new FarmPageQuery(
                blankToNull(farmNo), blankToNull(farmName),
                Species.of(speciesCode), FarmStatus.of(statusCode));
        return farmRepository.page(pageNum, pageSize, query);
    }

    /** 销户：软删除，之后名单里翻不出来。 */
    public Mono<Void> closeFarm(Long id) {
        return requireFarm(id).then(farmRepository.softDelete(id));
    }

    private Mono<Farm> requireFarm(Long id) {
        return farmRepository.findById(id)
                .switchIfEmpty(Mono.error(new BizException("养殖场不存在或已销户：id=" + id)));
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
