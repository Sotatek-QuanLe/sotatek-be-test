package com.sotatek.order.model.dto.external;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MemberResponse {
    private Long id;
    private String name;
    private String email;
    private String status; // ACTIVE, INACTIVE, SUSPENDED
    private String grade; // BRONZE, SILVER, GOLD, PLATINUM
}
