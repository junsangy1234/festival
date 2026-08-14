package com.junsang.festival.domain.diagnosis.policy;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

// YAML 정책 파일을 Spring Bean으로 등록한다.
@Configuration
@EnableConfigurationProperties({RiskPolicyProperties.class, RecommendationPolicyProperties.class})
public class DiagnosisPolicyConfig {
}
