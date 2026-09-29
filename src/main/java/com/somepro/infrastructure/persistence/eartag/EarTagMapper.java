package com.somepro.infrastructure.persistence.eartag;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.somepro.infrastructure.persistence.eartag.po.EarTagPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 耳标的 MyBatis-Plus Mapper（基础设施层）。
 *
 * 额外的「取年内最大耳标号」方法给编号生成用：刻意手写不带 del_flag 过滤的 SQL，
 * 已核销（del_flag=1）耳标的号也要占位，号不回收、不与历史号撞。
 *
 * 阻塞（JDBC）API，只能在 boundedElastic 线程上经仓储适配器调用。
 */
@Mapper
public interface EarTagMapper extends BaseMapper<EarTagPO> {

    /**
     * 取指定前缀（如 ET-2026-）下现存耳标号的最大值；一个都没有时返回 null。
     * 加 {@code FOR UPDATE} 做当前读，配合取号短事务 + 命名锁读到最新已提交号。
     */
    @Select("SELECT MAX(tag_no) FROM t_ear_tag WHERE tag_no LIKE CONCAT(#{prefix}, '%') FOR UPDATE")
    String selectMaxTagNo(@Param("prefix") String prefix);
}
