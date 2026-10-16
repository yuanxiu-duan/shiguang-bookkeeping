package com.moneykeeper.service;

import com.moneykeeper.entity.AccountBook;
import com.moneykeeper.repository.AccountBookMemberRepository;
import com.moneykeeper.repository.AccountBookRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AccountBookService {

    private final AccountBookRepository accountBookRepository;
    private final AccountBookMemberRepository memberRepository;
    private final AccountBookMemberService memberService;

    public AccountBookService(AccountBookRepository accountBookRepository,
                              AccountBookMemberRepository memberRepository,
                              AccountBookMemberService memberService) {
        this.accountBookRepository = accountBookRepository;
        this.memberRepository = memberRepository;
        this.memberService = memberService;
    }

    public List<AccountBook> getAccountBooks(Long userId) {
        List<AccountBook> ownBooks = accountBookRepository.findByUserIdOrderByIdAsc(userId);
        List<AccountBook> sharedBooks = memberService.getSharedAccountBooks(userId);
        List<AccountBook> result = new ArrayList<>(ownBooks);
        for (AccountBook shared : sharedBooks) {
            if (result.stream().noneMatch(b -> b.getId().equals(shared.getId()))) {
                result.add(shared);
            }
        }

        Map<Long, String> aliasMap = memberRepository.findByUserId(userId).stream()
                .filter(m -> m.getAlias() != null)
                .collect(Collectors.toMap(
                        m -> m.getAccountBookId(),
                        m -> m.getAlias()
                ));

        for (AccountBook book : result) {
            String alias = aliasMap.get(book.getId());
            book.setAlias(alias);
        }

        return result;
    }

    @Transactional
    public AccountBook createAccountBook(Long userId, String name, String description) {
        if (accountBookRepository.existsByUserIdAndName(userId, name)) {
            throw new IllegalArgumentException("账本名称已存在");
        }

        AccountBook accountBook = new AccountBook();
        accountBook.setUserId(userId);
        accountBook.setName(name);
        accountBook.setDescription(description);
        accountBook.setIsDefault(false);

        AccountBook saved = accountBookRepository.save(accountBook);

        memberService.createOwnerMember(saved.getId(), userId);

        return saved;
    }

    @Transactional
    public AccountBook updateAccountBook(Long userId, Long id, String name, String description) {
        AccountBook accountBook = accountBookRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("账本不存在"));

        if (!memberService.isOwner(userId, id)) {
            throw new IllegalArgumentException("无权限修改此账本");
        }

        if (name != null && !name.trim().isEmpty()) {
            if (!name.equals(accountBook.getName()) && accountBookRepository.existsByUserIdAndName(userId, name)) {
                throw new IllegalArgumentException("账本名称已存在");
            }
            accountBook.setName(name.trim());
        }

        if (description != null) {
            accountBook.setDescription(description);
        }

        return accountBookRepository.save(accountBook);
    }

    @Transactional
    public void deleteAccountBook(Long userId, Long id) {
        AccountBook accountBook = accountBookRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("账本不存在"));

        if (!memberService.isOwner(userId, id)) {
            throw new IllegalArgumentException("无权限删除此账本");
        }

        if (Boolean.TRUE.equals(accountBook.getIsDefault())) {
            throw new IllegalArgumentException("默认账本不能删除");
        }

        accountBookRepository.delete(accountBook);
    }

    @Transactional
    public AccountBook getDefaultAccountBook(Long userId) {
        return accountBookRepository.findByUserIdAndIsDefaultTrue(userId)
                .orElseGet(() -> {
                    AccountBook defaultBook = new AccountBook();
                    defaultBook.setUserId(userId);
                    defaultBook.setName("默认账本");
                    defaultBook.setIsDefault(true);
                    AccountBook saved = accountBookRepository.save(defaultBook);
                    memberService.createOwnerMember(saved.getId(), userId);
                    return saved;
                });
    }

    @Transactional
    public AccountBook createDefaultAccountBook(Long userId) {
        return getDefaultAccountBook(userId);
    }
}
