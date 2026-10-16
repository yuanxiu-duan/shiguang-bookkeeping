package com.moneykeeper.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "account_book_members", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"account_book_id", "user_id"})
})
public class AccountBookMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_book_id", nullable = false)
    private Long accountBookId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private MemberRole role = MemberRole.MEMBER;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private MemberStatus status = MemberStatus.PENDING;

    @Column(name = "invited_by")
    private Long invitedBy;

    @Column(length = 50)
    private String alias;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    @Transient
    private String username;

    @Transient
    private String inviterUsername;

    @Transient
    private String accountBookName;

    public enum MemberRole {
        OWNER, MEMBER
    }

    public enum MemberStatus {
        PENDING, ACCEPTED, REJECTED
    }
}
