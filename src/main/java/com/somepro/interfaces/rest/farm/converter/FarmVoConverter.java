package com.somepro.interfaces.rest.farm.converter;

import com.somepro.domain.farm.model.Farm;
import com.somepro.domain.farm.model.FarmStatus;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.domain.shared.model.Species;
import com.somepro.interfaces.rest.farm.vo.FarmVO;
import com.somepro.interfaces.rest.shared.PageVO;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Farm（领域）→ FarmVO（对外）转换器（用户接口层）。
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
                d.getSpecies() == null ? null : d.getSpecies().getLabel(),
                d.getStockQty(),
                d.getStatus() == null ? null : d.getStatus().name(),
                d.getStatus() == null ? null : d.getStatus().getLabel(),
                d.getCreateTime());
    }

    public static PageVO<FarmVO> toPageVo(PageResult<Farm> page) {
        List<FarmVO> content = page.content().stream()
                .map(FarmVoConverter::toVo)
                .collect(Collectors.toList());
        return new PageVO<>(content, page.total(), page.pageNum(), page.pageSize(), page.totalPages());
    }

    /** 查询参数里的码值转枚举；空白当未传。 */
    public static Species parseSpecies(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return Species.fromCode(code);
    }

    public static FarmStatus parseStatus(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return FarmStatus.fromCode(code);
    }
}
