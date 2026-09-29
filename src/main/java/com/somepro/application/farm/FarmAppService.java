package com.somepro.application.farm;

import com.somepro.application.support.BizNoGenerator;
import com.somepro.common.exception.BizException;
import com.somepro.domain.farm.model.Farm;
import com.somepro.domain.farm.model.FarmQuery;
import com.somepro.domain.farm.model.FarmStatus;
import com.somepro.domain.farm.repository.FarmRepository;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.domain.shared.model.Species;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

/**
 * 养殖场档案用例（应用层）：编排登记/改/查/销，不写表细节。
 *
 * 出入参都是领域对象，不认识 PO / VO。
 * 编号分配规则：FM-{年份}-{4 位顺序号}，序号取该年已占用最大号 +1（含已软删历史，号不回收）；
 * uk_farm_no 唯一键兜底并发，撞键时重取号重试。
 */
@Service
public class FarmAppService {

    /** 唯一键冲突时的补号重试次数（首次 + 重试合计最多 3 次落库）。 */
    private static final long NO_CONFLICT_RETRY_TIMES = 2;

    private final FarmRepository farmRepository;

    public FarmAppService(FarmRepository farmRepository) {
        this.farmRepository = farmRepository;
    }

    public Mono<Farm> register(String farmName, String ownerName, String phone, String address,
                               Species species, Integer stockQty) {
        return Mono.defer(() -> allocateFarmNo()
                        .map(farmNo -> Farm.register(farmNo, farmName, ownerName, phone, address, species, stockQty))
                        .flatMap(farmRepository::save))
                // uk_farm_no 撞键（并立同号）时重取号重试，首次 + 重试合计最多 3 次落库
                .retryWhen(Retry.max(NO_CONFLICT_RETRY_TIMES).filter(FarmAppService::isDuplicateKey));
    }

    public Mono<Farm> updateInfo(Long id, String farmName, String ownerName, String phone,
                                 String address, Integer stockQty) {
        return requireFarm(id).flatMap(farm -> {
            // 部分更新：只动传了的字段；种类刻意不在这里改
            if (farmName != null || ownerName != null || phone != null || address != null) {
                farm.changeInfo(
                        farmName != null ? farmName : farm.getFarmName(),
                        ownerName != null ? ownerName : farm.getOwnerName(),
                        phone != null ? phone : farm.getPhone(),
                        address != null ? address : farm.getAddress());
            }
            if (stockQty != null) {
                farm.changeStock(stockQty);
            }
            return farmRepository.save(farm);
        });
    }

    public Mono<Farm> changeStatus(Long id, FarmStatus status) {
        return requireFarm(id)
                .doOnNext(farm -> farm.changeStatus(status))
                .flatMap(farmRepository::save);
    }

    public Mono<Farm> get(Long id) {
        return requireFarm(id);
    }

    public Mono<PageResult<Farm>> page(int pageNum, int pageSize, FarmQuery query) {
        return farmRepository.page(pageNum, pageSize, query);
    }

    public Mono<Void> close(Long id) {
        // 先确认账上有这场（且未销过），再软删；销掉后名单自动翻不出来（@TableLogic）
        return requireFarm(id).flatMap(farm -> farmRepository.softDelete(farm.getId()));
    }

    private Mono<Farm> requireFarm(Long id) {
        return farmRepository.findById(id)
                .switchIfEmpty(Mono.error(new BizException("养殖场档案不存在或已注销：id=" + id)));
    }

    private Mono<String> allocateFarmNo() {
        int year = java.time.Year.now().getValue();
        return farmRepository.findMaxFarmNo(year)
                .map(max -> BizNoGenerator.next(FarmRepository.FARM_NO_PREFIX, year, max, 4))
                .defaultIfEmpty(BizNoGenerator.first(FarmRepository.FARM_NO_PREFIX, year, 4));
    }

    private static boolean isDuplicateKey(Throwable t) {
        // MyBatis-Plus/Spring 把 MySQL 1062 翻译成 DuplicateKeyException；
        // 个别驱动包装层级下兜底按 SQLState 23000 / 1062 报文再认一次
        if (t instanceof DuplicateKeyException) {
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
