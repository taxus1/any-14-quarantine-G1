package com.somepro.infrastructure.persistence.farm;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.somepro.infrastructure.persistence.farm.po.FarmPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 养殖场档案的 MyBatis-Plus Mapper（基础设施层）。
 *
 * BaseMapper 提供常规 CRUD；这里只额外加一个「取年内最大编号」的方法给编号生成用。
 * 该查询刻意手写 SQL：编号即使在记录销户（del_flag=1）后也不能回收复用，否则会和历史号撞，
 * 因此不能走 @TableLogic 自动追加的 del_flag=0，必须把已销户的记录一起数进去。
 *
 * 阻塞（JDBC）API，只能在 boundedElastic 线程上经仓储适配器调用。
 */
@Mapper
public interface FarmMapper extends BaseMapper<FarmPO> {

    /**
     * 取指定前缀（如 FM-2026-）下现存编号的最大值；一个都没有时返回 null。
     * CONCAT 拼参数，前缀仍走 #{} 占位，不存在注入问题。
     *
     * 加 {@code FOR UPDATE} 做「当前读」：在取号短事务 + 命名锁内，
     * 它读取的是最新已提交版本（而非事务开始时的一致性快照），
     * 保证等待锁的后来者拿到前者刚提交的号。
     */
    @Select("SELECT MAX(farm_no) FROM t_farm WHERE farm_no LIKE CONCAT(#{prefix}, '%') FOR UPDATE")
    String selectMaxFarmNo(@Param("prefix") String prefix);
}
