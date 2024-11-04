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
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

@Slf4j
@Component
@EnableScheduling
public class ExcelFileGenerator {
    private final ExceptionSender exceptionSender;
    @Autowired
    private TaskScheduler taskScheduler;

    public ExcelFileGenerator(ExceptionSender exceptionSender) {
        this.exceptionSender = exceptionSender;
    }

    /**
     * Excel 파일의 헤더를 작성합니다.
     *
     * @param fileheader 파일 헤더 리스트
     * @param sheet      생성할 시트
     * @param workbook   워크북
     */
    private void createHeaderRow(List<String> fileheader, Sheet sheet, Workbook workbook) {
        Row headerRow = sheet.createRow(0);
        ExcelCellStyle headerStyle = new ExcelCellStyle(workbook, true);
        String firstTitle = "'" + PreviousMonthConfig.lastMonth_yyyyMM + "'";
        // 첫 번째 셀에 이전 달 정보 추가


        // 나머지 헤더 셀 추가
        for (int i = 0; i < fileheader.size(); i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(fileheader.get(i));
            cell.setCellStyle(headerStyle.getStyle());
        }

        Cell cell0 = headerRow.createCell(0);
        cell0.setCellValue(firstTitle);
        cell0.setCellStyle(headerStyle.getStyle());
    }

    /**
     * Excel 데이터 영역에 값을 입력합니다.
     *
     * @param sheet  생성할 시트
     * @param month  월 정보
     * @param dbData 데이터 리스트
     */
    private void populateDataRows(Sheet sheet, String month, List<ExcelColDTO> dbData) {
        final AtomicInteger rowIndex = new AtomicInteger(1);
        ExcelCellStyle borderedStyle = new ExcelCellStyle(sheet.getWorkbook(), false); // 셀 스타일 생성

        IntStream.range(0, dbData.size()).forEach(i -> {
            ExcelColDTO dto = dbData.get(i);
            Row row = sheet.createRow(rowIndex.getAndIncrement());

            // 각 셀에 값 설정 및 테두리 스타일 적용
            setCellValueWithBorder(row.createCell(0), month, borderedStyle);
            setCellValueWithBorder(row.createCell(1), dto.getRoad_name(), borderedStyle);
            setCellValueWithBorder(row.createCell(2), dto.getDir_name(), borderedStyle);
            setCellValueWithBorder(row.createCell(3), dto.getSt_name(), borderedStyle);
            setCellValueWithBorder(row.createCell(4), dto.getEd_name(), borderedStyle);
            setCellValueWithBorder(row.createCell(5), dto.getDistance(), borderedStyle);
            setCellValueWithBorder(row.createCell(6), dto.getWeekDay_avg(), borderedStyle);
            setCellValueWithBorder(row.createCell(7), dto.getWeekEnd_avg(), borderedStyle);
            setCellValueWithBorder(row.createCell(8), dto.getAll_avg(), borderedStyle);
        });
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
     * xlsx 파일을 생성합니다.
     *
     * @param fileheader 파일 헤더 리스트
     * @param filePath   생성할 파일 경로
     * @param month      월 정보
     * @param data       데이터 리스트
     */
    public void generate_File(List<String> fileheader, String filePath, String month, List<ExcelColDTO> data, String extension) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        try (BufferedOutputStream fileOut = new BufferedOutputStream(new FileOutputStream(filePath));
             Workbook workbook = "xlsx".equals(extension) ? new SXSSFWorkbook() : new HSSFWorkbook()) {

            Sheet sheet = workbook.createSheet("Data");
            createHeaderRow(fileheader, sheet, workbook);
            populateDataRows(sheet, month, data);
            workbook.write(fileOut);
            log.info("{} 파일이 생성되었습니다. generate_File", extension);
            log.info("------------------------------------------------------------> [ END ]");
        } catch (Exception e) {
            log.info("{} 파일 생성 중 오류가 발생하여 5분 뒤에 재시도 합니다.{}", extension, e.getMessage());
            taskScheduler.schedule(()
                            -> re_generate_File(fileheader, filePath, month, data, extension),
                    PreviousMonthConfig.now_5min);
            log.info(PreviousMonthConfig.now_5min.toString());
        }
    }

    public void re_generate_File(List<String> fileheader, String filePath, String month, List<ExcelColDTO> data, String extension) {
        try (BufferedOutputStream fileOut = new BufferedOutputStream(new FileOutputStream(filePath));
             Workbook workbook = "xlsx".equals(extension) ? new SXSSFWorkbook() : new HSSFWorkbook()) {

            Sheet sheet = workbook.createSheet("Data");
            createHeaderRow(fileheader, sheet, workbook);
            populateDataRows(sheet, month, data);
            workbook.write(fileOut);
            log.info("------------------------------------------------------------> [ END ]");
        } catch (IOException e) {
            log.info("{} 파일 생성 중 오류가 발생했습니다. {}", extension, e.getMessage());
            exceptionSender.sendExceptionAlert("IOException");
        } catch (IllegalArgumentException e) {
            log.warn("전달된 데이터가 유효하지 않습니다: {}", e.getMessage());
            exceptionSender.sendExceptionAlert("IllegalArgumentException");
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
