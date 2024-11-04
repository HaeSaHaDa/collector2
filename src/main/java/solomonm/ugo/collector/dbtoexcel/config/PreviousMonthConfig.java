package solomonm.ugo.collector.dbtoexcel.config;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;

@Slf4j
public class PreviousMonthConfig {
    /**
     * 전달의 [첫번째 날, 마지막날, 연월, 월]을 구하는 클래스
     */
    static LocalDate lastMonth;
    static LocalDate firstDay;
    static LocalDate lastDay;

    public static String firstDay_yyyyMMdd;
    public static String lastDay_yyyyMMdd;
    public static String lastMonth_yyyyMM;
    public static String lastMonth_MM;
    public static Instant now_5min;


    static {
        try {
            now_5min = Instant.now().plus(Duration.ofSeconds(1));
            lastMonth = LocalDate.now().minusMonths(1);
            firstDay = lastMonth.with(TemporalAdjusters.firstDayOfMonth());
            lastDay = lastMonth.with(TemporalAdjusters.lastDayOfMonth());

            firstDay_yyyyMMdd = firstDay.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            lastDay_yyyyMMdd = lastDay.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            lastMonth_yyyyMM = lastMonth.format(DateTimeFormatter.ofPattern("yyyyMM"));
            lastMonth_MM = lastMonth.format(DateTimeFormatter.ofPattern("MM"));
        } catch (Exception e) {
            // 예외 발생 시 로깅 및 기본 값 설정
            log.error("날짜 처리 중 오류 발생: " + e.getMessage());
            lastMonth_yyyyMM = "000000"; // 기본 값
            lastMonth_MM = "00";         // 기본 값
        }
    }

}
