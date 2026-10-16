package com.moneykeeper.repository;

import com.moneykeeper.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    List<Tag> findByUserIdOrderByNameAsc(Long userId);

    Optional<Tag> findByUserIdAndName(Long userId, String name);

    List<Tag> findByUserIdAndNameContainingIgnoreCase(Long userId, String keyword);

    List<Tag> findByAccountBookIdOrderByNameAsc(Long accountBookId);

    Optional<Tag> findByAccountBookIdAndName(Long accountBookId, String name);

    List<Tag> findByAccountBookIdAndNameContainingIgnoreCase(Long accountBookId, String keyword);
}
