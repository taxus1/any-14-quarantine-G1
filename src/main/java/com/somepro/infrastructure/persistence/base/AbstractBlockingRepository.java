package com.somepro.infrastructure.persistence.base;

import com.somepro.infrastructure.config.ReactiveOperatorContext;
import com.somepro.infrastructure.persistence.audit.AuditContextHolder;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.function.Supplier;

/**
 * 仓储适配器共用的「响应式外壳 × 阻塞 JDBC」桥接基类（基础设施层）。
 *
 * 红线与 DemoItemRepositoryImpl 里那份私有 blocking 完全一致：
 * 1. 先 deferContextual 取 Reactor Context 里的操作人（切线程后就读不到了）；
 * 2. 再 subscribeOn(boundedElastic) —— 绝不能在 Netty event-loop 上跑 JDBC；
 * 3. 操作人放进 AuditContextHolder 供 MetaObjectHandler 填 createBy/updateBy。
 *
 * 新业务模块的仓储适配器直接继承它，Mapper 只允许在 {@link #blocking} 的 supplier 里调用。
 */
public abstract class AbstractBlockingRepository {

    protected <T> Mono<T> blocking(Supplier<T> supplier) {
        return Mono.deferContextual(ctx -> {
            String operator = ReactiveOperatorContext.getOperator(ctx);
            return Mono.fromCallable(() -> {
                AuditContextHolder.setOperator(operator);
                try {
                    return supplier.get();
                } finally {
                    AuditContextHolder.clear();
                }
            }).subscribeOn(Schedulers.boundedElastic());
        });
    }
}
