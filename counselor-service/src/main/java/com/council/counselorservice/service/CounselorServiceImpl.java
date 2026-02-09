package com.council.counselorservice.service;
import com.council.counselorservice.dto.request.CreateCounselorRequest;
import com.council.counselorservice.dto.request.UpdateCounselorRequest;
import com.council.counselorservice.dto.response.CounselorResponse;
import com.council.counselorservice.exception.AccessDeniedException;
import com.council.counselorservice.exception.ResourceNotFoundException;
import com.council.counselorservice.model.Counselor;
import com.council.counselorservice.model.Specialization;
import com.council.counselorservice.repository.CounselorRepository;
import com.council.counselorservice.repository.SpecializationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CounselorServiceImpl implements CounselorService {

    private final CounselorRepository counselorRepository;
    private final SpecializationRepository specializationRepository;

    public CounselorServiceImpl(CounselorRepository counselorRepository,
                                SpecializationRepository specializationRepository) {
        this.counselorRepository = counselorRepository;
        this.specializationRepository = specializationRepository;
    }

    @Override
//    public CreateCounselorRequest createCounselor(CreateCounselorRequest counselor) {
//
//        // Rule: One counselor profile per user
//        if (counselorRepository.existsByUserId(counselor.getUserId())) {
//            throw new IllegalStateException("Counselor profile already exists");
//        }
//
//        // Handle specializations (avoid duplicates)
//        Set<Specialization> managedSpecializations = new HashSet<>();
//
//        for (Specialization specialization : counselor.getSpecializations()) {
//            Specialization existing = specializationRepository
//                    .findByName(specialization.getName())
//                    .orElseGet(() -> specializationRepository.save(specialization));
//
//            managedSpecializations.add(existing);
//        }
//
//        counselor.setSpecializations(managedSpecializations);
//        return counselorRepository.save(counselor);
//    }
    public CounselorResponse createCounselor(Long userId,CreateCounselorRequest request,String role) {
        if (!"THERAPIST".equals(role)) {
            throw new AccessDeniedException("Only therapists can create counselor profiles");
        }
        if (counselorRepository.existsByUserId(userId)) {
            throw new IllegalStateException("Counselor profile already exists");
        }

        Counselor counselor = new Counselor();
        counselor.setUserId(userId);
        counselor.setFullName(request.getFullName());
        counselor.setQualification(request.getQualification());
        counselor.setExperienceYears(request.getExperienceYears());
        counselor.setBio(request.getBio());
        counselor.setPricePerSession(request.getPricePerSession());

        Set<Specialization> specializations =
                request.getSpecializations().stream()
                        .map(name -> specializationRepository
                                .findByName(name)
                                .orElseGet(() -> {
                                    Specialization s = new Specialization();
                                    s.setName(name);
                                    return specializationRepository.save(s);
                                })
                        )
                        .collect(Collectors.toSet());


        counselor.setSpecializations(specializations);

        Counselor saved = counselorRepository.save(counselor);
        return mapToResponse(saved);
    }
    @Override
//    public Counselor getByUserId(Long userId) {
//        return counselorRepository.findByUserId(userId)
//                .orElseThrow(() -> new ResourceNotFoundException("Counselor not found"));
//    }
    @Transactional(readOnly = true)
    public CounselorResponse getByUserId(Long userId) {
        Counselor counselor = counselorRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Counselor not found"));


        return mapToResponse(counselor);
    }
    @Override
//    public List<Counselor> getAllActiveCounselors() {
//        return counselorRepository.findAll()
//                .stream()
//                .filter(Counselor::isActive)
//                .toList();
//    }
    @Transactional(readOnly = true)
    public Set<CounselorResponse> getAllActiveCounselors() {
        return counselorRepository.findByActiveTrue()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional(readOnly = true)
    public CounselorResponse getById(Long counselorId) {
        Counselor counselor = counselorRepository.findById(counselorId)
                .orElseThrow(() -> new ResourceNotFoundException("Counselor not found"));


        return mapToResponse(counselor);
    }


    private CounselorResponse mapToResponse(Counselor counselor) {
        return CounselorResponse.builder()
                .id(counselor.getId())
                .userId(counselor.getUserId())
                .fullName(counselor.getFullName())
                .qualification(counselor.getQualification())
                .experienceYears(counselor.getExperienceYears())
                .bio(counselor.getBio())
                .pricePerSession(counselor.getPricePerSession())
                .active(counselor.isActive())
                .specializations(
                        counselor.getSpecializations()
                                .stream()
                                .map(Specialization::getName)
                                .collect(Collectors.toSet())
                )
                .build();
    }
    @Override
    public CounselorResponse updateCounselor(
            Long counselorId,
            Long userId,
            String role,
            UpdateCounselorRequest request
    )
    {
        if (!"THERAPIST".equals(role)) {
            throw new AccessDeniedException("Only therapists can update counselor profiles");
        }

        Counselor counselor = counselorRepository.findById(counselorId)
                .orElseThrow(() -> new ResourceNotFoundException("Counselor not found"));


        if (!counselor.getUserId().equals(userId)) {
            throw new AccessDeniedException("You are not allowed to update this profile");

        }


        // Update simple fields
        if (request.getFullName() != null) {
            counselor.setFullName(request.getFullName());
        }

        if (request.getQualification() != null) {
            counselor.setQualification(request.getQualification());
        }

        if (request.getExperienceYears() != null) {
            counselor.setExperienceYears(request.getExperienceYears());
        }


        if (request.getBio() != null) {
            counselor.setBio(request.getBio());
        }

        if (request.getPricePerSession() != null) {
            counselor.setPricePerSession(request.getPricePerSession());
        }

        if (request.getActive() != null) {
            counselor.setActive(request.getActive());
        }

        // Update specializations (replace existing set)
        if (request.getSpecializations() != null && !request.getSpecializations().isEmpty()) {

            Set<Specialization> updatedSpecializations =
                    request.getSpecializations().stream()
                            .map(name -> specializationRepository
                                    .findByName(name)
                                    .orElseGet(() -> {
                                        Specialization s = new Specialization();
                                        s.setName(name);
                                        return specializationRepository.save(s);
                                    })
                            )
                            .collect(Collectors.toSet());

            counselor.setSpecializations(updatedSpecializations);
        }

        Counselor updated = counselorRepository.save(counselor);
        return mapToResponse(updated);
    }

}
