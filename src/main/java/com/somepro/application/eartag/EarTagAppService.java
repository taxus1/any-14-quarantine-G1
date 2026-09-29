package com.somepro.application.eartag;

import com.somepro.common.exception.BizException;
import com.somepro.domain.eartag.model.EarTag;
import com.somepro.domain.eartag.model.EarTagPageQuery;
import com.somepro.domain.eartag.model.EarTagStatus;
import com.somepro.domain.eartag.repository.EarTagRepository;
import com.somepro.domain.farm.model.Farm;
import com.somepro.domain.farm.repository.FarmRepository;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.domain.shared.model.Species;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * 畜禽耳标应用服务：编排用例，业务规则在领域聚合 EarTag。
 *
 * 耳标种类不由调用方填：登记/改挂时先读所属养殖场档案，用场的种类，
 * 保证「场里怎么定的种类，耳标就照什么样」，不会场里场外对不上。
 * 耳标号在仓储落库时自动取号。
 */
@Service
public class EarTagAppService {

    private final EarTagRepository earTagRepository;
    private final FarmRepository farmRepository;

    public EarTagAppService(EarTagRepository earTagRepository, FarmRepository farmRepository) {
        this.earTagRepository = earTagRepository;
        this.farmRepository = farmRepository;
    }

    /**
     * 发放（登记）一枚新耳标：
     * 只给归属场，种类照场里的走；发放时刻没传就取登记时刻；状态默认「已发放待佩戴」。
     */
    public Mono<EarTag> issueTag(Long farmId, LocalDateTime issuedAt) {
        return requireFarm(farmId).flatMap(farm -> {
            EarTag tag = EarTag.issue(farm.getId(), farm.getSpecies(),
                    issuedAt != null ? issuedAt : LocalDateTime.now());
            return earTagRepository.save(tag);
        });
    }

    /**
     * 改正耳标：可改归属场（种类随之换）、发放/佩戴时刻、状态。
     * 参数为 {@code null} 表示该项不动。
     */
    public Mono<EarTag> updateTag(Long id, Long farmId, LocalDateTime issuedAt,
                                  LocalDateTime wornAt, String statusCode) {
        return requireTag(id).flatMap(tag -> {
            // 改挂场才重新读场、种类随场换；不改挂就维持耳标现有的归属与种类
            Mono<Farm> farmMono = farmId != null
                    ? requireFarm(farmId)
                    : Mono.empty();
            return farmMono
                    .flatMap(farm -> {
                        tag.reattach(farm.getId(), farm.getSpecies());
                        return Mono.just(tag);
                    })
                    .defaultIfEmpty(tag)
                    .flatMap(t -> {
                        if (issuedAt != null || wornAt != null) {
                            t.updateTimes(issuedAt, wornAt);
                        }
                        if (statusCode != null) {
                            t.changeStatus(EarTagStatus.of(statusCode), wornAt);
                        }
                        return earTagRepository.save(t);
                    });
        });
    }

    /** 单查一枚耳标。 */
    public Mono<EarTag> getTag(Long id) {
        return requireTag(id);
    }

    /** 翻名单：按场、种类、状态任意组合；一个都不填就整份名单分页翻。 */
    public Mono<PageResult<EarTag>> pageTags(int pageNum, int pageSize,
                                             Long farmId, String speciesCode, String statusCode) {
        EarTagPageQuery query = new EarTagPageQuery(
                farmId, Species.of(speciesCode), EarTagStatus.of(statusCode));
        return earTagRepository.page(pageNum, pageSize, query);
    }

    /** 销一枚耳标：软删除，之后名单里翻不出来。 */
    public Mono<Void> disableTag(Long id) {
        return requireTag(id).then(earTagRepository.softDelete(id));
    }

    private Mono<EarTag> requireTag(Long id) {
        return earTagRepository.findById(id)
                .switchIfEmpty(Mono.error(new BizException("耳标不存在或已核销：id=" + id)));
    }

    private Mono<Farm> requireFarm(Long farmId) {
        return farmRepository.findById(farmId)
                .switchIfEmpty(Mono.error(new BizException("耳标归属的养殖场不存在或已销户：farmId=" + farmId)));
    }
}
