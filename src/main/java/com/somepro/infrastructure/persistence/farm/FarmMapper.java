package com.somepro.infrastructure.persistence.farm;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.somepro.infrastructure.persistence.farm.po.FarmPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 养殖场档案 Mapper（基础设施层），阻塞 JDBC，只能在仓储适配器的 blocking(...) 里调用。
 */
@Mapper
public interface FarmMapper extends BaseMapper<FarmPO> {

    /**
     * 取某前缀（如 FM-2026-）下的最大场编号。
     *
     * ⚠️ 这里刻意手写 SQL 而不走 LambdaQueryWrapper：@TableLogic 会给普通查询自动追加
     * del_flag = 0，但编号不能因软删除而回收复用（删过的号也得认），故必须连历史行一起看。
     */
    @Select("SELECT MAX(farm_no) FROM t_farm WHERE farm_no LIKE #{prefix}")
    String selectMaxFarmNo(@Param("prefix") String prefix);
}
