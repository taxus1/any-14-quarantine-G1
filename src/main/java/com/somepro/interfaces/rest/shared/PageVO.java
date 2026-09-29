package com.somepro.interfaces.rest.shared;

import java.io.Serializable;
import java.util.List;

/**
 * 对外分页返回对象（用户接口层，各业务模块共用）—— 不可变 record。
 *
 * 与领域层 {@code PageResult} 的分工同 demo 模块：totalPages 是派生展示字段，
 * 留在接口层，领域 PageResult 保持零框架依赖（record 组件即序列化字段）。
 */
public record PageVO<T>(List<T> content, long total, int pageNum, int pageSize, int totalPages)
        implements Serializable {
}
