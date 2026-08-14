package com.junsang.festival.domain.diagnosis.policy;

import com.junsang.festival.domain.diagnosis.calculation.DiagnosisMetric;
import org.springframework.stereotype.Component;

import java.util.Map;

// 정책 문구의 {key}를 계산 결과의 값 또는 속성으로 치환한다.
@Component
public class PolicyTemplateResolver {

    // 템플릿에 지표 값과 부가 정보를 적용한다.
    public String resolve(String template, DiagnosisMetric metric) {
        if (template == null) {
            return null;
        }

        String resolved = template.replace("{value}", metric.value().stripTrailingZeros().toPlainString());
        for (Map.Entry<String, String> entry : metric.attributes().entrySet()) {
            resolved = resolved.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return resolved;
    }
}
