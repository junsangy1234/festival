UPDATE diagnoses
SET timing_fit_weight = 20,
    region_demand_weight = 20,
    connectivity_weight = 20,
    accessibility_weight = 20,
    competition_resistance_weight = 20
WHERE timing_fit_weight IS NULL
   OR region_demand_weight IS NULL
   OR connectivity_weight IS NULL
   OR accessibility_weight IS NULL
   OR competition_resistance_weight IS NULL;

ALTER TABLE diagnoses ALTER COLUMN timing_fit_weight SET NOT NULL;
ALTER TABLE diagnoses ALTER COLUMN region_demand_weight SET NOT NULL;
ALTER TABLE diagnoses ALTER COLUMN connectivity_weight SET NOT NULL;
ALTER TABLE diagnoses ALTER COLUMN accessibility_weight SET NOT NULL;
ALTER TABLE diagnoses ALTER COLUMN competition_resistance_weight SET NOT NULL;
