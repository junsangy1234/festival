package com.junsang.festival.domain.diagnosis.policy;

import java.math.BigDecimal;

// 정책 파일에 선언한 기준값과 진단 지표를 비교하는 연산자다.
public enum ComparisonOperator {
    GREATER_THAN {
        @Override
        public boolean matches(BigDecimal value, BigDecimal threshold) {
            return value.compareTo(threshold) > 0;
        }
    },
    GREATER_THAN_OR_EQUAL {
        @Override
        public boolean matches(BigDecimal value, BigDecimal threshold) {
            return value.compareTo(threshold) >= 0;
        }
    },
    LESS_THAN {
        @Override
        public boolean matches(BigDecimal value, BigDecimal threshold) {
            return value.compareTo(threshold) < 0;
        }
    },
    LESS_THAN_OR_EQUAL {
        @Override
        public boolean matches(BigDecimal value, BigDecimal threshold) {
            return value.compareTo(threshold) <= 0;
        }
    },
    EQUAL {
        @Override
        public boolean matches(BigDecimal value, BigDecimal threshold) {
            return value.compareTo(threshold) == 0;
        }
    };

    // 하나의 지표가 정책의 임계 조건을 만족하는지 판정한다.
    public abstract boolean matches(BigDecimal value, BigDecimal threshold);
}
