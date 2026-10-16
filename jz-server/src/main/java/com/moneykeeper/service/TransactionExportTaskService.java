package com.moneykeeper.service;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moneykeeper.dto.TransactionExportRequest;
import com.moneykeeper.entity.TransactionExportTask;
import com.moneykeeper.entity.TransactionExportTask.ExportStatus;
import com.moneykeeper.entity.User;
import com.moneykeeper.repository.TransactionExportTaskRepository;
import com.moneykeeper.repository.UserRepository;

@Service
public class TransactionExportTaskService {

    private final TransactionExportTaskRepository taskRepository;
    private final UserRepository userRepository;
    private final AccountBookService accountBookService;
    private final ExcelService excelService;
    private final EmailService emailService;

    @Value("${app.export.max-tasks-per-run:1}")
    private int maxTasksPerRun;

    @Value("${app.export.zip-threshold-bytes:10485760}")
    private long zipThresholdBytes;

    public TransactionExportTaskService(TransactionExportTaskRepository taskRepository,
                                        UserRepository userRepository,
                                        AccountBookService accountBookService,
                                        ExcelService excelService,
                                        EmailService emailService) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.accountBookService = accountBookService;
        this.excelService = excelService;
        this.emailService = emailService;
    }

    @Transactional
    public TransactionExportTask submit(User user, TransactionExportRequest request) {
        if (request == null) {
            request = new TransactionExportRequest();
        }
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new IllegalArgumentException("请先在设置中绑定邮箱，再导出账单流水");
        }

        LocalDate endDate = request.getEndDate() == null ? LocalDate.now() : request.getEndDate();
        LocalDate startDate = request.getStartDate() == null ? endDate.minusMonths(3) : request.getStartDate();
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("开始日期不能晚于结束日期");
        }
        if (startDate.isBefore(endDate.minusYears(1))) {
            throw new IllegalArgumentException("单次最多导出一年内的账单流水");
        }

        Long accountBookId = request.getAccountBookId();
        if (accountBookId == null) {
            accountBookId = accountBookService.getDefaultAccountBook(user.getId()).getId();
        }

        TransactionExportTask task = new TransactionExportTask();
        task.setUserId(user.getId());
        task.setAccountBookId(accountBookId);
        task.setEmail(user.getEmail());
        task.setStartDate(startDate);
        task.setEndDate(endDate);
        task.setStatus(ExportStatus.PENDING);
        return taskRepository.save(task);
    }

    public List<TransactionExportTask> listRecent(User user) {
        return taskRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), PageRequest.of(0, 20));
    }

    @Scheduled(fixedDelayString = "${app.export.schedule-delay-ms:60000}")
    @Transactional
    public synchronized void processPendingTasks() {
        if (taskRepository.existsByStatus(ExportStatus.PROCESSING)) {
            return;
        }

        int limit = Math.max(1, maxTasksPerRun);
        for (int i = 0; i < limit; i++) {
            TransactionExportTask task = claimNextTask();
            if (task == null) {
                return;
            }
            processTask(task);
        }
    }

    @Transactional
    public TransactionExportTask claimNextTask() {
        return taskRepository.findFirstByStatusOrderByCreatedAtAsc(ExportStatus.PENDING)
                .map(task -> {
                    task.setStatus(ExportStatus.PROCESSING);
                    task.setStartedAt(LocalDateTime.now());
                    task.setErrorMessage(null);
                    return taskRepository.save(task);
                })
                .orElse(null);
    }

    private void processTask(TransactionExportTask task) {
        try {
            User user = userRepository.findById(task.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
            Path exportFile = excelService.generateTransactionExportFile(
                    task.getUserId(),
                    task.getAccountBookId(),
                    task.getStartDate(),
                    task.getEndDate()
            );
            Path mailFile = excelService.zipIfLarge(exportFile, zipThresholdBytes);
            emailService.sendTransactionExport(user.getEmail(), user.getNickname(), mailFile);
            markSuccess(task.getId(), mailFile);
        } catch (Exception e) {
            markFailed(task.getId(), e.getMessage());
        }
    }

    @Transactional
    public void markSuccess(Long taskId, Path file) {
        TransactionExportTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("导出任务不存在"));
        task.setStatus(ExportStatus.SUCCESS);
        task.setFilePath(file.toString());
        task.setFileName(file.getFileName().toString());
        task.setCompletedAt(LocalDateTime.now());
        task.setEmailedAt(LocalDateTime.now());
        taskRepository.save(task);
    }

    @Transactional
    public void markFailed(Long taskId, String errorMessage) {
        TransactionExportTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("导出任务不存在"));
        task.setStatus(ExportStatus.FAILED);
        task.setErrorMessage(errorMessage == null ? "导出失败" : errorMessage);
        task.setCompletedAt(LocalDateTime.now());
        taskRepository.save(task);
    }
}
