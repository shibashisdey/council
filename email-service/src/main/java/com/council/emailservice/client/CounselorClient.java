package com.council.emailservice.client;

import com.council.emailservice.dto.CounselorResponse;

public interface CounselorClient {
    CounselorResponse getCounselor(Long counselorId);
}
