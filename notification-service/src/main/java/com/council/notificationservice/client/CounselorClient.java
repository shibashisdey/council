package com.council.notificationservice.client;

import com.council.notificationservice.dto.CounselorResponse;

public interface CounselorClient {
    CounselorResponse getCounselorByUserId(Long userId);
}
