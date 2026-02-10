package com.council.notificationservice.client;

import com.council.notificationservice.dto.UserPublicResponse;

public interface UserClient {
    UserPublicResponse getUserPublic(Long userId);
}
