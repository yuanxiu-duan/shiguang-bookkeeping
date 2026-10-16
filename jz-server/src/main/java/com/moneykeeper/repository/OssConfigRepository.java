package com.moneykeeper.repository;

import com.moneykeeper.entity.OssConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OssConfigRepository extends JpaRepository<OssConfig, Long> {
    Optional<OssConfig> findByIsActiveTrue();
}
