package com.moneykeeper.controller;

import com.moneykeeper.dto.InviteRequest;
import com.moneykeeper.dto.MemberResponse;
import com.moneykeeper.dto.Result;
import com.moneykeeper.entity.AccountBook;
import com.moneykeeper.entity.User;
import com.moneykeeper.service.AccountBookMemberService;
import com.moneykeeper.service.AccountBookService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/account-books")
public class AccountBookController {

    private final AccountBookService accountBookService;
    private final AccountBookMemberService memberService;

    public AccountBookController(AccountBookService accountBookService,
                                 AccountBookMemberService memberService) {
        this.accountBookService = accountBookService;
        this.memberService = memberService;
    }

    @GetMapping
    public Result<List<AccountBook>> getAccountBooks(@AuthenticationPrincipal User user) {
        return Result.success(accountBookService.getAccountBooks(user.getId()));
    }

    @PostMapping
    public Result<AccountBook> createAccountBook(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, String> body) {
        String name = body.get("name");
        String description = body.get("description");
        if (name == null || name.trim().isEmpty()) {
            return Result.error(400, "账本名称不能为空");
        }
        return Result.success(accountBookService.createAccountBook(user.getId(), name, description));
    }

    @PutMapping("/{id}")
    public Result<AccountBook> updateAccountBook(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String name = body.get("name");
        String description = body.get("description");
        return Result.success(accountBookService.updateAccountBook(user.getId(), id, name, description));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteAccountBook(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        accountBookService.deleteAccountBook(user.getId(), id);
        return Result.success(null);
    }

    @GetMapping("/{id}/members")
    public Result<List<MemberResponse>> getMembers(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        if (!memberService.isOwner(user.getId(), id)) {
            return Result.error(403, "只有账本所有者才能查看成员列表");
        }
        return Result.success(memberService.getMembers(id));
    }

    @PostMapping("/{id}/members/invite")
    public Result<MemberResponse> inviteMember(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestBody InviteRequest request) {
        return Result.success(memberService.inviteMember(user.getId(), id, request.getUsername()));
    }

    @PutMapping("/{id}/members/{memberId}/accept")
    public Result<MemberResponse> acceptInvitation(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @PathVariable Long memberId) {
        return Result.success(memberService.acceptInvitation(user.getId(), memberId));
    }

    @PutMapping("/{id}/members/{memberId}/reject")
    public Result<Void> rejectInvitation(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @PathVariable Long memberId) {
        memberService.rejectInvitation(user.getId(), memberId);
        return Result.success(null);
    }

    @DeleteMapping("/{id}/members/{memberId}")
    public Result<Void> removeMember(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @PathVariable Long memberId) {
        memberService.removeMember(user.getId(), id, memberId);
        return Result.success(null);
    }

    @PostMapping("/{id}/leave")
    public Result<Void> leaveAccountBook(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        memberService.leaveAccountBook(user.getId(), id);
        return Result.success(null);
    }

    @PutMapping("/{id}/alias")
    public Result<Void> setAlias(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        if (!memberService.hasAccess(user.getId(), id)) {
            return Result.error(403, "您不是该账本的成员，无法设置别名");
        }
        memberService.setAlias(user.getId(), id, body.get("alias"));
        return Result.success(null);
    }

    @GetMapping("/invitations")
    public Result<List<MemberResponse>> getPendingInvitations(@AuthenticationPrincipal User user) {
        return Result.success(memberService.getPendingInvitations(user.getId()));
    }

    @GetMapping("/{id}/role")
    public Result<Map<String, String>> getMyRole(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        String role = memberService.getMemberRole(user.getId(), id);
        return Result.success(Map.of("role", role != null ? role : "NONE"));
    }
}
