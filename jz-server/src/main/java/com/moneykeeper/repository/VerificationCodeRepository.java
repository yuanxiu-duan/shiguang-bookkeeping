package com.moneykeeper.repository;

import com.moneykeeper.entity.VerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Long> {

    Optional<VerificationCode> findByEmailAndCodeAndTypeAndUsedFalseAndExpiredAtAfter(
            String email, String code, String type, LocalDateTime now);

    long countByEmailIgnoreCaseAndCreatedAtAfter(String email, LocalDateTime createdAt);

    @Modifying
    @Query("update VerificationCode v set v.used = true where lower(v.email) = lower(:email) and v.type = :type and v.used = false")
    void markUnusedCodesUsed(@Param("email") String email, @Param("type") String type);

    @Modifying
    @Query("update VerificationCode v set v.used = true where v.id = :id")
    void markCodeUsed(@Param("id") Long id);
}