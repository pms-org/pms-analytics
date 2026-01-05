package com.pms.analytics.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.pms.analytics.dao.AnalysisDao;
import com.pms.analytics.dao.DltOutboxDao;
import com.pms.analytics.dao.entity.AnalysisEntity;
import com.pms.analytics.dao.entity.DltOutbox;
import com.pms.analytics.dto.BatchResult;
import com.pms.analytics.dto.TransactionDto;
import com.pms.analytics.dto.TransactionOuterClass.Transaction;
import com.pms.analytics.exception.InsufficientHoldingsException;
import com.pms.analytics.mapper.TransactionMapper;
import com.pms.analytics.utilities.TradeSide;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final AnalysisDao analysisDao;

    private final DltOutboxDao dltOutboxDao;

    private final IdempotencyService idempotencyService;

    @Transactional
    public BatchResult processBatchInTransaction(List<Transaction> messages) {
        System.out.println("Processing the batch in the transaction service: ");

        List<Transaction> validMessages = new ArrayList<>();
        //create a map here 
        Map<AnalysisEntity.AnalysisKey, AnalysisEntity> cachedAnalysisMap = new HashMap<>();

        Set<AnalysisEntity.AnalysisKey> analysisKeysInBatch = new HashSet<>();

        Set<String> batchTransactionIds = new HashSet<>();

        Set<String> processedTransactionIds = new HashSet<>();

        List<DltOutbox> dltOutboxEntries = new ArrayList<>();

        for (Transaction message : messages) {
            try {

                if (!batchTransactionIds.add(message.getTransactionId())) {
                    System.out.println("Duplicate transaction in same batch: " + message.getTransactionId());
                    continue;
                }

                if (idempotencyService.isDuplicate(message.getTransactionId())) {
                    System.out.println("Transaction: " + message.getTransactionId() + " already processed!");
                    continue;
                }

                validMessages.add(message);

                AnalysisEntity.AnalysisKey analysisKey = new AnalysisEntity.AnalysisKey(
                        UUID.fromString(message.getPortfolioId()),
                        message.getSymbol()
                );

                analysisKeysInBatch.add(analysisKey);

            } catch (RuntimeException ex) {
                System.out.println("Error processing transaction " + message.getTransactionId() + ": " + ex.getMessage());
            }

        }

        // Fetch existing analysis entities for the keys in the batch
        List<AnalysisEntity> existingAnalysisEntities = analysisDao.findAllById(analysisKeysInBatch);

        existingAnalysisEntities.forEach((analysisEntity) -> {
            cachedAnalysisMap.put(analysisEntity.getId(), analysisEntity);
        });

        for (Transaction message : validMessages) {
            try {

                System.out.println("Processing Transaction message: " + message);

                TransactionDto transactionDto = TransactionMapper.fromProto(message);

                processTransaction(transactionDto, cachedAnalysisMap);

                processedTransactionIds.add(message.getTransactionId());
            } catch (RuntimeException ex) {
                System.out.println("Error processing transaction " + message.getTransactionId() + ": " + ex.getMessage());

                DltOutbox dltOutbox = new DltOutbox();
                dltOutbox.setPortfolioId(UUID.fromString(message.getPortfolioId()));
                dltOutbox.setPayload(message.toByteArray());
                dltOutbox.setStatus("PENDING");
                dltOutboxEntries.add(dltOutbox);
            }

        }

        //save all as a batch to db
        List<AnalysisEntity> batchedAnalysisEntities = new ArrayList<>(cachedAnalysisMap.values());

        if (!batchedAnalysisEntities.isEmpty()) {
            log.info("Saving a batch of {} analysis records.", batchedAnalysisEntities.size());
            analysisDao.saveAll(batchedAnalysisEntities);
        }

        if(!dltOutboxEntries.isEmpty())
        {
            log.info("Saving in the Dlt outbox table");
            dltOutboxDao.saveAll(dltOutboxEntries);
        }
        
        return new BatchResult(batchedAnalysisEntities, processedTransactionIds);

    }

    public void processTransaction(TransactionDto dto, Map<AnalysisEntity.AnalysisKey, AnalysisEntity> cachedAnalysisMap) {

        AnalysisEntity.AnalysisKey key
                = new AnalysisEntity.AnalysisKey(dto.getPortfolioId(), dto.getSymbol());

        AnalysisEntity analysisEntity;

        boolean isAlreadyCached = cachedAnalysisMap.containsKey(key);

        if (isAlreadyCached) {

            System.out.println("Using cached AnalysisEntity for key: " + key);
            analysisEntity = cachedAnalysisMap.get(key);

        } else {
            System.out.println("Creating new AnalysisEntity for key: " + key);

            AnalysisEntity newAnalysisEntity = new AnalysisEntity();
            newAnalysisEntity.setId(key);
            newAnalysisEntity.setHoldings(0L);
            newAnalysisEntity.setTotalInvested(BigDecimal.ZERO);
            newAnalysisEntity.setRealizedPnl(BigDecimal.ZERO);

            analysisEntity = newAnalysisEntity;

        }

        if (dto.getSide() == TradeSide.BUY) {
            handleBuy(analysisEntity, dto);
        } else {
            handleSell(analysisEntity, dto);
        }


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
            throw new InsufficientHoldingsException("Insufficient holdings: Trying to sell " + qty + " but only " + currentHoldings + " available.");
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
