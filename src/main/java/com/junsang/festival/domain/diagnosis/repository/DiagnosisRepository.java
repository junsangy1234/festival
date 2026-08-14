package com.junsang.festival.domain.diagnosis.repository;

import com.junsang.festival.domain.diagnosis.entity.Diagnosis;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiagnosisRepository extends JpaRepository<Diagnosis, String> {
}
