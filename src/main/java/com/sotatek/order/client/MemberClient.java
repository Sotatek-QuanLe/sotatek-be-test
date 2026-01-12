package com.sotatek.order.client;

import com.sotatek.order.model.dto.external.MemberResponse;
import org.springframework.lang.NonNull;

public interface MemberClient {
    MemberResponse getMember(@NonNull String memberId);
}
