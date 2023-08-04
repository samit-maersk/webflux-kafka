package com.example.webfluxkafka;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@ExtendWith(SpringExtension.class)
public class LocalDateAndTimeTest {

    @Test
    void localDateTest () {
        String strDate = "20150804";
        LocalDate aLD = LocalDate.parse(strDate, DateTimeFormatter.ofPattern("yyyyMMdd"));
        System.out.println("Date: " + aLD);
        LocalDate aaLDT = LocalDate.now();
        System.out.println("Current Date : " + aaLDT);

        String strDatewithTime = "2015-08-04T10:11:30";
        LocalDateTime aLDT = LocalDateTime.parse(strDatewithTime);
        System.out.println("Date with Time: " + aLDT);
    }
}
