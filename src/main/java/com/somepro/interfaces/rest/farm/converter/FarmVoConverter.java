package com.somepro.interfaces.rest.farm.converter;

import com.somepro.domain.farm.model.Farm;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.interfaces.rest.common.vo.PageVO;
import com.somepro.interfaces.rest.farm.vo.FarmVO;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Farm（领域）→ FarmVO（对外）转换器（用户接口层）。
 * Controller 不直接返回领域对象，避免内部字段被无意识序列化出去。
 */
public final class FarmVoConverter {

    private FarmVoConverter() {
    }

    public static FarmVO toVo(Farm d) {
        return new FarmVO(
                d.getId(),
                d.getFarmNo(),
                d.getFarmName(),
                d.getOwnerName(),
                d.getPhone(),
                d.getAddress(),
                d.getSpecies() == null ? null : d.getSpecies().name(),
                d.getStockQty(),
                d.getStatus() == null ? null : d.getStatus().name(),
                d.getCreateTime());
    }

    public static PageVO<FarmVO> toPageVo(PageResult<Farm> page) {
        List<FarmVO> content = page.content().stream()
                .map(FarmVoConverter::toVo)
                .collect(Collectors.toList());
        return new PageVO<>(content, page.total(), page.pageNum(), page.pageSize(), page.totalPages());
    }
}
