package com.moneykeeper.service;

import com.moneykeeper.entity.Tag;
import com.moneykeeper.repository.TagRepository;
import com.moneykeeper.repository.TransactionTagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class TagService {

    private final TagRepository tagRepository;
    private final TransactionTagRepository transactionTagRepository;
    private final AccountBookService accountBookService;
    private final AccountBookMemberService memberService;

    public TagService(TagRepository tagRepository, TransactionTagRepository transactionTagRepository,
                      AccountBookService accountBookService, AccountBookMemberService memberService) {
        this.tagRepository = tagRepository;
        this.transactionTagRepository = transactionTagRepository;
        this.accountBookService = accountBookService;
        this.memberService = memberService;
    }

    public List<Tag> getUserTags(Long userId) {
        return tagRepository.findByUserIdOrderByNameAsc(userId);
    }

    public List<Tag> searchTags(Long userId, Long accountBookId, String keyword) {
        if (accountBookId == null) {
            accountBookId = accountBookService.getDefaultAccountBook(userId).getId();
        }

        checkAccess(userId, accountBookId);

        if (keyword == null || keyword.trim().isEmpty()) {
            return tagRepository.findByAccountBookIdOrderByNameAsc(accountBookId);
        }
        return tagRepository.findByAccountBookIdAndNameContainingIgnoreCase(accountBookId, keyword.trim());
    }

    public Tag createTag(Long userId, Long accountBookId, String name, String color) {
        if (accountBookId == null) {
            accountBookId = accountBookService.getDefaultAccountBook(userId).getId();
        }

        checkAccess(userId, accountBookId);

        Optional<Tag> existing = tagRepository.findByAccountBookIdAndName(accountBookId, name.trim());
        if (existing.isPresent()) {
            return existing.get();
        }

        Tag tag = new Tag();
        tag.setUserId(userId);
        tag.setAccountBookId(accountBookId);
        tag.setName(name.trim());
        tag.setColor(color != null && !color.trim().isEmpty() ? color.trim() : "#409EFF");
        return tagRepository.save(tag);
    }

    public Tag updateTag(Long userId, Long id, String name, String color) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("标签不存在"));

        if (tag.getAccountBookId() != null) {
            checkAccess(userId, tag.getAccountBookId());
        } else if (!tag.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权限修改此标签");
        }

        if (name != null && !name.trim().isEmpty()) {
            Optional<Tag> existing = tagRepository.findByUserIdAndName(userId, name.trim());
            if (existing.isPresent() && !existing.get().getId().equals(id)) {
                throw new IllegalArgumentException("标签名称已存在");
            }
            tag.setName(name.trim());
        }

        if (color != null && !color.trim().isEmpty()) {
            tag.setColor(color.trim());
        }

        return tagRepository.save(tag);
    }

    @Transactional
    public void deleteTag(Long userId, Long id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("标签不存在"));

        if (tag.getAccountBookId() != null) {
            checkAccess(userId, tag.getAccountBookId());
        } else if (!tag.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权限删除此标签");
        }

        transactionTagRepository.deleteAll(transactionTagRepository.findByTagId(id));
        tagRepository.delete(tag);
    }

    public List<Long> getOrCreateTags(Long userId, Long accountBookId, List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return List.of();
        }

        if (accountBookId == null) {
            accountBookId = accountBookService.getDefaultAccountBook(userId).getId();
        }

        checkAccess(userId, accountBookId);

        Long finalAccountBookId = accountBookId;
        return tagNames.stream()
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .distinct()
                .map(name -> {
                    Optional<Tag> existing = tagRepository.findByAccountBookIdAndName(finalAccountBookId, name);
                    if (existing.isPresent()) {
                        return existing.get().getId();
                    }
                    Tag tag = new Tag();
                    tag.setUserId(userId);
                    tag.setAccountBookId(finalAccountBookId);
                    tag.setName(name);
                    tag.setColor("#409EFF");
                    return tagRepository.save(tag).getId();
                })
                .toList();
    }

    private void checkAccess(Long userId, Long accountBookId) {
        if (!memberService.hasAccess(userId, accountBookId)) {
            throw new IllegalArgumentException("您没有访问此账本的权限");
        }
    }
}
