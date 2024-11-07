package solomonm.ugo.collector.dbtoexcel.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;
import solomonm.ugo.collector.dbtoexcel.config.PreviousMonthConfig;
import solomonm.ugo.collector.dbtoexcel.dto.ExcelColDTO;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.util.List;

@Slf4j
@Component
@EnableScheduling
public class ExcelFileGenerator {
    private final ExceptionSender exceptionSender;
    private boolean retry = true; // 재시도 플래그를 처음에는 true로 설정

    @Autowired
    private TaskScheduler taskScheduler;

    public ExcelFileGenerator(ExceptionSender exceptionSender) {
        this.exceptionSender = exceptionSender;
    }

    /**
     * 셀에 값을 설정하고 테두리 스타일을 적용합니다.
     *
     * @param cell      설정할 셀
     * @param value     셀에 설정할 값
     * @param cellStyle 적용할 셀 스타일
     */
    private void setCellValueWithBorder(Cell cell, Object value, ExcelCellStyle cellStyle) {
        if (value != null) {
            if (value instanceof Number) {
                cell.setCellValue(((Number) value).doubleValue());
            } else {
                cell.setCellValue(value.toString());
            }
        } else {
            cell.setBlank();
        }

        cell.setCellStyle(cellStyle.getStyle()); // 스타일 적용
    }


    /**
     * 주어진 데이터를 기반으로 Excel 파일을 생성하는 메서드입니다. 실패 시 다시 한 번 재시도한 후 생성히 불가능하면 exception에 대해 메일을 발송합니다.
     *
     * @param fileHeader Excel 파일의 헤더 목록
     * @param filePath   생성할 파일의 경로
     * @param month      데이터의 기준이 되는 월 정보
     * @param dbData     파일에 기록할 데이터 리스트
     * @param extension  파일의 확장자 ("xlsx" 또는 "xls")
     */
    public void generateFile(List<String> fileHeader, String filePath, String month, List<ExcelColDTO> dbData, String extension) {
        try (BufferedOutputStream fileOut = new BufferedOutputStream(new FileOutputStream(filePath));
             Workbook workbook = "xlsx".equals(extension) ? new SXSSFWorkbook() : new HSSFWorkbook()) {

            // 시트 및 헤더 생성
            Sheet sheet = workbook.createSheet("Data");
            Row headerRow = sheet.createRow(0);
            ExcelCellStyle headerStyle = new ExcelCellStyle(workbook, true);

            // 헤더 셀 추가
            for (int i = 0; i < fileHeader.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(fileHeader.get(i));
                cell.setCellStyle(headerStyle.getStyle());
            }
            // 첫 번째 셀에 이전 달 정보 설정
            String firstTitle = "'" + PreviousMonthConfig.lastMonth_yyyyMM + "'";
            Cell cell0 = headerRow.createCell(0);
            cell0.setCellValue(firstTitle);
            cell0.setCellStyle(headerStyle.getStyle());

            // 데이터 행 생성
            ExcelCellStyle borderedStyle = new ExcelCellStyle(sheet.getWorkbook(), false);
            int rowIndex = 1;
            for (ExcelColDTO dto : dbData) {
                Row row = sheet.createRow(rowIndex++);
                setCellValueWithBorder(row.createCell(0), month, borderedStyle);
                setCellValueWithBorder(row.createCell(1), dto.getRoad_name(), borderedStyle);
                setCellValueWithBorder(row.createCell(2), dto.getDir_name(), borderedStyle);
                setCellValueWithBorder(row.createCell(3), dto.getSt_name(), borderedStyle);
                setCellValueWithBorder(row.createCell(4), dto.getEd_name(), borderedStyle);
                setCellValueWithBorder(row.createCell(5), dto.getDistance(), borderedStyle);
                setCellValueWithBorder(row.createCell(6), dto.getWeekDay_avg(), borderedStyle);
                setCellValueWithBorder(row.createCell(7), dto.getWeekEnd_avg(), borderedStyle);
                setCellValueWithBorder(row.createCell(8), dto.getAll_avg(), borderedStyle);
            }

            // 파일 저장
            workbook.write(fileOut);
            log.info("{} 파일이 생성되었습니다. generateFile", extension);
            log.info("------------------------------------------------------------> [ END ]");

        } catch (Exception e) {
            // 재시도 플래그 설정하여 한 번만 재시도 예약
            if (retry) {
                retry = false;
                log.error("{} 파일 생성 중 오류가 발생하여 5분 뒤에 재시도 합니다. 오류: {}", extension, e.getMessage());
                taskScheduler.schedule(
                        () -> generateFile(fileHeader, filePath, month, dbData, extension),
                        PreviousMonthConfig.now_5min
                );
                log.info("다음 재시도 시각: {}", PreviousMonthConfig.now_5min);
            }else{
                exceptionSender.exceptionSender(e.getMessage());
            }

        }
    }





    // 내부 클래스: ExcelCellStyle
    private static class ExcelCellStyle {
        private final CellStyle style;

        public ExcelCellStyle(Workbook workbook, boolean isHeader) {
            style = workbook.createCellStyle();
            if (isHeader) {
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
        }

        public CellStyle getStyle() {
            return style;
        }
    }
}
