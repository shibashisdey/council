package com.council.emailservice.client;

import com.council.emailservice.dto.UserProfileResponse;

public interface UserClient {
    UserProfileResponse getUserPublic(Long userId);
}
