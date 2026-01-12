package com.sotatek.order.client.impl;

import com.sotatek.order.client.MemberClient;
import com.sotatek.order.exception.MemberNotFoundException;
import com.sotatek.order.model.dto.external.MemberResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class MockMemberClient implements MemberClient {

    @Override
    public MemberResponse getMember(@NonNull String memberId) {
        if ("not-found".equals(memberId)) {
            throw new MemberNotFoundException("Member not found with id: " + memberId);
        }

        String status = "inactive-member".equals(memberId) ? "INACTIVE" : "ACTIVE";

        return MemberResponse.builder()
                .id(1L)
                .name("Mock User")
                .email("mock@example.com")
                .status(status)
                .grade("GOLD")
                .build();
    }
}
