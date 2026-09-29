package com.somepro.infrastructure.persistence.eartag;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.somepro.infrastructure.persistence.eartag.po.EarTagPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 畜禽耳标 Mapper（基础设施层），阻塞 JDBC，只能在仓储适配器的 blocking(...) 里调用。
 */
@Mapper
public interface EarTagMapper extends BaseMapper<EarTagPO> {

    /**
     * 取某前缀（如 ET-2026-）下的最大耳标号，含历史已软删记录（编号不回收）。
     */
    @Select("SELECT MAX(tag_no) FROM t_ear_tag WHERE tag_no LIKE #{prefix}")
    String selectMaxTagNo(@Param("prefix") String prefix);
}
