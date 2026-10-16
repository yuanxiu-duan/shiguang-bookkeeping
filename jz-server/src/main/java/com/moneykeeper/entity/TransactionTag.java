package com.moneykeeper.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "transaction_tags")
public class TransactionTag {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false)
    private Long transactionId;

    @Column(name = "tag_id", nullable = false)
    private Long tagId;

    public TransactionTag(Long transactionId, Long tagId) {
        this.transactionId = transactionId;
        this.tagId = tagId;
    }
}
