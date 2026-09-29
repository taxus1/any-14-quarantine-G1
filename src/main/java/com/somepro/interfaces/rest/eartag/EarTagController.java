package com.somepro.interfaces.rest.eartag;

import com.somepro.application.eartag.EarTagAppService;
import com.somepro.common.Result;
import com.somepro.domain.eartag.model.EarTagQuery;
import com.somepro.interfaces.rest.eartag.converter.EarTagVoConverter;
import com.somepro.interfaces.rest.eartag.dto.CreateEarTagRequest;
import com.somepro.interfaces.rest.eartag.dto.UpdateEarTagRequest;
import com.somepro.interfaces.rest.eartag.vo.EarTagVO;
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
 * 畜禽耳标接口（用户接口层）。
 *
 * 登记时不传种类：挂到哪家场就照那家场的种类走。每行返回 tagNo 与所属场 farmNo 方便对号。
 */
@RestController
@RequestMapping("/api/ear-tags")
public class EarTagController {

    private final EarTagAppService earTagAppService;

    public EarTagController(EarTagAppService earTagAppService) {
        this.earTagAppService = earTagAppService;
    }

    /** 登记新耳标（默认 ISSUED；传 wornAt 则登记即佩戴 USED）。 */
    @PostMapping
    public Mono<Result<EarTagVO>> create(@Valid @RequestBody CreateEarTagRequest req) {
        return earTagAppService.register(req.farmId(), req.issuedAt(), req.wornAt())
                .map(EarTagVoConverter::toVo)
                .map(Result::ok);
    }

    /**
     * 翻耳标名单：tagNo / farmId / species / status 任意组合，全不填翻整份；
     * 每行带 tagNo，另带 farmNo 方便和场名单对号。
     */
    @GetMapping
    public Mono<Result<PageVO<EarTagVO>>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String tagNo,
            @RequestParam(required = false) Long farmId,
            @RequestParam(required = false) String species,
            @RequestParam(required = false) String status) {
        EarTagQuery query = new EarTagQuery(tagNo, farmId,
                EarTagVoConverter.parseSpecies(species), EarTagVoConverter.parseStatus(status));
        return earTagAppService.page(pageNum, pageSize, query)
                .map(EarTagVoConverter::toPageVo)
                .map(Result::ok);
    }

    /** 看单条耳标。 */
    @GetMapping("/{id}")
    public Mono<Result<EarTagVO>> get(@PathVariable Long id) {
        return earTagAppService.get(id)
                .map(EarTagVoConverter::toVo)
                .map(Result::ok);
    }

    /** 改耳标：改挂场（种类随场走）/发放时刻/状态/佩戴时刻，部分更新。 */
    @PutMapping("/{id}")
    public Mono<Result<EarTagVO>> update(@PathVariable Long id,
                                         @Valid @RequestBody UpdateEarTagRequest req) {
        return earTagAppService.update(id, req.farmId(), req.issuedAt(),
                        EarTagVoConverter.parseStatus(req.status()), req.wornAt())
                .map(EarTagVoConverter::toVo)
                .map(Result::ok);
    }

    /** 销耳标：软删，之后名单里翻不出来。 */
    @DeleteMapping("/{id}")
    public Mono<Result<EarTagVO>> disable(@PathVariable Long id) {
        return earTagAppService.disable(id).then(Mono.just(Result.ok()));
    }
}
