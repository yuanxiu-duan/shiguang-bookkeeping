package com.moneykeeper.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.moneykeeper.dto.TransactionExcelDTO;
import com.moneykeeper.dto.TransactionRequest;
import com.moneykeeper.entity.Category;
import com.moneykeeper.entity.Category.CategoryType;
import com.moneykeeper.entity.Transaction;
import com.moneykeeper.entity.User;
import com.moneykeeper.repository.CategoryRepository;
import com.moneykeeper.repository.TransactionRepository;
import com.moneykeeper.repository.UserRepository;

@Service
public class ExcelService {

    private final TransactionService transactionService;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final AccountBookService accountBookService;
    private final UserRepository userRepository;

    @Value("${app.export.output-dir:exports}")
    private String exportOutputDir;

    public ExcelService(TransactionService transactionService,
                       CategoryRepository categoryRepository,
                       TransactionRepository transactionRepository,
                       AccountBookService accountBookService,
                       UserRepository userRepository) {
        this.transactionService = transactionService;
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.accountBookService = accountBookService;
        this.userRepository = userRepository;
    }

    public Map<String, Object> importTransactions(Long userId, Long accountBookId, MultipartFile file) throws IOException {
        if (accountBookId == null) {
            accountBookId = accountBookService.getDefaultAccountBook(userId).getId();
        }

        List<String> errors = new ArrayList<>();
        List<TransactionExcelDTO> successList = new ArrayList<>();
        Set<Integer> errorRows = new HashSet<>();

        Long finalAccountBookId = accountBookId;
        List<Category> allCategories = categoryRepository.findByAccountBookIdOrUserIdIsNullOrderBySortOrderAsc(accountBookId);
        Map<String, Category> expenseCategoryMap = allCategories.stream()
                .filter(c -> c.getType() == CategoryType.EXPENSE)
                .collect(Collectors.toMap(Category::getName, c -> c, (existing, replacement) -> existing));
        Map<String, Category> incomeCategoryMap = allCategories.stream()
                .filter(c -> c.getType() == CategoryType.INCOME)
                .collect(Collectors.toMap(Category::getName, c -> c, (existing, replacement) -> existing));

        // 创建人(username) -> userId 缓存，避免每行重复查询
        Map<String, Long> creatorCache = new HashMap<>();

        EasyExcel.read(file.getInputStream(), TransactionExcelDTO.class, new ReadListener<TransactionExcelDTO>() {
            private int rowNum = 1;

            @Override
            public void invoke(TransactionExcelDTO dto, AnalysisContext context) {
                rowNum++;

                try {
                    String error = validateRow(dto, rowNum, expenseCategoryMap, incomeCategoryMap);
                    if (error != null) {
                        errors.add(error);
                        errorRows.add(rowNum);
                        return;
                    }

                    CategoryType type = "支出".equals(dto.getType()) ? CategoryType.EXPENSE : CategoryType.INCOME;
                    Map<String, Category> categoryMap = type == CategoryType.EXPENSE ? expenseCategoryMap : incomeCategoryMap;
                    Category category = categoryMap.get(dto.getCategoryName());

                    // 解析"创建人"列：按 username 查找对应用户 ID
                    // 为空或找不到用户时，回退到当前登录用户
                    Long creatorUserId = resolveCreatorUserId(dto.getCreatedBy(), userId, creatorCache);

                    TransactionRequest request = new TransactionRequest();
                    request.setAccountBookId(finalAccountBookId);
                    request.setType(type);
                    request.setCategoryId(category.getId());
                    request.setAmount(dto.getAmount());
                    request.setTransactionDate(parseDate(dto.getTransactionDate()));
                    request.setNote(dto.getNote());
                    request.setAccount(dto.getAccount());

                    transactionService.createTransaction(creatorUserId, request);
                    successList.add(dto);

                } catch (Exception e) {
                    errors.add("第" + rowNum + "行: " + e.getMessage());
                    errorRows.add(rowNum);
                }
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {
            }
        }).sheet().doRead();

        Map<String, Object> result = new HashMap<>();
        result.put("successCount", successList.size());
        result.put("failCount", errors.size());
        result.put("errors", errors);
        result.put("totalCount", successList.size() + errors.size());

        return result;
    }

    public void exportTransactions(Long userId, Long accountBookId, LocalDate startDate, LocalDate endDate, HttpServletResponse response) throws IOException {
        List<TransactionExcelDTO> exportData = buildExportData(userId, accountBookId, startDate, endDate);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("交易记录_" + LocalDate.now(), StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        EasyExcel.write(response.getOutputStream(), TransactionExcelDTO.class)
                .sheet("交易记录")
                .doWrite(exportData);
    }

    public Path generateTransactionExportFile(Long userId, Long accountBookId, LocalDate startDate, LocalDate endDate) throws IOException {
        List<TransactionExcelDTO> exportData = buildExportData(userId, accountBookId, startDate, endDate);
        Path outputDir = Path.of(exportOutputDir).toAbsolutePath().normalize();
        Files.createDirectories(outputDir);
        String fileName = "交易记录_" + userId + "_" + LocalDate.now() + "_" + System.currentTimeMillis() + ".xlsx";
        Path file = outputDir.resolve(fileName);
        EasyExcel.write(file.toFile(), TransactionExcelDTO.class)
                .sheet("交易记录")
                .doWrite(exportData);
        return file;
    }

    public Path zipIfLarge(Path file, long thresholdBytes) throws IOException {
        if (Files.size(file) <= thresholdBytes) {
            return file;
        }

        Path zipFile = file.resolveSibling(file.getFileName().toString().replaceFirst("\\.xlsx$", ".zip"));
        try (ZipOutputStream zipOut = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            zipOut.putNextEntry(new ZipEntry(file.getFileName().toString()));
            Files.copy(file, zipOut);
            zipOut.closeEntry();
        }
        return zipFile;
    }

    private List<TransactionExcelDTO> buildExportData(Long userId, Long accountBookId, LocalDate startDate, LocalDate endDate) {
        if (accountBookId == null) {
            accountBookId = accountBookService.getDefaultAccountBook(userId).getId();
        }

        if (startDate == null) {
            startDate = LocalDate.now().minusMonths(3);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        List<Transaction> transactions = transactionRepository
                .findByAccountBookIdAndTransactionDateBetweenOrderByTransactionDateDesc(accountBookId, startDate, endDate);

        List<Long> categoryIds = transactions.stream()
                .map(Transaction::getCategoryId)
                .distinct()
                .toList();

        Map<Long, Category> categoryMap = categoryRepository.findAllById(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, c -> c));

        List<Long> userIds = transactions.stream()
                .map(Transaction::getUserId)
                .distinct()
                .toList();

        Map<Long, String> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId,
                        u -> u.getNickname() != null && !u.getNickname().isBlank() ? u.getNickname() : u.getUsername()));

        return transactions.stream()
                .map(t -> {
                    TransactionExcelDTO dto = new TransactionExcelDTO();
                    dto.setTransactionDate(t.getTransactionDate().toString());
                    dto.setType(t.getType() == CategoryType.EXPENSE ? "支出" : "收入");
                    dto.setAmount(t.getAmount());
                    dto.setCategoryName(categoryMap.getOrDefault(t.getCategoryId(), new Category()).getName());
                    dto.setNote(t.getNote());
                    dto.setCreatedBy(userMap.getOrDefault(t.getUserId(), ""));
                    dto.setAccount(t.getAccount());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    public void downloadTemplate(HttpServletResponse response) throws IOException {
        List<TransactionExcelDTO> templateData = new ArrayList<>();

        TransactionExcelDTO example1 = new TransactionExcelDTO();
        example1.setTransactionDate("2025-05-01");
        example1.setType("支出");
        example1.setAmount(new BigDecimal("50.00"));
        example1.setCategoryName("吃喝");
        example1.setNote("午餐");
        example1.setCreatedBy("");
        example1.setAccount("微信钱包");

        TransactionExcelDTO example2 = new TransactionExcelDTO();
        example2.setTransactionDate("2025-05-02");
        example2.setType("收入");
        example2.setAmount(new BigDecimal("10000.00"));
        example2.setCategoryName("工资");
        example2.setNote("5月工资");
        example2.setCreatedBy("");
        example2.setAccount("银行卡");

        templateData.add(example1);
        templateData.add(example2);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("交易记录导入模板", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        EasyExcel.write(response.getOutputStream(), TransactionExcelDTO.class)
                .sheet("模板")
                .doWrite(templateData);
    }

    private String validateRow(TransactionExcelDTO dto, int rowNum,
                               Map<String, Category> expenseCategoryMap,
                               Map<String, Category> incomeCategoryMap) {
        if (dto.getTransactionDate() == null || dto.getTransactionDate().trim().isEmpty()) {
            return "第" + rowNum + "行: 交易日期不能为空";
        }

        if (dto.getType() == null || dto.getType().trim().isEmpty()) {
            return "第" + rowNum + "行: 类型不能为空（只能是'支出'或'收入'）";
        }

        if (!"支出".equals(dto.getType()) && !"收入".equals(dto.getType())) {
            return "第" + rowNum + "行: 类型只能是'支出'或'收入'";
        }

        if (dto.getAmount() == null) {
            return "第" + rowNum + "行: 金额不能为空";
        }

        if (dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return "第" + rowNum + "行: 金额必须大于0";
        }

        if (dto.getCategoryName() == null || dto.getCategoryName().trim().isEmpty()) {
            return "第" + rowNum + "行: 分类名称不能为空";
        }

        CategoryType type = "支出".equals(dto.getType()) ? CategoryType.EXPENSE : CategoryType.INCOME;
        Map<String, Category> categoryMap = type == CategoryType.EXPENSE ? expenseCategoryMap : incomeCategoryMap;

        if (!categoryMap.containsKey(dto.getCategoryName())) {
            return "第" + rowNum + "行: 分类'" + dto.getCategoryName() + "'不存在，请先在系统中添加该分类";
        }

        try {
            parseDate(dto.getTransactionDate());
        } catch (Exception e) {
            return "第" + rowNum + "行: 日期格式错误，请使用 yyyy-MM-dd 格式（如：2025-05-01）";
        }

        return null;
    }

    /**
     * 解析"创建人"列：按 username 查找对应用户 ID。
     * - 创建人为空 → 回退到当前登录用户
     * - 找不到用户 → 回退到当前登录用户
     * - 找到用户但不是账本成员 → 由 createTransaction 内部 checkAccess 抛出异常，该行失败
     */
    private Long resolveCreatorUserId(String createdBy, Long currentUserId, Map<String, Long> cache) {
        if (createdBy == null || createdBy.trim().isEmpty()) {
            return currentUserId;
        }
        String username = createdBy.trim();
        Long cached = cache.get(username);
        if (cached != null) {
            return cached;
        }
        Long resolved = userRepository.findByUsername(username)
                .map(User::getId)
                .orElse(currentUserId);
        cache.put(username, resolved);
        return resolved;
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null) return null;
        dateStr = dateStr.trim();

        DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        DateTimeFormatter formatter3 = DateTimeFormatter.ofPattern("yyyyMMdd");

        try {
            return LocalDate.parse(dateStr, formatter1);
        } catch (Exception e) {
            try {
                return LocalDate.parse(dateStr, formatter2);
            } catch (Exception e2) {
                return LocalDate.parse(dateStr.replaceAll("/", "-"), formatter3);
            }
        }
    }
}
