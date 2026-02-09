package com.council.availabilityservice.repository;

import com.council.availabilityservice.model.PublicHoliday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PublicHolidayRepository
        extends JpaRepository<PublicHoliday, Long> {

    Optional<PublicHoliday> findByHolidayDate(LocalDate date);

    Optional<PublicHoliday> findByHolidayDateAndCountryCode(
            LocalDate date,
            String countryCode
    );

    List<PublicHoliday> findByHolidayDateBefore(LocalDate cutoffDate);

    List<PublicHoliday> findByHolidayDateAfter(LocalDate cutoffDate);

    List<PublicHoliday> findByHolidayDateBetween(LocalDate start, LocalDate end);
}
