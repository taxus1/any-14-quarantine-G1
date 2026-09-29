package com.somepro.interfaces.rest.eartag;

import com.somepro.application.eartag.EarTagAppService;
import com.somepro.common.Result;
import com.somepro.interfaces.rest.common.vo.PageVO;
import com.somepro.interfaces.rest.eartag.converter.EarTagVoConverter;
import com.somepro.interfaces.rest.eartag.dto.EarTagCreateRequest;
import com.somepro.interfaces.rest.eartag.dto.EarTagUpdateRequest;
import com.somepro.interfaces.rest.eartag.vo.EarTagVO;
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
 * 畜禽耳标接口（用户接口层）：只做协议适配 + VO 转换，编排交给 EarTagAppService。
 *
 * 路由：
 * - POST   /api/ear-tags          发放登记（只给场，种类随场，号由后端取）
 * - PUT    /api/ear-tags/{id}     改正（改挂场/发放时刻/佩戴时刻/状态，空字段不动）
 * - GET    /api/ear-tags/{id}     看一枚
 * - GET    /api/ear-tags          翻名单（按场/种类/状态任意组合，可全空，分页）
 * - DELETE /api/ear-tags/{id}     核销（软删除，名单不再翻出）
 */
@RestController
@RequestMapping("/api/ear-tags")
public class EarTagController {

    private final EarTagAppService earTagAppService;

    public EarTagController(EarTagAppService earTagAppService) {
        this.earTagAppService = earTagAppService;
    }

    @PostMapping
    public Mono<Result<EarTagVO>> create(@Valid @RequestBody EarTagCreateRequest req) {
        return earTagAppService.issueTag(req.farmId(), req.issuedAt())
                .map(EarTagVoConverter::toVo)
                .map(Result::ok);
    }

    @PutMapping("/{id}")
    public Mono<Result<EarTagVO>> update(@PathVariable Long id,
                                         @Valid @RequestBody EarTagUpdateRequest req) {
        return earTagAppService.updateTag(id, req.farmId(), req.issuedAt(), req.wornAt(), req.status())
                .map(EarTagVoConverter::toVo)
                .map(Result::ok);
    }

    @GetMapping("/{id}")
    public Mono<Result<EarTagVO>> get(@PathVariable Long id) {
        return earTagAppService.getTag(id)
                .map(EarTagVoConverter::toVo)
                .map(Result::ok);
    }

    @GetMapping
    public Mono<Result<PageVO<EarTagVO>>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long farmId,
            @RequestParam(required = false) String species,
            @RequestParam(required = false) String status) {
        return earTagAppService.pageTags(pageNum, pageSize, farmId, species, status)
                .map(EarTagVoConverter::toPageVo)
                .map(Result::ok);
    }

    @DeleteMapping("/{id}")
    public Mono<Result<Void>> delete(@PathVariable Long id) {
        return earTagAppService.disableTag(id).thenReturn(Result.ok());
    }
}
