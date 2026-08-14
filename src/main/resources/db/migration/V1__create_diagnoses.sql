CREATE TABLE diagnoses (
    id VARCHAR(36) PRIMARY KEY,
    festival_name VARCHAR(100) NOT NULL,
    region_code VARCHAR(30) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    festival_type VARCHAR(30) NOT NULL,
    scale VARCHAR(20) NOT NULL,
    recurrence_type VARCHAR(20) NOT NULL,
    existing_festival_content_id VARCHAR(50),
    timing_fit_weight INTEGER,
    region_demand_weight INTEGER,
    connectivity_weight INTEGER,
    accessibility_weight INTEGER,
    competition_resistance_weight INTEGER,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
