package com.moneykeeper.repository;

import com.moneykeeper.entity.TransactionTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionTagRepository extends JpaRepository<TransactionTag, Long> {
    
    List<TransactionTag> findByTransactionId(Long transactionId);
    
    List<TransactionTag> findByTransactionIdIn(List<Long> transactionIds);
    
    List<TransactionTag> findByTagId(Long tagId);
    
    @Modifying
    @Query("DELETE FROM TransactionTag tt WHERE tt.transactionId = :transactionId")
    void deleteByTransactionId(@Param("transactionId") Long transactionId);
    
    @Query("SELECT tt.transactionId FROM TransactionTag tt WHERE tt.tagId = :tagId")
    List<Long> findTransactionIdsByTagId(@Param("tagId") Long tagId);
    
    @Query("SELECT tt.tagId FROM TransactionTag tt WHERE tt.transactionId IN :transactionIds")
    List<Long> findTagIdsByTransactionIds(@Param("transactionIds") List<Long> transactionIds);
}
