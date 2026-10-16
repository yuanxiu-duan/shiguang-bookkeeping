package com.moneykeeper.service;

import com.moneykeeper.dto.TransactionRequest;
import com.moneykeeper.entity.Category;
import com.moneykeeper.entity.Category.CategoryType;
import com.moneykeeper.entity.Tag;
import com.moneykeeper.entity.Transaction;
import com.moneykeeper.entity.TransactionTag;
import com.moneykeeper.entity.User;
import com.moneykeeper.repository.CategoryRepository;
import com.moneykeeper.repository.TagRepository;
import com.moneykeeper.repository.TransactionRepository;
import com.moneykeeper.repository.TransactionTagRepository;
import com.moneykeeper.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final TransactionTagRepository transactionTagRepository;
    private final AccountBookService accountBookService;
    private final AccountBookMemberService memberService;
    private final UserRepository userRepository;

    public TransactionService(TransactionRepository transactionRepository,
                              CategoryRepository categoryRepository,
                              TagRepository tagRepository,
                              TransactionTagRepository transactionTagRepository,
                              AccountBookService accountBookService,
                              AccountBookMemberService memberService,
                              UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.transactionTagRepository = transactionTagRepository;
        this.accountBookService = accountBookService;
        this.memberService = memberService;
        this.userRepository = userRepository;
    }

    public Page<Transaction> getTransactions(Long userId, LocalDate startDate, LocalDate endDate,
                                              Long tagId, Long accountBookId, Pageable pageable) {
        if (startDate == null) {
            startDate = LocalDate.now().minusMonths(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        if (accountBookId == null) {
            accountBookId = accountBookService.getDefaultAccountBook(userId).getId();
        }

        checkAccess(userId, accountBookId);

        Page<Transaction> transactions;

        if (tagId != null) {
            List<Long> transactionIds = transactionTagRepository.findTransactionIdsByTagId(tagId);
            if (transactionIds.isEmpty()) {
                return new PageImpl<>(List.of(), pageable, 0);
            }

            List<Transaction> allTransactions = transactionRepository
                    .findByAccountBookIdAndIdInAndTransactionDateBetweenOrderByTransactionDateDescCreatedAtDesc(
                            accountBookId, transactionIds, startDate, endDate);

            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), allTransactions.size());
            List<Transaction> pageContent = start > allTransactions.size() ? List.of() : allTransactions.subList(start, end);

            transactions = new PageImpl<>(pageContent, pageable, allTransactions.size());
        } else {
            transactions = transactionRepository
                    .findByAccountBookIdAndTransactionDateBetweenOrderByTransactionDateDescCreatedAtDesc(
                            accountBookId, startDate, endDate, pageable);
        }

        enrichWithCategories(transactions.getContent());
        enrichWithTags(transactions.getContent());
        return transactions;
    }

    public Page<Transaction> getTransactions(Long userId, LocalDate startDate, LocalDate endDate,
                                              Long tagId, Long accountBookId, CategoryType type, Pageable pageable) {
        if (startDate == null) {
            startDate = LocalDate.now().minusMonths(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        if (accountBookId == null) {
            accountBookId = accountBookService.getDefaultAccountBook(userId).getId();
        }

        checkAccess(userId, accountBookId);

        Page<Transaction> transactions;

        if (tagId != null) {
            List<Long> transactionIds = transactionTagRepository.findTransactionIdsByTagId(tagId);
            if (transactionIds.isEmpty()) {
                return new PageImpl<>(List.of(), pageable, 0);
            }

            List<Transaction> allTransactions = transactionRepository
                    .findByAccountBookIdAndIdInAndTransactionDateBetweenOrderByTransactionDateDescCreatedAtDesc(
                            accountBookId, transactionIds, startDate, endDate);

            if (type != null) {
                allTransactions = allTransactions.stream()
                        .filter(t -> t.getType() == type)
                        .toList();
            }

            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), allTransactions.size());
            List<Transaction> pageContent = start > allTransactions.size() ? List.of() : allTransactions.subList(start, end);

            transactions = new PageImpl<>(pageContent, pageable, allTransactions.size());
        } else if (type != null) {
            transactions = transactionRepository
                    .findByAccountBookIdAndTypeAndTransactionDateBetweenOrderByTransactionDateDescCreatedAtDesc(
                            accountBookId, type, startDate, endDate, pageable);
        } else {
            transactions = transactionRepository
                    .findByAccountBookIdAndTransactionDateBetweenOrderByTransactionDateDescCreatedAtDesc(
                            accountBookId, startDate, endDate, pageable);
        }

        enrichWithCategories(transactions.getContent());
        enrichWithTags(transactions.getContent());
        return transactions;
    }

    public Page<Transaction> searchTransactions(Long userId, Long accountBookId,
                                                LocalDate startDate, LocalDate endDate,
                                                CategoryType type, List<Long> categoryIds,
                                                String account, BigDecimal minAmount, BigDecimal maxAmount,
                                                String keyword, Pageable pageable) {
        final Long resolvedBookId = (accountBookId != null)
                ? accountBookId
                : accountBookService.getDefaultAccountBook(userId).getId();

        checkAccess(userId, resolvedBookId);

        Specification<Transaction> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("accountBookId"), resolvedBookId));

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("transactionDate"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("transactionDate"), endDate));
            }
            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (categoryIds != null && !categoryIds.isEmpty()) {
                predicates.add(root.get("categoryId").in(categoryIds));
            }
            if (account != null && !account.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("account"), account));
            }
            if (minAmount != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("amount"), minAmount));
            }
            if (maxAmount != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("amount"), maxAmount));
            }
            if (keyword != null && !keyword.trim().isEmpty()) {
                String kw = "%" + keyword.trim() + "%";
                jakarta.persistence.criteria.Subquery<Long> sub = query.subquery(Long.class);
                jakarta.persistence.criteria.Root<Category> subCat = sub.from(Category.class);
                sub.select(subCat.get("id"));
                sub.where(cb.equal(subCat.get("accountBookId"), resolvedBookId),
                        cb.like(subCat.get("name"), kw));
                predicates.add(cb.or(
                        cb.like(root.get("note"), kw),
                        root.get("categoryId").in(sub)
                ));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<Transaction> page = transactionRepository.findAll(spec, pageable);

        enrichWithCategories(page.getContent());
        enrichWithTags(page.getContent());
        return page;
    }

    public Page<Transaction> getAllTransactionsPaged(Long userId, Long accountBookId,
                                                      CategoryType type, Pageable pageable) {
        if (accountBookId == null) {
            accountBookId = accountBookService.getDefaultAccountBook(userId).getId();
        }

        checkAccess(userId, accountBookId);

        Page<Transaction> page;

        if (type != null) {
            page = transactionRepository.findByAccountBookIdAndTypeOrderByTransactionDateDescCreatedAtDesc(
                    accountBookId, type, pageable);
        } else {
            page = transactionRepository.findByAccountBookIdOrderByTransactionDateDescCreatedAtDesc(
                    accountBookId, pageable);
        }

        enrichWithCategories(page.getContent());
        enrichWithTags(page.getContent());
        return page;
    }

    public List<Transaction> getRecentTransactions(Long userId, int days, Long accountBookId) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);

        if (accountBookId == null) {
            accountBookId = accountBookService.getDefaultAccountBook(userId).getId();
        }

        checkAccess(userId, accountBookId);

        List<Transaction> transactions = transactionRepository
                .findByAccountBookIdAndTransactionDateBetweenOrderByTransactionDateDesc(accountBookId, startDate, endDate);

        enrichWithCategories(transactions);
        enrichWithTags(transactions);
        return transactions;
    }

    @Transactional
    public Transaction createTransaction(Long userId, TransactionRequest request) {
        Long accountBookId = request.getAccountBookId();
        if (accountBookId == null) {
            accountBookId = accountBookService.getDefaultAccountBook(userId).getId();
        }

        checkAccess(userId, accountBookId);

        validateCategory(accountBookId, request.getCategoryId());
        List<Long> validTagIds = validateAndFilterTags(accountBookId, request.getTagIds());

        Transaction transaction = new Transaction();
        transaction.setUserId(userId);
        transaction.setAccountBookId(accountBookId);
        transaction.setAmount(request.getAmount());
        transaction.setType(request.getType());
        transaction.setCategoryId(request.getCategoryId());
        transaction.setNote(request.getNote());
        transaction.setAccount(request.getAccount());
        transaction.setTransactionDate(request.getTransactionDate());

        Transaction saved = transactionRepository.save(transaction);

        if (validTagIds != null && !validTagIds.isEmpty()) {
            saveTransactionTags(saved.getId(), validTagIds);
        }

        enrichWithCategory(saved);
        enrichWithTags(List.of(saved));
        enrichWithCreatedBy(saved);
        return saved;
    }

    @Transactional
    public Transaction updateTransaction(Long userId, Long accountBookId, Long id, TransactionRequest request) {
        if (accountBookId == null) {
            accountBookId = accountBookService.getDefaultAccountBook(userId).getId();
        }
        
        checkAccess(userId, accountBookId);
        
        Transaction existing = transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("交易记录不存在"));

        if (!existing.getAccountBookId().equals(accountBookId)) {
            throw new IllegalArgumentException("无权限修改此记录");
        }

        // 权限校验：只有记录创建者或账本所有者可以修改
        if (!existing.getUserId().equals(userId) && !memberService.isOwner(userId, accountBookId)) {
            throw new IllegalArgumentException("无权限修改此记录，只有记录创建者或账本所有者可以修改");
        }

        Long finalAccountBookId = existing.getAccountBookId();
        if (request.getAccountBookId() != null) {
            finalAccountBookId = request.getAccountBookId();
        }

        validateCategory(finalAccountBookId, request.getCategoryId());
        List<Long> validTagIds = validateAndFilterTags(finalAccountBookId, request.getTagIds());

        // 保留原始创建人，不覆盖 userId
        existing.setAccountBookId(finalAccountBookId);
        existing.setAmount(request.getAmount());
        existing.setType(request.getType());
        existing.setCategoryId(request.getCategoryId());
        existing.setNote(request.getNote());
        existing.setAccount(request.getAccount());
        existing.setTransactionDate(request.getTransactionDate());

        Transaction saved = transactionRepository.save(existing);

        transactionTagRepository.deleteByTransactionId(saved.getId());
        if (validTagIds != null && !validTagIds.isEmpty()) {
            saveTransactionTags(saved.getId(), validTagIds);
        }

        enrichWithCategory(saved);
        enrichWithTags(List.of(saved));
        enrichWithCreatedBy(saved);
        return saved;
    }

    @Transactional
    public void deleteTransaction(Long userId, Long accountBookId, Long id) {
        if (accountBookId == null) {
            accountBookId = accountBookService.getDefaultAccountBook(userId).getId();
        }
        
        checkAccess(userId, accountBookId);
        
        Transaction existing = transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("交易记录不存在"));

        if (!existing.getAccountBookId().equals(accountBookId)) {
            throw new IllegalArgumentException("无权删除此记录");
        }

        // 权限校验：只有记录创建者或账本所有者可以删除
        if (!existing.getUserId().equals(userId) && !memberService.isOwner(userId, accountBookId)) {
            throw new IllegalArgumentException("无权删除此记录，只有记录创建者或账本所有者可以删除");
        }

        transactionTagRepository.deleteByTransactionId(id);
        transactionRepository.delete(existing);
    }

    public BigDecimal getMonthlyTotal(Long userId, Long accountBookId, CategoryType type, LocalDate startDate, LocalDate endDate) {
        if (accountBookId == null) {
            return transactionRepository.sumByUserIdAndTypeAndTransactionDateBetween(userId, type, startDate, endDate);
        }
        checkAccess(userId, accountBookId);
        return transactionRepository.sumByAccountBookIdAndTypeAndTransactionDateBetween(accountBookId, type, startDate, endDate);
    }

    public BigDecimal getMonthlyTotal(Long userId, CategoryType type, LocalDate startDate, LocalDate endDate) {
        return transactionRepository.sumByUserIdAndTypeAndTransactionDateBetween(userId, type, startDate, endDate);
    }

    public List<Object[]> getMonthlyTotalByCategory(Long userId, Long accountBookId, CategoryType type, LocalDate startDate, LocalDate endDate) {
        if (accountBookId == null) {
            return transactionRepository.sumByUserIdAndTypeAndTransactionDateBetweenGroupByCategory(userId, type, startDate, endDate);
        }
        checkAccess(userId, accountBookId);
        return transactionRepository.sumByAccountBookIdAndTypeAndTransactionDateBetweenGroupByCategory(accountBookId, type, startDate, endDate);
    }

    public List<Object[]> getMonthlyTotalByCategory(Long userId, CategoryType type, LocalDate startDate, LocalDate endDate) {
        return transactionRepository.sumByUserIdAndTypeAndTransactionDateBetweenGroupByCategory(userId, type, startDate, endDate);
    }

    public List<Object[]> getDailyTrend(Long userId, Long accountBookId, LocalDate startDate, LocalDate endDate) {
        if (accountBookId == null) {
            return transactionRepository.sumByUserIdAndTransactionDateBetweenGroupByDateAndType(userId, startDate, endDate);
        }
        checkAccess(userId, accountBookId);
        return transactionRepository.sumByAccountBookIdAndTransactionDateBetweenGroupByDateAndType(accountBookId, startDate, endDate);
    }

    public List<Object[]> getDailyTrend(Long userId, LocalDate startDate, LocalDate endDate) {
        return transactionRepository.sumByUserIdAndTransactionDateBetweenGroupByDateAndType(userId, startDate, endDate);
    }

    public List<Object[]> getMonthlyTrendByAccountBookId(Long userId, Long accountBookId, LocalDate startDate) {
        if (accountBookId == null) {
            return transactionRepository.getMonthlyTrend(userId, startDate);
        }
        checkAccess(userId, accountBookId);
        return transactionRepository.getMonthlyTrendByBookId(accountBookId, startDate);
    }

    public List<Object[]> getCategoryMonthlyTrendByAccountBookId(Long userId, Long accountBookId, Long categoryId, LocalDate startDate) {
        if (accountBookId == null) {
            return transactionRepository.getCategoryMonthlyTrend(userId, categoryId, startDate);
        }
        checkAccess(userId, accountBookId);
        return transactionRepository.getCategoryMonthlyTrendByBookId(accountBookId, categoryId, startDate);
    }

    private void saveTransactionTags(Long transactionId, List<Long> tagIds) {
        List<TransactionTag> tags = tagIds.stream()
                .distinct()
                .map(tagId -> new TransactionTag(transactionId, tagId))
                .toList();
        transactionTagRepository.saveAll(tags);
    }

    private void validateCategory(Long accountBookId, Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("分类不存在"));

        if (category.getAccountBookId() != null && !category.getAccountBookId().equals(accountBookId)) {
            throw new IllegalArgumentException("无效的分类");
        }
    }

    private List<Long> validateAndFilterTags(Long accountBookId, List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return null;
        }

        List<Tag> bookTags = tagRepository.findByAccountBookIdOrderByNameAsc(accountBookId);
        Set<Long> bookTagIds = bookTags.stream()
                .map(Tag::getId)
                .collect(Collectors.toSet());

        return tagIds.stream()
                .filter(bookTagIds::contains)
                .distinct()
                .toList();
    }

    private void enrichWithCategories(List<Transaction> transactions) {
        if (transactions.isEmpty()) return;

        List<Long> categoryIds = transactions.stream()
                .map(Transaction::getCategoryId)
                .distinct()
                .toList();

        Map<Long, Category> categoryMap = categoryRepository.findAllById(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, Function.identity()));

        transactions.forEach(t -> t.setCategory(categoryMap.get(t.getCategoryId())));
        enrichWithCreatedBy(transactions);
    }

    private void enrichWithCategory(Transaction transaction) {
        categoryRepository.findById(transaction.getCategoryId()).ifPresent(transaction::setCategory);
    }

    private void enrichWithTags(List<Transaction> transactions) {
        if (transactions.isEmpty()) return;

        List<Long> transactionIds = transactions.stream()
                .map(Transaction::getId)
                .toList();

        List<TransactionTag> allTransactionTags = transactionTagRepository.findByTransactionIdIn(transactionIds);

        Map<Long, List<Long>> transactionTagIdsMap = new HashMap<>();
        for (TransactionTag tt : allTransactionTags) {
            transactionTagIdsMap.computeIfAbsent(tt.getTransactionId(), k -> new ArrayList<>())
                    .add(tt.getTagId());
        }

        List<Long> allTagIds = allTransactionTags.stream()
                .map(TransactionTag::getTagId)
                .distinct()
                .toList();

        Map<Long, Tag> tagMap = tagRepository.findAllById(allTagIds).stream()
                .collect(Collectors.toMap(Tag::getId, Function.identity()));

        for (Transaction t : transactions) {
            List<Long> tagIds = transactionTagIdsMap.getOrDefault(t.getId(), List.of());
            List<Tag> tags = tagIds.stream()
                    .map(tagMap::get)
                    .filter(Objects::nonNull)
                    .toList();
            t.setTags(tags);
        }
    }

    private void checkAccess(Long userId, Long accountBookId) {
        if (!memberService.hasAccess(userId, accountBookId)) {
            throw new IllegalArgumentException("您没有访问此账本的权限");
        }
    }

    private void enrichWithCreatedBy(List<Transaction> transactions) {
        if (transactions.isEmpty()) return;

        List<Long> userIds = transactions.stream()
                .map(Transaction::getUserId)
                .distinct()
                .toList();

        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        transactions.forEach(t -> {
            User u = userMap.get(t.getUserId());
            if (u != null) {
                t.setCreatedBy(u.getUsername());
                t.setCreatedByAvatar(u.getAvatar());
            } else {
                t.setCreatedBy("未知用户");
            }
        });
    }

    private void enrichWithCreatedBy(Transaction transaction) {
        userRepository.findById(transaction.getUserId())
                .ifPresent(u -> {
                    transaction.setCreatedBy(u.getUsername());
                    transaction.setCreatedByAvatar(u.getAvatar());
                });
    }
}
