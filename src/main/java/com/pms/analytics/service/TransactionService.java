
package com.pms.analytics.service;

import com.pms.analytics.dao.AnalysisDao;
import com.pms.analytics.dao.entity.AnalysisEntity;
import com.pms.analytics.dto.TransactionDto;
import com.pms.analytics.dto.TransactionOuterClass.Transaction;
import com.pms.analytics.externalRedis.RedisTransactionCache;
import com.pms.analytics.mapper.TransactionMapper;
import com.pms.analytics.utilities.TradeSide;

import jakarta.transaction.Transactional;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final RedisTransactionCache transactionCache;
    private final AnalysisDao analysisDao;
    private final SimpMessagingTemplate messagingTemplate;


    @Transactional
    public void processTransaction(Transaction message) {

        TransactionDto dto = TransactionMapper.fromProto(message);
        System.out.println("Received Transaction DTO: " + dto);

        boolean isProcessed = transactionCache.isDuplicate(dto.getTransactionId().toString());
        if (isProcessed) {
            System.out.println("Transaction: " + dto.getTransactionId() + " already processed!");
            return;
        }

        processTransaction(dto);

        // Mark as processed
        transactionCache.markProcessed(dto.getTransactionId().toString());
    }

    private void processTransaction(TransactionDto dto) {

        AnalysisEntity.AnalysisKey key =
                new AnalysisEntity.AnalysisKey(dto.getPortfolioId(), dto.getSymbol());

        Optional<AnalysisEntity> existing = analysisDao.findById(key);

        if (dto.getSide() == TradeSide.BUY) {
            // Create new if not exists
            AnalysisEntity entity = existing.orElseGet(() -> {
                AnalysisEntity e = new AnalysisEntity();
                e.setId(key);
                e.setHoldings(0L);
                e.setTotalInvested(BigDecimal.ZERO);
                e.setRealizedPnl(BigDecimal.ZERO);
                return e;
            });
            handleBuy(entity, dto);
            analysisDao.save(entity);
            messagingTemplate.convertAndSend("/topic/position-update",entity);


        } else { // SELL
            if (existing.isEmpty()) {
                System.err.println("SELL failed: position does not exist for " + key);
                return;
            }
            AnalysisEntity entity = existing.get();
            handleSell(entity, dto);
            analysisDao.save(entity);
            messagingTemplate.convertAndSend("/topic/position-update",entity);
        }
    }


    private void handleBuy(AnalysisEntity entity, TransactionDto dto) {

        long qty = dto.getQuantity();
        BigDecimal price = dto.getBuyPrice();

        entity.setHoldings(entity.getHoldings() + qty);

        BigDecimal invested = price.multiply(BigDecimal.valueOf(qty));
        entity.setTotalInvested(entity.getTotalInvested().add(invested));

        System.out.println("BUY updated: " + entity);
    }

    private void handleSell(AnalysisEntity entity, TransactionDto dto) {

        long qty = dto.getQuantity();
        BigDecimal sellPrice = dto.getSellPrice();
        BigDecimal buyPrice = dto.getBuyPrice(); // Already provided

        long currentHoldings = entity.getHoldings();

        // cannot sell more than current holdings
        if (qty > currentHoldings) {
            qty = currentHoldings;
        }

        // (SellPrice - BuyPrice) * quantity
        BigDecimal pnl = sellPrice.subtract(buyPrice).multiply(BigDecimal.valueOf(qty));
        entity.setRealizedPnl(entity.getRealizedPnl().add(pnl));

        // Reduce holdings & total invested
        entity.setHoldings(currentHoldings - qty);
        BigDecimal investedReduction = buyPrice.multiply(BigDecimal.valueOf(qty));
        entity.setTotalInvested(entity.getTotalInvested().subtract(investedReduction));

        // Reset total invested if no holdings left
        if (entity.getHoldings() == 0) {
            entity.setTotalInvested(BigDecimal.ZERO);
        }

        System.out.println("SELL updated: " + entity);
    }

}
