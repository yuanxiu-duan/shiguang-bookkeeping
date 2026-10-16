package com.moneykeeper.controller;

import com.moneykeeper.dto.Result;
import com.moneykeeper.entity.Tag;
import com.moneykeeper.entity.User;
import com.moneykeeper.service.TagService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tags")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    public Result<List<Tag>> getTags(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Long accountBookId,
            @RequestParam(required = false) String keyword) {
        return Result.success(tagService.searchTags(user.getId(), accountBookId, keyword));
    }

    @PostMapping
    public Result<Tag> createTag(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Long accountBookId,
            @RequestBody Map<String, String> body) {
        String name = body.get("name");
        String color = body.get("color");
        if (name == null || name.trim().isEmpty()) {
            return Result.error(400, "标签名称不能为空");
        }
        return Result.success(tagService.createTag(user.getId(), accountBookId, name, color));
    }

    @PutMapping("/{id}")
    public Result<Tag> updateTag(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String name = body.get("name");
        String color = body.get("color");
        return Result.success(tagService.updateTag(user.getId(), id, name, color));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteTag(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        tagService.deleteTag(user.getId(), id);
        return Result.success(null);
    }
}
