package com.somepro.interfaces.rest.eartag.converter;

import com.somepro.domain.eartag.model.EarTag;
import com.somepro.domain.eartag.model.EarTagStatus;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.domain.shared.model.Species;
import com.somepro.interfaces.rest.eartag.vo.EarTagVO;
import com.somepro.interfaces.rest.shared.PageVO;

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
                d.getFarmNo(),
                d.getSpecies() == null ? null : d.getSpecies().name(),
                d.getSpecies() == null ? null : d.getSpecies().getLabel(),
                d.getIssuedAt(),
                d.getWornAt(),
                d.getStatus() == null ? null : d.getStatus().name(),
                d.getStatus() == null ? null : d.getStatus().getLabel(),
                d.getCreateTime());
    }

    public static PageVO<EarTagVO> toPageVo(PageResult<EarTag> page) {
        List<EarTagVO> content = page.content().stream()
                .map(EarTagVoConverter::toVo)
                .collect(Collectors.toList());
        return new PageVO<>(content, page.total(), page.pageNum(), page.pageSize(), page.totalPages());
    }

    public static Species parseSpecies(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return Species.fromCode(code);
    }

    public static EarTagStatus parseStatus(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return EarTagStatus.fromCode(code);
    }
}
