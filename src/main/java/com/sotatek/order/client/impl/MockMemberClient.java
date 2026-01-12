package com.sotatek.order.client.impl;

import com.sotatek.order.client.MemberClient;
import com.sotatek.order.exception.MemberNotFoundException;
import com.sotatek.order.model.dto.external.MemberResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import com.sotatek.order.exception.ServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class MockMemberClient implements MemberClient {

    @Override
    @CircuitBreaker(name = "memberService", fallbackMethod = "memberFallback")
    @Retry(name = "memberService")
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

    public MemberResponse memberFallback(String memberId, Throwable t) {
        log.error("Member service fallback for id: {}, error: {}", memberId, t.getMessage());
        throw new ServiceUnavailableException("Member service is temporarily unavailable: " + t.getMessage());
    }
}
