package com.pms.analytics.mapper;

import com.pms.analytics.dto.RiskEventDto;
import com.pms.analytics.dto.RiskEventOuterClass.RiskEvent;

import java.util.UUID;

public class RiskEventMapper {

    public static RiskEvent toProto(RiskEventDto dto) {
        return RiskEvent.newBuilder()
                .setPortfolioId(dto.getPortfolioId().toString())
                .setAvgRateOfReturn(dto.getAvgRateOfReturn())
                .setSharpeRatio(dto.getSharpeRatio())
                .setSortinoRatio(dto.getSortinoRatio())
                .build();
    }

    public static RiskEventDto fromProto(RiskEvent proto) {
        return new RiskEventDto(
                UUID.fromString(proto.getPortfolioId()),
                proto.getAvgRateOfReturn(),
                proto.getSharpeRatio(),
                proto.getSortinoRatio()
        );
    }
}
