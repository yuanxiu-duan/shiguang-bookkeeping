package com.moneykeeper.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberResponse {
    private Long id;
    private Long accountBookId;
    private Long userId;
    private String username;
    private String role;
    private String status;
    private Long invitedBy;
    private String inviterUsername;
    private String createdAt;
}
