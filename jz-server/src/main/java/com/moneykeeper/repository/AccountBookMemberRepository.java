package com.moneykeeper.repository;

import com.moneykeeper.entity.AccountBookMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountBookMemberRepository extends JpaRepository<AccountBookMember, Long> {

    List<AccountBookMember> findByUserId(Long userId);

    Optional<AccountBookMember> findByAccountBookIdAndUserId(Long accountBookId, Long userId);

    List<AccountBookMember> findByAccountBookIdAndStatus(Long accountBookId, AccountBookMember.MemberStatus status);

    boolean existsByAccountBookIdAndUserIdAndStatus(Long accountBookId, Long userId, AccountBookMember.MemberStatus status);

    void deleteByAccountBookIdAndUserId(Long accountBookId, Long userId);
}
