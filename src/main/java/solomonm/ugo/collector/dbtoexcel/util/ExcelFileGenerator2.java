package solomonm.ugo.collector.dbtoexcel.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;
import solomonm.ugo.collector.dbtoexcel.config.PreviousMonthConfig;
import solomonm.ugo.collector.dbtoexcel.dto.ExcelColDTO;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@EnableScheduling
public class ExcelFileGenerator2 {
    private final ExceptionSender exceptionSender;
    private final TaskScheduler taskScheduler;
    private boolean retry = true; // 재시도 플래그를 처음에는 true로 설정
    private CellStyle style;

    public ExcelFileGenerator2(ExceptionSender exceptionSender, TaskScheduler taskScheduler) {
        this.exceptionSender = exceptionSender;
        this.taskScheduler = taskScheduler;
    }


    /**
     * 셀에 값을 설정하고 테두리 스타일을 적용합니다.
     *
     * @param workbook 엑셀 워크북 인스턴스
     * @param cell     값과 스타일을 설정할 셀
     * @param value    셀에 설정할 값 (Number 또는 String)
     * @param isHeader true면 헤더 스타일을 적용하고, false면 기본 스타일을 적용
     */
    private void setCellValueWithBorder(
            Workbook workbook,
            Cell cell,
            Object value,
            boolean isHeader
    ) {
        // 셀에 값을 설정
        if (value != null) {
            if (value instanceof Number) {
                cell.setCellValue(((Number) value).doubleValue()); // 숫자 타입이면 숫자 값으로 설정
            } else {
                cell.setCellValue(value.toString()); // 문자열로 변환 후 설정
            }
        } else {
            cell.setBlank(); // 값이 null이면 빈 셀로 설정
        }

        // 셀 스타일 생성
        style = workbook.createCellStyle();

        if (isHeader) {
            // 헤더 스타일 지정 (굵은 Arial 폰트, 폰트 크기 10)
            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            boldFont.setFontName("Arial");
            boldFont.setFontHeightInPoints((short) 10);
            style.setFont(boldFont);
        }
        // 테두리 스타일 적용
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        // 스타일을 셀에 적용
        cell.setCellStyle(style);
    }


    /**
     * 주어진 데이터를 기반으로 Excel 파일을 생성하는 메서드입니다. 실패 시 다시 한 번 재시도한 후 생성히 불가능하면 exception에 대해 메일을 발송합니다.
     *
     * @param fileHeader Excel 파일의 헤더 목록
     * @param filePath   생성할 파일의 경로
     * @param fileName   생성할 파일의 이름
     * @param month      데이터의 기준이 되는 월 정보
     * @param dbData     파일에 기록할 데이터 리스트
     * @param extension  파일의 확장자 ("xlsx" 또는 "xls")
     */
    public void generateFile(
            List<String> fileHeader,
            String filePath,
            String fileName,
            String month,
            List<ExcelColDTO> dbData,
            String extension,
            Instant fileRegenTime
    ) {
        try (BufferedOutputStream fileOut = new BufferedOutputStream(new FileOutputStream(filePath));
             Workbook workbook = "xlsx".equals(extension) ? new SXSSFWorkbook() : new HSSFWorkbook()) {

            // 시트 및 헤더 생성
            Sheet sheet = workbook.createSheet("Data");
            Row headerRow = sheet.createRow(0);

            // 헤더 셀 추가
            for (int i = 0; i < fileHeader.size(); i++) {
                setCellValueWithBorder(workbook, headerRow.createCell(i), fileHeader.get(i), true);
            }
            // 첫 번째 셀에 이전 달 정보 설정
            String firstTitle = "'" + PreviousMonthConfig.lastMonth_yyyyMM + "'";
            setCellValueWithBorder(workbook, headerRow.createCell(0), firstTitle, true);

            // 데이터 행 생성 및 각 셀에 데이터와 스타일 적용
            int rowIndex = 1;
            for (ExcelColDTO dto : dbData) {
                Row row = sheet.createRow(rowIndex++);
                setCellValueWithBorder(workbook, row.createCell(0), month, false);
                setCellValueWithBorder(workbook, row.createCell(1), dto.getRoad_name(), false);
                setCellValueWithBorder(workbook, row.createCell(2), dto.getDir_name(), false);
                setCellValueWithBorder(workbook, row.createCell(3), dto.getSt_name(), false);
                setCellValueWithBorder(workbook, row.createCell(4), dto.getEd_name(), false);
                setCellValueWithBorder(workbook, row.createCell(5), dto.getDistance(), false);
                setCellValueWithBorder(workbook, row.createCell(6), dto.getWeekDay_avg(), false);
                setCellValueWithBorder(workbook, row.createCell(7), dto.getWeekEnd_avg(), false);
                setCellValueWithBorder(workbook, row.createCell(8), dto.getAll_avg(), false);

            }

            // 파일 저장
            workbook.write(fileOut);

            log.info("{} 파일이 생성되었습니다. ", fileName);
            log.info("------------------------------------------------------------> [ END ]");

        } catch (Exception e) {
            // 재시도 플래그 설정하여 한 번만 재시도 예약
            if (retry) {
                retry = false;
                log.error("{} 파일 생성 중 오류가 발생하여 5분 뒤에 재시도 합니다. 오류: {}", fileName, e.getMessage());
                taskScheduler.schedule(
                        () -> generateFile(fileHeader, filePath, fileName, month, dbData, extension, fileRegenTime),
                        fileRegenTime
                );
                log.info("다음 재시도 시각: {}", fileRegenTime);
            } else {
                exceptionSender.exceptionSender(fileName, e.getMessage());
            }

        }
    }

}
