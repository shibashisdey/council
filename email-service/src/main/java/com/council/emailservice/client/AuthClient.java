package com.council.emailservice.client;

import com.council.emailservice.dto.AuthUserResponse;

public interface AuthClient {
    AuthUserResponse getUserInternal(Long userId);
}
