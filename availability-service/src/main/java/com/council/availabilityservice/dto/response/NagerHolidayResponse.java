package com.council.availabilityservice.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NagerHolidayResponse {
    private String date;
    private String localName;
    private String name;
    private String countryCode;
}
