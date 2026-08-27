CREATE TABLE festival_histories (
    id VARCHAR(36) PRIMARY KEY,
    festival_name VARCHAR(255) NOT NULL,
    region_name VARCHAR(100),
    signgu_name VARCHAR(100),
    last_year_visitors DECIMAL(15, 2),
    budget_million_won DECIMAL(15, 2),
    first_held_year INTEGER,
    round_count INTEGER
);

CREATE INDEX idx_festival_histories_name ON festival_histories (festival_name);
