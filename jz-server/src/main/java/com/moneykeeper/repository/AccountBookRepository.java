package com.moneykeeper.repository;

import com.moneykeeper.entity.AccountBook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountBookRepository extends JpaRepository<AccountBook, Long> {

    List<AccountBook> findByUserIdOrderByIdAsc(Long userId);

    Optional<AccountBook> findByUserIdAndName(Long userId, String name);

    boolean existsByUserIdAndName(Long userId, String name);

    Optional<AccountBook> findByUserIdAndIsDefaultTrue(Long userId);
}
