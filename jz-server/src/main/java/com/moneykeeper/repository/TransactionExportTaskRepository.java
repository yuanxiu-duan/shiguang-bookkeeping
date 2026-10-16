package com.moneykeeper.repository;

import com.moneykeeper.entity.TransactionExportTask;
import com.moneykeeper.entity.TransactionExportTask.ExportStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionExportTaskRepository extends JpaRepository<TransactionExportTask, Long> {
    boolean existsByStatus(ExportStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<TransactionExportTask> findFirstByStatusOrderByCreatedAtAsc(ExportStatus status);

    List<TransactionExportTask> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
