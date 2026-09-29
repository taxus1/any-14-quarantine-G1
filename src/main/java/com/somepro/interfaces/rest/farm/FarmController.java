package com.somepro.interfaces.rest.farm;

import com.somepro.application.farm.FarmAppService;
import com.somepro.common.Result;
import com.somepro.interfaces.rest.common.vo.PageVO;
import com.somepro.interfaces.rest.farm.converter.FarmVoConverter;
import com.somepro.interfaces.rest.farm.dto.FarmCreateRequest;
import com.somepro.interfaces.rest.farm.dto.FarmStatusRequest;
import com.somepro.interfaces.rest.farm.dto.FarmUpdateRequest;
import com.somepro.interfaces.rest.farm.vo.FarmVO;
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
 * 养殖场档案接口（用户接口层）：只做协议适配 + VO 转换，编排交给 FarmAppService。
 *
 * 路由：
 * - POST   /api/farms          登记（编号由后端按年自动取号）
 * - PUT    /api/farms/{id}     改正档案（场名/负责人/电话/场址/种类/存栏）
 * - PUT    /api/farms/{id}/status  变更业务状态（在用/停业/注销）
 * - GET    /api/farms/{id}     看一条
 * - GET    /api/farms          翻名单（场名/编号/种类/状态任意组合，可全空，分页）
 * - DELETE /api/farms/{id}     销户（软删除，名单不再翻出）
 */
@RestController
@RequestMapping("/api/farms")
public class FarmController {

    private final FarmAppService farmAppService;

    public FarmController(FarmAppService farmAppService) {
        this.farmAppService = farmAppService;
    }

    @PostMapping
    public Mono<Result<FarmVO>> create(@Valid @RequestBody FarmCreateRequest req) {
        return farmAppService.createFarm(
                        req.farmName(), req.ownerName(), req.phone(), req.address(),
                        req.species(), req.stockQty())
                .map(FarmVoConverter::toVo)
                .map(Result::ok);
    }

    @PutMapping("/{id}")
    public Mono<Result<FarmVO>> update(@PathVariable Long id,
                                       @Valid @RequestBody FarmUpdateRequest req) {
        return farmAppService.updateFarm(
                        id, req.farmName(), req.ownerName(), req.phone(), req.address(),
                        req.species(), req.stockQty())
                .map(FarmVoConverter::toVo)
                .map(Result::ok);
    }

    @PutMapping("/{id}/status")
    public Mono<Result<FarmVO>> updateStatus(@PathVariable Long id,
                                             @Valid @RequestBody FarmStatusRequest req) {
        return farmAppService.updateFarmStatus(id, req.status())
                .map(FarmVoConverter::toVo)
                .map(Result::ok);
    }

    @GetMapping("/{id}")
    public Mono<Result<FarmVO>> get(@PathVariable Long id) {
        return farmAppService.getFarm(id)
                .map(FarmVoConverter::toVo)
                .map(Result::ok);
    }

    @GetMapping
    public Mono<Result<PageVO<FarmVO>>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String farmNo,
            @RequestParam(required = false) String farmName,
            @RequestParam(required = false) String species,
            @RequestParam(required = false) String status) {
        return farmAppService.pageFarms(pageNum, pageSize, farmNo, farmName, species, status)
                .map(FarmVoConverter::toPageVo)
                .map(Result::ok);
    }

    @DeleteMapping("/{id}")
    public Mono<Result<Void>> delete(@PathVariable Long id) {
        return farmAppService.closeFarm(id).thenReturn(Result.ok());
    }
}
