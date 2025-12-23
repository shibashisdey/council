package com.council.counselorservice.service;

import com.council.counselorservice.dto.request.CreateCounselorRequest;
import com.council.counselorservice.dto.request.UpdateCounselorRequest;
import com.council.counselorservice.dto.response.CounselorResponse;
import com.council.counselorservice.model.Counselor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
@Service
public interface CounselorService {

    CounselorResponse createCounselor(Long userId,CreateCounselorRequest counselor,String role);

    CounselorResponse getByUserId(Long userId);

    Set<CounselorResponse> getAllActiveCounselors();

    CounselorResponse getById(Long counselorId);



    CounselorResponse updateCounselor(
            Long counselorId,
            Long userId,
            String role,
            UpdateCounselorRequest request
    );
}
