package com.somepro.interfaces.rest.farm;

import com.somepro.application.farm.FarmAppService;
import com.somepro.common.Result;
import com.somepro.domain.farm.model.FarmQuery;
import com.somepro.domain.farm.model.FarmStatus;
import com.somepro.domain.shared.model.Species;
import com.somepro.interfaces.rest.farm.converter.FarmVoConverter;
import com.somepro.interfaces.rest.farm.dto.CreateFarmRequest;
import com.somepro.interfaces.rest.farm.dto.UpdateFarmRequest;
import com.somepro.interfaces.rest.farm.dto.UpdateFarmStatusRequest;
import com.somepro.interfaces.rest.farm.vo.FarmVO;
import com.somepro.interfaces.rest.shared.PageVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * 养殖场档案接口（用户接口层）：只做协议适配 + VO 转换，业务编排交给 FarmAppService。
 *
 * 场编号由系统分配（FM-年份-4 位序号），不在登记入参里；销场走 DELETE（软删，名单不再翻出），
 * 状态变更（停业/复业/注销标记）走单独的 status 接口。
 */
@RestController
@RequestMapping("/api/farms")
public class FarmController {

    private final FarmAppService farmAppService;

    public FarmController(FarmAppService farmAppService) {
        this.farmAppService = farmAppService;
    }

    /** 登记新场。 */
    @PostMapping
    public Mono<Result<FarmVO>> create(@Valid @RequestBody CreateFarmRequest req) {
        return farmAppService.register(
                        req.farmName(), req.ownerName(), req.phone(), req.address(),
                        Species.fromCode(req.species()), req.stockQty())
                .map(FarmVoConverter::toVo)
                .map(Result::ok);
    }

    /**
     * 翻场名单：farmNo / farmName / species / status 任意组合，全不填翻整份；
     * pageNum/pageSize 透传，按固定顺序分页，行间带 farmNo 对号。
     */
    @GetMapping
    public Mono<Result<PageVO<FarmVO>>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String farmNo,
            @RequestParam(required = false) String farmName,
            @RequestParam(required = false) String species,
            @RequestParam(required = false) String status) {
        FarmQuery query = new FarmQuery(farmNo, farmName,
                FarmVoConverter.parseSpecies(species), FarmVoConverter.parseStatus(status));
        return farmAppService.page(pageNum, pageSize, query)
                .map(FarmVoConverter::toPageVo)
                .map(Result::ok);
    }

    /** 看单场档案。 */
    @GetMapping("/{id}")
    public Mono<Result<FarmVO>> get(@PathVariable Long id) {
        return farmAppService.get(id)
                .map(FarmVoConverter::toVo)
                .map(Result::ok);
    }

    /** 改档案（场名/负责人/电话/场址/存栏，部分更新；种类不可改）。 */
    @PutMapping("/{id}")
    public Mono<Result<FarmVO>> update(@PathVariable Long id,
                                       @Valid @RequestBody UpdateFarmRequest req) {
        return farmAppService.updateInfo(id, req.farmName(), req.ownerName(), req.phone(),
                        req.address(), req.stockQty())
                .map(FarmVoConverter::toVo)
                .map(Result::ok);
    }

    /** 变状态（ACTIVE 在用 / SUSPENDED 停业 / CLOSED 注销）。 */
    @PutMapping("/{id}/status")
    public Mono<Result<FarmVO>> changeStatus(@PathVariable Long id,
                                             @Valid @RequestBody UpdateFarmStatusRequest req) {
        return farmAppService.changeStatus(id, FarmStatus.fromCode(req.status()))
                .map(FarmVoConverter::toVo)
                .map(Result::ok);
    }

    /** 销场：软删，之后名单里翻不出来。 */
    @DeleteMapping("/{id}")
    public Mono<Result<Void>> close(@PathVariable Long id) {
        return farmAppService.close(id).then(Mono.just(Result.ok()));
    }
}
