WITH listing_data as (
  SELECT 
    ad.name as app_name,
    a.name as asset_name,
    a."type" as asset_type,  
    ROUND(CAST((price + fee) AS DECIMAL(20, 2)) / 100, 2) as listing_cost_dollars,
    listingid,
    a.appid,
    a.status as asset_status,
    a.descriptions[1][1] as asset_description,
    a.contextid,
    a.currency as asset_currency,
    a.commodity,
    li.ingestion_time,
    -- Calculate listing age in hours
    date_diff('hour', li.ingestion_time, current_timestamp) as listing_age_hours
  FROM lakehouse.silver.slv_steam__new_listings_listinginfo li
  LEFT JOIN lakehouse.silver.slv_steam__new_listings_assets a ON li.asset[4] = a.id
  LEFT JOIN lakehouse.silver.slv_steam__new_listings_app_data ad ON a.appid = ad.appid
  WHERE a.status = 'active'  -- Only consider active listings
), 

-- Calculate price statistics for each app/asset combination
price_stats AS (
  SELECT
    app_name,
    asset_name,
    AVG(listing_cost_dollars) as avg_price,
    STDDEV(listing_cost_dollars) as price_stddev,
    MIN(listing_cost_dollars) as min_price,
    MAX(listing_cost_dollars) as max_price,
    COUNT(*) as total_listings,
    -- Calculate median price using percentile_cont
    approx_percentile(listing_cost_dollars, 0.5) as median_price,
    -- Calculate 25th and 75th percentiles for IQR
    approx_percentile(listing_cost_dollars, 0.25) as q1_price,
    approx_percentile(listing_cost_dollars, 0.75) as q3_price
  FROM listing_data
  GROUP BY app_name, asset_name
),

-- Filter out outliers and suspicious listings
valid_listings AS (
  SELECT 
    ld.*,
    ps.avg_price,
    ps.median_price,
    ps.price_stddev,
    ps.q1_price,
    ps.q3_price,
    -- Calculate IQR (Interquartile Range)
    ps.q3_price - ps.q1_price as iqr,
    -- Calculate upper and lower bounds for outliers (1.5 * IQR)
    ps.q3_price + (1.5 * (ps.q3_price - ps.q1_price)) as upper_bound,
    ps.q1_price - (1.5 * (ps.q3_price - ps.q1_price)) as lower_bound
  FROM listing_data ld
  JOIN price_stats ps ON ld.app_name = ps.app_name AND ld.asset_name = ps.asset_name
  WHERE 
    -- Only include listings within reasonable bounds
    ld.listing_cost_dollars BETWEEN ps.lower_bound AND ps.upper_bound
    -- Exclude listings that are more than 3 standard deviations from the mean
    AND ABS(ld.listing_cost_dollars - ps.avg_price) <= (3 * ps.price_stddev)
    -- Ensure we have enough valid listings for meaningful statistics
    AND ps.total_listings >= 5
)

SELECT 
  vl.app_name,
  vl.asset_name,
  vl.asset_type,
  vl.listing_cost_dollars,
  vl.avg_price,
  vl.median_price,
  vl.min_price,
  vl.max_price,
  -- Calculate discount percentage from median (more robust than mean)
  ROUND((vl.median_price - vl.listing_cost_dollars) / vl.median_price * 100, 2) as discount_percentage,
  vl.listing_age_hours,
  vl.asset_description,
  vl.asset_currency,
  vl.commodity,
  -- Include some statistics for reference
  vl.price_stddev,
  vl.iqr
FROM valid_listings vl
WHERE 
  -- Only consider listings below median price
  vl.listing_cost_dollars < vl.median_price
  -- Ensure price is positive
  AND vl.listing_cost_dollars > 0
  -- Only consider listings from the last 24 hours
  AND vl.listing_age_hours <= 24
  -- Only consider listings that are at least 10% below median
  AND (vl.median_price - vl.listing_cost_dollars) / vl.median_price >= 0.10
ORDER BY 
  discount_percentage DESC,
  listing_age_hours ASC




