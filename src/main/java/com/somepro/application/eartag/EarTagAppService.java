package com.somepro.application.eartag;

import com.somepro.application.support.BizNoGenerator;
import com.somepro.common.exception.BizException;
import com.somepro.domain.eartag.model.EarTag;
import com.somepro.domain.eartag.model.EarTagQuery;
import com.somepro.domain.eartag.model.EarTagStatus;
import com.somepro.domain.eartag.repository.EarTagRepository;
import com.somepro.domain.farm.model.Farm;
import com.somepro.domain.farm.repository.FarmRepository;
import com.somepro.domain.shared.model.PageResult;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.LocalDateTime;

/**
 * 畜禽耳标用例（应用层）。
 *
 * 关键规则：耳标种类不用调用方填 —— 挂到哪家场，就照那家场的种类走，
 * 所以登记/改挂前都先把场查出来，species 始终以场档案为唯一来源。
 * 耳标号：ET-{年份}-{6 位顺序号}，uk_tag_no 唯一键兜底并发撞号重试。
 */
@Service
public class EarTagAppService {

    private static final long NO_CONFLICT_RETRY_TIMES = 2;

    private final EarTagRepository earTagRepository;
    private final FarmRepository farmRepository;

    public EarTagAppService(EarTagRepository earTagRepository, FarmRepository farmRepository) {
        this.earTagRepository = earTagRepository;
        this.farmRepository = farmRepository;
    }

    /**
     * 登记新发耳标。
     *
     * @param farmId   挂到哪家场（必传，场必须存在）
     * @param issuedAt 发放时刻，null 取当前
     * @param wornAt   佩戴时刻；非空表示登记时已佩戴（USED），否则默认 ISSUED
     */
    public Mono<EarTag> register(Long farmId, LocalDateTime issuedAt, LocalDateTime wornAt) {
        return requireFarm(farmId).flatMap(farm -> Mono.defer(() -> allocateTagNo()
                        .map(tagNo -> EarTag.issue(tagNo, farm.getId(), farm.getSpecies(), issuedAt, wornAt))
                        .flatMap(earTagRepository::save)
                        // farmNo 不是表字段，落库后回填到场档案编号再返回，方便登记回单直接对号
                        .map(tag -> {
                            tag.setFarmNo(farm.getFarmNo());
                            return tag;
                        }))
                .retryWhen(Retry.max(NO_CONFLICT_RETRY_TIMES).filter(EarTagAppService::isDuplicateKey)));
    }

    /**
     * 改耳标：可改挂场（种类随新场走）、发放时刻、状态与佩戴时刻。所有字段可选，只动传了的。
     */
    public Mono<EarTag> update(Long id, Long farmId, LocalDateTime issuedAt,
                               EarTagStatus status, LocalDateTime wornAt) {
        return requireTag(id).flatMap(tag -> {
            // 改挂场：以新场种类为准；不传则沿用原场（也要在场存在的前提下）
            Long targetFarmId = farmId != null ? farmId : tag.getFarmId();
            return requireFarm(targetFarmId).flatMap(farm -> {
                if (farmId != null) {
                    tag.attachTo(farm.getId(), farm.getSpecies());
                } else if (tag.getSpecies() == null) {
                    // 历史数据缺种类时，用当前场的种类补齐
                    tag.attachTo(farm.getId(), farm.getSpecies());
                }
                if (issuedAt != null) {
                    tag.changeIssuedAt(issuedAt);
                }
                if (status != null) {
                    // 状态是主：非 USED 时即使传了 wornAt 也按规则清掉；USED 没带 wornAt 时领域补当前时刻
                    tag.changeStatus(status, wornAt);
                } else if (wornAt != null) {
                    // 没说改状态、只给佩戴时刻：视为登记佩戴，转 USED
                    tag.markWorn(wornAt);
                }
                return earTagRepository.save(tag)
                        .map(saved -> {
                            saved.setFarmNo(farm.getFarmNo());
                            return saved;
                        });
            });
        });
    }

    public Mono<EarTag> get(Long id) {
        return requireTag(id);
    }

    public Mono<PageResult<EarTag>> page(int pageNum, int pageSize, EarTagQuery query) {
        return earTagRepository.page(pageNum, pageSize, query);
    }

    public Mono<Void> disable(Long id) {
        // “能销”：软删后名单不再翻得出
        return requireTag(id).flatMap(tag -> earTagRepository.softDelete(tag.getId()));
    }

    private Mono<Farm> requireFarm(Long farmId) {
        if (farmId == null) {
            return Mono.error(new BizException("耳标必须挂在一家养殖场，请传 farmId"));
        }
        return farmRepository.findById(farmId)
                .switchIfEmpty(Mono.error(new BizException("所属养殖场档案不存在或已注销：farmId=" + farmId)));
    }

    private Mono<EarTag> requireTag(Long id) {
        return earTagRepository.findById(id)
                .switchIfEmpty(Mono.error(new BizException("耳标记录不存在或已注销：id=" + id)));
    }

    private Mono<String> allocateTagNo() {
        int year = java.time.Year.now().getValue();
        return earTagRepository.findMaxTagNo(year)
                .map(max -> BizNoGenerator.next(EarTagRepository.TAG_NO_PREFIX, year, max, 6))
                .defaultIfEmpty(BizNoGenerator.first(EarTagRepository.TAG_NO_PREFIX, year, 6));
    }

    private static boolean isDuplicateKey(Throwable t) {
        if (t instanceof org.springframework.dao.DuplicateKeyException) {
            return true;
        }
        Throwable c = t;
        while (c != null) {
            if (c instanceof java.sql.SQLException sql) {
                String state = sql.getSQLState();
                return "23000".equals(state) || String.valueOf(sql.getErrorCode()).equals("1062");
            }
            c = c.getCause();
        }
        return false;
    }
}
