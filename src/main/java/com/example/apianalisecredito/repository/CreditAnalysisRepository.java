package com.example.apianalisecredito.repository;

import com.example.apianalisecredito.repository.entity.CreditAnalysisEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreditAnalysisRepository extends JpaRepository<CreditAnalysisEntity, UUID> {
}
