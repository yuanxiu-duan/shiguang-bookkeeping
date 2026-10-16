package com.moneykeeper.service;

import com.moneykeeper.dto.MemberResponse;
import com.moneykeeper.entity.AccountBook;
import com.moneykeeper.entity.AccountBookMember;
import com.moneykeeper.entity.AccountBookMember.MemberRole;
import com.moneykeeper.entity.AccountBookMember.MemberStatus;
import com.moneykeeper.entity.User;
import com.moneykeeper.repository.AccountBookMemberRepository;
import com.moneykeeper.repository.AccountBookRepository;
import com.moneykeeper.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AccountBookMemberService {

    private final AccountBookMemberRepository memberRepository;
    private final AccountBookRepository accountBookRepository;
    private final UserRepository userRepository;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public AccountBookMemberService(AccountBookMemberRepository memberRepository,
                                    AccountBookRepository accountBookRepository,
                                    UserRepository userRepository) {
        this.memberRepository = memberRepository;
        this.accountBookRepository = accountBookRepository;
        this.userRepository = userRepository;
    }

    public boolean hasAccess(Long userId, Long accountBookId) {
        return memberRepository.existsByAccountBookIdAndUserIdAndStatus(
                accountBookId, userId, MemberStatus.ACCEPTED);
    }

    public boolean isOwner(Long userId, Long accountBookId) {
        return memberRepository.findByAccountBookIdAndUserId(accountBookId, userId)
                .map(m -> m.getRole() == MemberRole.OWNER)
                .orElse(false);
    }

    @Transactional
    public AccountBookMember createOwnerMember(Long accountBookId, Long userId) {
        AccountBookMember member = new AccountBookMember();
        member.setAccountBookId(accountBookId);
        member.setUserId(userId);
        member.setRole(MemberRole.OWNER);
        member.setStatus(MemberStatus.ACCEPTED);
        member.setInvitedBy(userId);
        return memberRepository.save(member);
    }

    @Transactional
    public MemberResponse inviteMember(Long ownerUserId, Long accountBookId, String username) {
        AccountBook accountBook = accountBookRepository.findById(accountBookId)
                .orElseThrow(() -> new IllegalArgumentException("账本不存在"));

        if (!accountBook.getUserId().equals(ownerUserId)) {
            throw new IllegalArgumentException("只有账本所有者才能邀请成员");
        }

        User invitee = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        if (invitee.getId().equals(ownerUserId)) {
            throw new IllegalArgumentException("不能邀请自己");
        }

        memberRepository.findByAccountBookIdAndUserId(accountBookId, invitee.getId())
                .ifPresent(existing -> {
                    if (existing.getStatus() == MemberStatus.ACCEPTED) {
                        throw new IllegalArgumentException("该用户已是账本成员");
                    }
                    if (existing.getStatus() == MemberStatus.PENDING) {
                        throw new IllegalArgumentException("已向该用户发送过邀请，请等待对方接受");
                    }
                    existing.setStatus(MemberStatus.PENDING);
                    existing.setInvitedBy(ownerUserId);
                    memberRepository.save(existing);
                    throw new IllegalArgumentException("已重新发送邀请");
                });

        AccountBookMember member = new AccountBookMember();
        member.setAccountBookId(accountBookId);
        member.setUserId(invitee.getId());
        member.setRole(MemberRole.MEMBER);
        member.setStatus(MemberStatus.PENDING);
        member.setInvitedBy(ownerUserId);
        AccountBookMember saved = memberRepository.save(member);

        return toMemberResponse(saved);
    }

    @Transactional
    public MemberResponse acceptInvitation(Long userId, Long memberId) {
        AccountBookMember member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("邀请不存在"));

        if (!member.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权操作此邀请");
        }

        if (member.getStatus() != MemberStatus.PENDING) {
            throw new IllegalArgumentException("邀请状态无效");
        }

        member.setStatus(MemberStatus.ACCEPTED);
        AccountBookMember saved = memberRepository.save(member);
        return toMemberResponse(saved);
    }

    @Transactional
    public void rejectInvitation(Long userId, Long memberId) {
        AccountBookMember member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("邀请不存在"));

        if (!member.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权操作此邀请");
        }

        if (member.getStatus() != MemberStatus.PENDING) {
            throw new IllegalArgumentException("邀请状态无效");
        }

        memberRepository.delete(member);
    }

    @Transactional
    public void removeMember(Long ownerUserId, Long accountBookId, Long memberId) {
        AccountBook accountBook = accountBookRepository.findById(accountBookId)
                .orElseThrow(() -> new IllegalArgumentException("账本不存在"));

        if (!accountBook.getUserId().equals(ownerUserId)) {
            throw new IllegalArgumentException("只有账本所有者才能移除成员");
        }

        AccountBookMember member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("成员不存在"));

        if (!member.getAccountBookId().equals(accountBookId)) {
            throw new IllegalArgumentException("成员不属于该账本");
        }

        if (member.getRole() == MemberRole.OWNER) {
            throw new IllegalArgumentException("不能移除账本所有者");
        }

        memberRepository.delete(member);
    }

    @Transactional
    public void leaveAccountBook(Long userId, Long accountBookId) {
        AccountBookMember member = memberRepository.findByAccountBookIdAndUserId(accountBookId, userId)
                .orElseThrow(() -> new IllegalArgumentException("您不是该账本的成员"));

        if (member.getRole() == MemberRole.OWNER) {
            throw new IllegalArgumentException("账本所有者不能退出，请先转让或删除账本");
        }

        memberRepository.delete(member);
    }

    public List<MemberResponse> getMembers(Long accountBookId) {
        List<AccountBookMember> accepted = memberRepository.findByAccountBookIdAndStatus(
                accountBookId, MemberStatus.ACCEPTED);
        List<AccountBookMember> pending = memberRepository.findByAccountBookIdAndStatus(
                accountBookId, MemberStatus.PENDING);

        List<AccountBookMember> all = new java.util.ArrayList<>(accepted);
        all.addAll(pending);
        return toMemberResponseList(all);
    }

    public List<MemberResponse> getPendingInvitations(Long userId) {
        List<AccountBookMember> pending = memberRepository.findByUserId(userId).stream()
                .filter(m -> m.getStatus() == MemberStatus.PENDING)
                .toList();
        return toMemberResponseList(pending);
    }

    public List<AccountBook> getSharedAccountBooks(Long userId) {
        List<AccountBookMember> memberships = memberRepository.findByUserId(userId).stream()
                .filter(m -> m.getStatus() == MemberStatus.ACCEPTED && m.getRole() == MemberRole.MEMBER)
                .toList();

        List<Long> bookIds = memberships.stream()
                .map(AccountBookMember::getAccountBookId)
                .toList();

        if (bookIds.isEmpty()) {
            return List.of();
        }

        return accountBookRepository.findAllById(bookIds);
    }

    public String getMemberRole(Long userId, Long accountBookId) {
        return memberRepository.findByAccountBookIdAndUserId(accountBookId, userId)
                .map(m -> m.getRole().name())
                .orElse(null);
    }

    @Transactional
    public void setAlias(Long userId, Long accountBookId, String alias) {
        AccountBookMember member = memberRepository.findByAccountBookIdAndUserId(accountBookId, userId)
                .orElseThrow(() -> new IllegalArgumentException("您不是该账本的成员"));

        member.setAlias(alias != null && alias.trim().isEmpty() ? null : alias);
        memberRepository.save(member);
    }

    public String getAlias(Long userId, Long accountBookId) {
        return memberRepository.findByAccountBookIdAndUserId(accountBookId, userId)
                .map(AccountBookMember::getAlias)
                .orElse(null);
    }

    private MemberResponse toMemberResponse(AccountBookMember member) {
        String username = userRepository.findById(member.getUserId())
                .map(User::getUsername).orElse("未知用户");
        String inviterUsername = member.getInvitedBy() != null
                ? userRepository.findById(member.getInvitedBy())
                .map(User::getUsername).orElse("未知用户")
                : null;

        return new MemberResponse(
                member.getId(),
                member.getAccountBookId(),
                member.getUserId(),
                username,
                member.getRole().name(),
                member.getStatus().name(),
                member.getInvitedBy(),
                inviterUsername,
                member.getCreatedAt() != null ? member.getCreatedAt().format(FMT) : null
        );
    }

    private List<MemberResponse> toMemberResponseList(List<AccountBookMember> members) {
        List<Long> userIds = members.stream()
                .flatMap(m -> {
                    List<Long> ids = new java.util.ArrayList<>();
                    ids.add(m.getUserId());
                    if (m.getInvitedBy() != null) {
                        ids.add(m.getInvitedBy());
                    }
                    return ids.stream();
                })
                .distinct()
                .toList();

        Map<Long, String> usernameMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername));

        return members.stream().map(member -> {
            String username = usernameMap.getOrDefault(member.getUserId(), "未知用户");
            String inviterUsername = member.getInvitedBy() != null
                    ? usernameMap.getOrDefault(member.getInvitedBy(), "未知用户")
                    : null;

            return new MemberResponse(
                    member.getId(),
                    member.getAccountBookId(),
                    member.getUserId(),
                    username,
                    member.getRole().name(),
                    member.getStatus().name(),
                    member.getInvitedBy(),
                    inviterUsername,
                    member.getCreatedAt() != null ? member.getCreatedAt().format(FMT) : null
            );
        }).toList();
    }
}
