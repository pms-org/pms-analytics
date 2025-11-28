package com.pms.analytics.service;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pms.analytics.dao.PnlDao;
import com.pms.analytics.dao.PositionDao;
import com.pms.analytics.dao.SectorAnalysisDao;
import com.pms.analytics.dao.SectorDao;
import com.pms.analytics.dao.entity.PnlEntity;
import com.pms.analytics.dao.entity.PositionEntity;
import com.pms.analytics.dao.entity.PositionEntity.PositionKey;
import com.pms.analytics.dao.entity.SectorAnalysisEntity;
import com.pms.analytics.dao.entity.SectorAnalysisEntity.SectorAnalysisKey;
import com.pms.analytics.dao.entity.SectorEntity;
import com.pms.analytics.dto.TransactionDto;
import com.pms.analytics.utilities.TradeSide;

import jakarta.transaction.Transactional;

@Service
public class TransactionListenerService {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PnlDao pnlDao;

    @Autowired
    private PositionDao positionDao;

    @Autowired
    private SectorDao sectorDao;

    @Autowired
    private SectorAnalysisDao sectorAnalysisDao;

    @Autowired
    ExternalPriceClient externalPriceClient;

    public TransactionListenerService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Transactional
    @KafkaListener(topics = "${app.kafka-topic}", groupId = "transaction-group")
    public void listen(@Payload(required = true) String message) {

        System.out.println("Raw message: " + message);

        try {
            TransactionDto transactionDto = objectMapper.readValue(message, TransactionDto.class);

            System.out.println("Converted DTO: " + transactionDto);

            Optional<PositionEntity> position = positionDao.findById(new PositionKey(transactionDto.getPortfolioId(),transactionDto.getSymbol()));
            
            Optional<PnlEntity> existing = pnlDao.findById(transactionDto.getTransactionId());
            
            SectorEntity sector = sectorDao.findById(transactionDto.getSymbol()).get();
            
            Optional<SectorAnalysisEntity> sectorAnalysis = sectorAnalysisDao.findById(new SectorAnalysisKey(transactionDto.getPortfolioId(), sector.getSector()));

            if(existing.isPresent()){
                System.out.println("Existing transction should be handled");
            }
            
            else{
                System.out.println("No Existing transction");
                PnlEntity newPnl = new PnlEntity();
                newPnl.setTransactionId(transactionDto.getTransactionId());
                newPnl.setPortfolioId(transactionDto.getPortfolioId());
                newPnl.setSymbol(transactionDto.getSymbol());
                newPnl.setTimestamp(transactionDto.getTimestamp());
                if(transactionDto.getSide().equals(TradeSide.BUY)) {
                    newPnl.setBuyPrice(transactionDto.getBuyPrice());
                    newPnl.setRemainingQuantity(transactionDto.getRemainingQuantity());
                    BigDecimal currentPrice = externalPriceClient.getCurrentPrice(transactionDto.getSymbol());
                    BigDecimal unrealizedPnl = (BigDecimal) (currentPrice.subtract(transactionDto.getBuyPrice())).multiply(BigDecimal.valueOf(transactionDto.getRemainingQuantity()));
                    newPnl.setUnrealizedPnl(unrealizedPnl);
                    System.out.println("Unrealized PnL: "+ unrealizedPnl);
                    if(position.isPresent()){
                        PositionEntity pos = position.get();
                        long holdings = pos.getHoldings();
                        pos.setHoldings(holdings+transactionDto.getRemainingQuantity());
                        System.out.println("Position: " + pos);
                        positionDao.save(pos);
                    }else{
                        PositionEntity pos = new PositionEntity();
                        pos.setId(new PositionKey(transactionDto.getPortfolioId(),transactionDto.getSymbol()));
                        pos.setHoldings(transactionDto.getRemainingQuantity());
                        System.out.println("Position: " + pos);
                        positionDao.saveAndFlush(pos);
                    }
                    if(sectorAnalysis.isPresent()){
                        SectorAnalysisEntity secAnalysis = sectorAnalysis.get();
                        BigDecimal invested = secAnalysis.getInvestedPrice();
                        BigDecimal quantity = BigDecimal.valueOf(transactionDto.getQuantity());
                        secAnalysis.setInvestedPrice(invested.add(transactionDto.getBuyPrice().multiply(quantity)));
                        sectorAnalysisDao.save(secAnalysis);
                    }else{
                        SectorAnalysisEntity secAnalysis = new SectorAnalysisEntity();
                        secAnalysis.setId(new SectorAnalysisKey(transactionDto.getPortfolioId(), sector.getSector()));
                        BigDecimal quantity = BigDecimal.valueOf(transactionDto.getQuantity());
                        secAnalysis.setInvestedPrice(transactionDto.getBuyPrice().multiply(quantity));
                        sectorAnalysisDao.save(secAnalysis);
                    }
                }else{
                    newPnl.setRemainingQuantity(transactionDto.getRemainingQuantity());
                    newPnl.setBuyPrice(null);
                    BigDecimal realizedPnl = (BigDecimal) (transactionDto.getSellPrice().subtract(transactionDto.getBuyPrice())).multiply(BigDecimal.valueOf(transactionDto.getQuantity()));
                    newPnl.setRealizedPnl(realizedPnl);
                    System.out.println("Realized PnL: "+ realizedPnl);
                    if(position.isPresent()){
                        PositionEntity pos = position.get();
                        long holdings = pos.getHoldings();
                        pos.setHoldings(holdings-transactionDto.getQuantity());
                        System.out.println("Position: " + pos);
                        positionDao.save(pos);
                    }
                    if(sectorAnalysis.isPresent()){
                        SectorAnalysisEntity secAnalysis = sectorAnalysis.get();
                        BigDecimal invested = secAnalysis.getInvestedPrice();
                        BigDecimal quantity = BigDecimal.valueOf(transactionDto.getQuantity());
                        secAnalysis.setInvestedPrice(invested.subtract(transactionDto.getBuyPrice().multiply(quantity)));
                        sectorAnalysisDao.save(secAnalysis);
                    }
                }
                System.out.println("NewPnl: " + newPnl);
                pnlDao.saveAndFlush(newPnl);
            }
        } catch (Exception e) {
            System.err.println("Failed to parse JSON: " + e.getMessage());
        }
    }
}
