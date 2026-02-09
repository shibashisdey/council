package com.council.availabilityservice.service;

import com.council.availabilityservice.dto.response.NagerHolidayResponse;
import com.council.availabilityservice.model.PublicHoliday;
import com.council.availabilityservice.repository.PublicHolidayRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Service
@Transactional
public class PublicHolidaySyncService {

    private final PublicHolidayRepository holidayRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${availability.holidays.country}")
    private String countryCode;

    public PublicHolidaySyncService(PublicHolidayRepository holidayRepository) {
        this.holidayRepository = holidayRepository;
    }

    public void syncHolidaysForNext45Days() {
        LocalDate today = LocalDate.now();
        LocalDate limit = today.plusDays(45);

        Set<Integer> years = new HashSet<>();
        years.add(today.getYear());
        years.add(limit.getYear());

        for (Integer year : years) {
            String url = "https://date.nager.at/api/v3/PublicHolidays/"
                    + year + "/" + countryCode;

            NagerHolidayResponse[] holidays =
                    restTemplate.getForObject(url, NagerHolidayResponse[].class);

            if (holidays == null) {
                continue;
            }

            for (NagerHolidayResponse h : holidays) {
                if (h.getDate() == null) {
                    continue;
                }
                LocalDate holidayDate = LocalDate.parse(h.getDate());
                if (holidayDate.isBefore(today) || holidayDate.isAfter(limit)) {
                    continue;
                }

                holidayRepository.findByHolidayDateAndCountryCode(holidayDate, countryCode)
                        .orElseGet(() -> {
                            PublicHoliday ph = new PublicHoliday();
                            ph.setHolidayDate(holidayDate);
                            ph.setName(h.getName() != null ? h.getName() : h.getLocalName());
                            ph.setCountryCode(countryCode);
                            return holidayRepository.save(ph);
                        });
            }
        }
    }
}
