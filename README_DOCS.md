# Analytics Service - Documentation Index

**Status:** ✅ Fully Operational  
**Last Updated:** January 29, 2026

---

## Quick Links

| Document | Purpose | When to Use |
|----------|---------|-------------|
| **[LLM_FIX_IT_GUIDE.md](./LLM_FIX_IT_GUIDE.md)** | 🚀 Quick LLM prompts for instant fixes | **START HERE** - Copy-paste into LLM for fast help |
| **[TROUBLESHOOTING_GUIDE.md](./TROUBLESHOOTING_GUIDE.md)** | 🔧 Complete technical troubleshooting | Deep-dive into issues and solutions |
| **[ANALYTICS_SERVICE_INVESTIGATION.md](./ANALYTICS_SERVICE_INVESTIGATION.md)** | 📊 Full investigation report | Understanding root causes and architecture |
| **[RESOLUTION_SUMMARY.md](./RESOLUTION_SUMMARY.md)** | ✅ Complete resolution summary | Overview of all changes made |
| **[TEST_VERIFICATION.md](./TEST_VERIFICATION.md)** | ✔️ Test results and verification | Proof that system is working |

---

## Common Issues - Quick Fix

### ❌ "Unable to connect to Redis"
**Fix:** Update `RedisConfig.java` to add password authentication  
**Details:** [LLM_FIX_IT_GUIDE.md](./LLM_FIX_IT_GUIDE.md#if-you-see-unable-to-connect-to-redis)

### ❌ "Character N is neither a decimal"
**Fix:** Add `parseBigDecimal()` helper in `TransactionMapper.java`  
**Details:** [LLM_FIX_IT_GUIDE.md](./LLM_FIX_IT_GUIDE.md#if-you-see-character-n-is-neither-a-decimal)

### ❌ "Insufficient holdings"
**Fix:** Skip instead of throw in `TransactionService.java`  
**Details:** [LLM_FIX_IT_GUIDE.md](./LLM_FIX_IT_GUIDE.md#if-you-see-insufficient-holdings)

### ❌ "relation does not exist"
**Fix:** Create `analytics_portfolio_risk_status` table  
**Details:** [LLM_FIX_IT_GUIDE.md](./LLM_FIX_IT_GUIDE.md#if-you-see-relation-does-not-exist)

### ❌ "Fetched 0 from outbox"
**Fix:** Populate historical data with `scripts/populate_historical_data.sql`  
**Details:** [LLM_FIX_IT_GUIDE.md](./LLM_FIX_IT_GUIDE.md#if-you-see-fetched-0-from-outbox)

---

## One-Command Diagnosis

```bash
# Run all diagnosis commands
kubectl logs deployment/analytics -n pms --tail=200 | grep -i error && \
kubectl exec deployment/postgres -n pms -- psql -U pms -d pmsdb -c "
SELECT 'analytics' as table, COUNT(*) FROM analytics
UNION ALL SELECT 'analytics_outbox', COUNT(*) FROM analytics_outbox
UNION ALL SELECT 'analytics_portfolio_value_history', COUNT(*) FROM analytics_portfolio_value_history
UNION ALL SELECT 'analytics_portfolio_risk_status', COUNT(*) FROM analytics_portfolio_risk_status;
"
```

---

## Quick Start for LLM Assistance

1. **Open [LLM_FIX_IT_GUIDE.md](./LLM_FIX_IT_GUIDE.md)**
2. **Copy the "Quick Start" section**
3. **Paste into your LLM conversation**
4. **Add your specific error logs**
5. **Follow LLM's step-by-step instructions**

---

## Project Structure

```
pms-analytics/
├── src/main/java/com/pms/analytics/
│   ├── config/
│   │   └── RedisConfig.java                    # ✅ Fixed: Password auth
│   ├── mapper/
│   │   └── TransactionMapper.java              # ✅ Fixed: BigDecimal parsing
│   ├── service/
│   │   ├── TransactionService.java             # ✅ Fixed: Skip insufficient
│   │   ├── BatchProcessingService.java         # ✅ Fixed: Trigger risk calc
│   │   └── RiskMetricsCalculator.java          # Risk metrics computation
│   └── scheduler/
│       ├── PriceUpdateScheduler.java           # Every 30 seconds
│       └── PortfolioValueScheduler.java        # Daily at 23:59
├── scripts/
│   └── populate_historical_data.sql            # ✅ Generate test data
├── TROUBLESHOOTING_GUIDE.md                    # 🔧 Technical deep-dive
├── LLM_FIX_IT_GUIDE.md                         # 🚀 Quick LLM prompts
├── ANALYTICS_SERVICE_INVESTIGATION.md          # 📊 Investigation report
├── RESOLUTION_SUMMARY.md                       # ✅ Complete summary
├── TEST_VERIFICATION.md                        # ✔️ Test results
└── README_DOCS.md                              # 📖 This file
```

---

## System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    ANALYTICS SERVICE                        │
└─────────────────────────────────────────────────────────────┘

Kafka (transactional-trades-topic)
  │
  ▼
TransactionMapper.parseBigDecimal()     ← Handles "NA" values
  │
  ▼
TransactionService.processBatch()       ← Skips insufficient holdings
  │
  ├─► AnalysisEntity (PostgreSQL)       ← Holdings, P&L tracking
  │
  ├─► WebSocket Broadcast               ← Real-time position updates
  │
  └─► RiskMetricsCalculator             ← Triggered after batch
        │
        ├─► Requires 29+ days history
        │   (analytics_portfolio_value_history)
        │
        ├─► Computes VaR, Sharpe, Sortino
        │
        └─► AnalysisOutbox (PostgreSQL) ← Risk events
              │
              ▼
        OutboxDispatcher
              │
              ▼
        Kafka (analytics-trades-topic)   ← Published events

Schedulers:
- PriceUpdateScheduler: Every 30s (refresh prices)
- PortfolioValueScheduler: Daily 23:59 IST (snapshot values)
```

---

## Key Components

### Database Tables
- **analytics:** Current portfolio positions (holdings, P&L)
- **analytics_outbox:** Risk events to publish (outbox pattern)
- **analytics_portfolio_value_history:** Daily portfolio snapshots
- **analytics_portfolio_risk_status:** Risk calculation tracking

### Schedulers
- **PriceUpdateScheduler:** Every 30 seconds
  - Fetches latest prices from external source
  - Updates Redis cache
  - Computes unrealized P&L
  - Triggers risk calculation

- **PortfolioValueScheduler:** Daily at 23:59 Asia/Kolkata
  - Calculates total portfolio value
  - Saves daily snapshot
  - Builds historical data for risk metrics

### Risk Metrics
- **Value at Risk (VaR):** Maximum expected loss
- **Sharpe Ratio:** Risk-adjusted return
- **Sortino Ratio:** Downside risk-adjusted return
- **Average Rate of Return:** Mean daily return

---

## Configuration

### Environment Variables
```yaml
# Redis
SPRING_DATA_REDIS_PASSWORD: redis              # ✅ CRITICAL
SPRING_DATA_REDIS_SENTINEL_MASTER: pms-redis
SPRING_DATA_REDIS_SENTINEL_NODES: redis-sentinel:26379

# Schedulers
ANALYTICS_PORTFOLIO_VALUE_CRON: "0 59 23 * * ?"
ANALYTICS_PORTFOLIO_VALUE_TIMEZONE: Asia/Kolkata
ANALYTICS_PRICE_REFRESH_DELAY_MS: 30000

# Risk Calculation
ANALYTICS_RISK_DECIMAL_SCALE: 8
ANALYTICS_RISK_MATH_PRECISION: 10
```

---

## Testing

### Populate Test Data
```bash
kubectl exec -i deployment/postgres -n pms -- psql -U pms -d pmsdb < \
  scripts/populate_historical_data.sql
```

### Verify System Health
```bash
# Check transactions processing
kubectl logs deployment/analytics -n pms --tail=50 | grep "Saving a batch"

# Check risk calculation
kubectl logs deployment/analytics -n pms --tail=50 | grep "Computing risk"

# Check outbox
kubectl exec deployment/postgres -n pms -- psql -U pms -d pmsdb -c \
  "SELECT status, COUNT(*) FROM analytics_outbox GROUP BY status;"
```

### Expected Healthy Output
```
✅ Batches processing every ~30 seconds
✅ Risk metrics computed for all portfolios
✅ Outbox entries created and sent to Kafka
✅ No errors in logs
```

---

## Build & Deploy

```bash
# Build
cd /mnt/c/Developer/pms-org/pms-analytics
docker build -t niishantdev/pms-analytics:latest .

# Push
docker push niishantdev/pms-analytics:latest

# Deploy
kubectl rollout restart deployment/analytics -n pms
kubectl rollout status deployment/analytics -n pms --timeout=120s
```

---

## Support

### I'm seeing errors in the logs
1. Check [LLM_FIX_IT_GUIDE.md](./LLM_FIX_IT_GUIDE.md) for quick fixes
2. Copy error message into LLM with context from guide
3. Follow step-by-step instructions

### I need to understand the architecture
1. Read [ANALYTICS_SERVICE_INVESTIGATION.md](./ANALYTICS_SERVICE_INVESTIGATION.md)
2. Review data flow diagrams
3. Check component descriptions

### I want to see what was fixed
1. Read [RESOLUTION_SUMMARY.md](./RESOLUTION_SUMMARY.md)
2. Review code changes
3. Check verification results

### I need detailed troubleshooting steps
1. Read [TROUBLESHOOTING_GUIDE.md](./TROUBLESHOOTING_GUIDE.md)
2. Run diagnosis commands
3. Apply specific fixes

---

## Success Criteria

System is healthy when:

- ✅ Pod status: 1/1 Running
- ✅ No Redis connection errors
- ✅ Transaction batches processing successfully
- ✅ No "Character N" or "Insufficient holdings" errors
- ✅ Historical data exists (150+ records)
- ✅ Risk metrics calculated
- ✅ Outbox populated (entries with SENT status)
- ✅ Events published to Kafka

---

## Contact & Version

- **Repository:** pms-org/pms-analytics
- **Branch:** main
- **Last Updated:** January 29, 2026
- **Documentation Version:** 1.0
- **System Status:** ✅ FULLY OPERATIONAL

---

**For immediate help, start with [LLM_FIX_IT_GUIDE.md](./LLM_FIX_IT_GUIDE.md)**
