-- Populate Historical Portfolio Value Data for Testing
-- This script creates 30 days of synthetic portfolio value history
-- to enable immediate risk metrics calculation

-- Clear any existing test data (optional)
-- DELETE FROM analytics_portfolio_value_history WHERE date >= CURRENT_DATE - INTERVAL '30 days';

DO $$
DECLARE
    portfolio_rec RECORD;
    day_offset INTEGER;
    base_value NUMERIC(20, 2);
    daily_value NUMERIC(20, 2);
    daily_volatility NUMERIC(5, 4);
    trend_factor NUMERIC(5, 4);
BEGIN
    -- Loop through each portfolio in the analytics table
    FOR portfolio_rec IN 
        SELECT DISTINCT portfolio_id FROM analytics
    LOOP
        RAISE NOTICE 'Generating historical data for portfolio: %', portfolio_rec.portfolio_id;
        
        -- Calculate current portfolio value as base
        SELECT SUM(holdings * 150.00) -- Using $150 as average price for simplicity
        INTO base_value
        FROM analytics
        WHERE portfolio_id = portfolio_rec.portfolio_id;
        
        -- If no base value, use a default
        IF base_value IS NULL OR base_value = 0 THEN
            base_value := 100000.00; -- $100k default portfolio
        END IF;
        
        -- Generate 30 days of historical data (going backwards from yesterday)
        FOR day_offset IN 1..30 LOOP
            -- Add some realistic variation:
            -- - Overall upward trend (0.1% per day on average)
            -- - Daily volatility (±2%)
            -- - Random walk component
            
            daily_volatility := (RANDOM() * 0.04) - 0.02; -- ±2% daily
            trend_factor := 1.0 + (0.001 * day_offset) + daily_volatility; -- Slight upward trend
            
            daily_value := base_value * trend_factor;
            
            -- Ensure positive values
            IF daily_value < 0 THEN
                daily_value := base_value * 0.95;
            END IF;
            
            -- Insert historical record (skip if already exists)
            IF NOT EXISTS (
                SELECT 1 FROM analytics_portfolio_value_history 
                WHERE portfolio_id = portfolio_rec.portfolio_id 
                AND date = CURRENT_DATE - day_offset
            ) THEN
                INSERT INTO analytics_portfolio_value_history 
                    (id, portfolio_id, date, portfolio_value, created_at, updated_at)
                VALUES (
                    gen_random_uuid(),
                    portfolio_rec.portfolio_id,
                    CURRENT_DATE - day_offset,
                    daily_value,
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP
                );
            END IF;
            
        END LOOP;
        
        RAISE NOTICE 'Completed portfolio: % with base value: %', portfolio_rec.portfolio_id, base_value;
    END LOOP;
END $$;

-- Verify the data was created
SELECT 
    COUNT(*) as total_records,
    COUNT(DISTINCT portfolio_id) as unique_portfolios,
    MIN(date) as earliest_date,
    MAX(date) as latest_date,
    AVG(portfolio_value) as avg_portfolio_value
FROM analytics_portfolio_value_history;

-- Show sample data for verification
SELECT 
    portfolio_id,
    date,
    portfolio_value,
    LAG(portfolio_value) OVER (PARTITION BY portfolio_id ORDER BY date) as prev_value,
    ROUND(
        (portfolio_value - LAG(portfolio_value) OVER (PARTITION BY portfolio_id ORDER BY date)) / 
        NULLIF(LAG(portfolio_value) OVER (PARTITION BY portfolio_id ORDER BY date), 0) * 100,
        2
    ) as daily_return_pct
FROM analytics_portfolio_value_history
WHERE portfolio_id = (SELECT portfolio_id FROM analytics_portfolio_value_history LIMIT 1)
ORDER BY date DESC
LIMIT 10;
