package com.council.appointmentservice.client;

import com.council.appointmentservice.dto.response.CounselorResponse;

public interface CounselorClient {
    CounselorResponse getCounselorByUserId(Long userId);
}
