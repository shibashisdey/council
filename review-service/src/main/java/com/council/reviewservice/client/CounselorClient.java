package com.council.reviewservice.client;

import com.council.reviewservice.dto.response.CounselorResponse;

public interface CounselorClient {
    CounselorResponse getCounselorByUserId(Long userId);
}
