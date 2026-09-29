package com.somepro.interfaces.rest.eartag.converter;

import com.somepro.domain.eartag.model.EarTag;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.interfaces.rest.common.vo.PageVO;
import com.somepro.interfaces.rest.eartag.vo.EarTagVO;

import java.util.List;
import java.util.stream.Collectors;

/**
 * EarTag（领域）→ EarTagVO（对外）转换器（用户接口层）。
 */
public final class EarTagVoConverter {

    private EarTagVoConverter() {
    }

    public static EarTagVO toVo(EarTag d) {
        return new EarTagVO(
                d.getId(),
                d.getTagNo(),
                d.getFarmId(),
                d.getSpecies() == null ? null : d.getSpecies().name(),
                d.getIssuedAt(),
                d.getWornAt(),
                d.getStatus() == null ? null : d.getStatus().name(),
                d.getCreateTime());
    }

    public static PageVO<EarTagVO> toPageVo(PageResult<EarTag> page) {
        List<EarTagVO> content = page.content().stream()
                .map(EarTagVoConverter::toVo)
                .collect(Collectors.toList());
        return new PageVO<>(content, page.total(), page.pageNum(), page.pageSize(), page.totalPages());
    }
}
