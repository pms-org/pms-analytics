package com.pms.analytics.mapper;

import com.pms.analytics.dto.TransactionDto;
import com.pms.analytics.dto.TransactionOuterClass.Transaction;


import java.math.BigDecimal;
import java.util.UUID;

public class TransactionMapper {


    public static Transaction toProto(TransactionDto dto) {
        return Transaction.newBuilder()
                .setTransactionId(dto.getTransactionId().toString())
                .setPortfolioId(dto.getPortfolioId().toString())
                .setSymbol(dto.getSymbol())
                .setSide(dto.getSide() != null ? dto.getSide().name() : "UNKNOWN")
                .setBuyPrice(dto.getBuyPrice() != null ? dto.getBuyPrice().toString() : "0")
                .setSellPrice(dto.getSellPrice() != null ? dto.getSellPrice().toString() : "0")
                .setQuantity(dto.getQuantity())
                .build();
    }


    public static TransactionDto fromProto(Transaction proto) {
        return new TransactionDto(
                UUID.fromString(proto.getTransactionId()),
                UUID.fromString(proto.getPortfolioId()),
                proto.getSymbol(),
                com.pms.analytics.utilities.TradeSide.valueOf(proto.getSide()),
                parseBigDecimal(proto.getBuyPrice()),
                parseBigDecimal(proto.getSellPrice()),
                proto.getQuantity()
        );
    }

    private static BigDecimal parseBigDecimal(String value) {
        if (value == null || value.trim().isEmpty() || value.equalsIgnoreCase("NA")) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

}
