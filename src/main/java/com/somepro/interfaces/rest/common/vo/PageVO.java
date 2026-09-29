package com.somepro.interfaces.rest.common.vo;

import java.io.Serializable;
import java.util.List;

/**
 * 对外分页返回对象（VO，用户接口层，跨业务模块共用）—— 不可变 record。
 *
 * 与领域层 {@code PageResult} 的分工：PageResult 只有 content/total/pageNum/pageSize，
 * 派生字段 totalPages 放接口层，避免把 Jackson 注解带进领域层（同 demo 模块的 PageVO）。
 */
public record PageVO<T>(List<T> content, long total, int pageNum, int pageSize, int totalPages)
        implements Serializable {
}
