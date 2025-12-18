package com.pms.analytics.service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.pms.analytics.dao.AnalysisDao;
import com.pms.analytics.dao.entity.AnalysisEntity;
import com.pms.analytics.dto.TransactionDto;
import com.pms.analytics.utilities.TradeSide;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransactionService {

    // private final RedisTransactionCache transactionCache;
    private final AnalysisDao analysisDao;

    // @Transactional
    // public void processTransaction(Transaction message) {
    //     TransactionDto dto = TransactionMapper.fromProto(message);
    //     System.out.println("Received Transaction DTO: " + dto);
    //     boolean isProcessed = transactionCache.isDuplicate(dto.getTransactionId().toString());
    //     if (isProcessed) {
    //         System.out.println("Transaction: " + dto.getTransactionId() + " already processed!");
    //         return;
    //     }
    //     processTransaction(dto);
    //     // Mark as processed
    //     transactionCache.markProcessed(dto.getTransactionId().toString());
    // }
   
    public void processTransaction(TransactionDto dto, Map<AnalysisEntity.AnalysisKey, AnalysisEntity> cachedAnalysisMap) {

        AnalysisEntity.AnalysisKey key
                = new AnalysisEntity.AnalysisKey(dto.getPortfolioId(), dto.getSymbol());

        AnalysisEntity analysisEntity;

        boolean isAlreadyCached = cachedAnalysisMap.containsKey(key);

        if (isAlreadyCached) {

            System.out.println("Using cached AnalysisEntity for key: " + key);
            analysisEntity = cachedAnalysisMap.get(key);

        } else {
            System.out.println("Fetching AnalysisEntity from DB for key: " + key);

            Optional<AnalysisEntity> existing = analysisDao.findById(key);

            analysisEntity = existing.orElseGet(() -> {
                AnalysisEntity e = new AnalysisEntity();
                e.setId(key);
                e.setHoldings(0L);
                e.setTotalInvested(BigDecimal.ZERO);
                e.setRealizedPnl(BigDecimal.ZERO);
                return e;
            });
        }

        if (dto.getSide() == TradeSide.BUY) {
            handleBuy(analysisEntity, dto);       
        } else {
            handleSell(analysisEntity, dto);
        }

        // if (dto.getSide() == TradeSide.BUY) {
        //     // Create new if not exists
        //     AnalysisEntity entity = existing.orElseGet(() -> {
        //         AnalysisEntity e = new AnalysisEntity();
        //         e.setId(key);
        //         e.setHoldings(0L);
        //         e.setTotalInvested(BigDecimal.ZERO);
        //         e.setRealizedPnl(BigDecimal.ZERO);
        //         return e;
        //     });
        //     handleBuy(entity, dto);
        //     analysisDao.save(entity);
        //     messagingTemplate.convertAndSend("/topic/position-update",entity);
        // } else { // SELL
        //     if (existing.isEmpty()) {
        //         System.err.println("SELL failed: position does not exist for " + key);
        //         return;
        //     }
        //     AnalysisEntity entity = existing.get();
        //     handleSell(entity, dto);
        //     analysisDao.save(entity);
        //     messagingTemplate.convertAndSend("/topic/position-update",entity);
        // }

        cachedAnalysisMap.put(key, analysisEntity);
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
            System.err.println("SELL failed: insufficient holdings. Trying to sell " + qty
                    + " but only " + currentHoldings + " available.");
            return;
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
