package solomonm.ugo.collector.dbtoexcel.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Component;
import solomonm.ugo.collector.dbtoexcel.config.PreviousMonthConfig;
import solomonm.ugo.collector.dbtoexcel.dto.ExcelColDTO;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

@Slf4j
@Component
public class ExcelFileGenerator2 {

    /**
     * Excel 파일의 헤더를 작성합니다.
     *
     * @param fileheader 파일 헤더 리스트
     * @param sheet      생성할 시트
     * @param workbook   워크북
     */
    private void createHeaderRow(List<String> fileheader, Sheet sheet, Workbook workbook) {
        Row headerRow = sheet.createRow(0);
        CellStyle cellStyle = createHeaderCellStyle(workbook);
        String header0 = "'" + PreviousMonthConfig.lastMonth_yyyyMM + "'";

        // 첫 번째 셀에 이전 달 정보 추가
        Cell cell0 = headerRow.createCell(0);
        cell0.setCellValue(header0);
        cell0.setCellStyle(cellStyle);

        // 나머지 헤더 셀 추가
        for (int i = 0; i < fileheader.size(); i++) {
            Cell cell = headerRow.createCell(i + 1);
            cell.setCellValue(fileheader.get(i));
            cell.setCellStyle(cellStyle);
        }
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
        CellStyle borderedStyle = createBorderedCellStyle(sheet.getWorkbook()); // 셀 스타일 생성

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
    private void setCellValueWithBorder(Cell cell, Object value, CellStyle cellStyle) {
        setCellValue(cell, value); // 값 설정
        cell.setCellStyle(cellStyle); // 스타일 적용
    }

    /**
     * 셀에 값을 설정합니다.
     *
     * @param cell  설정할 셀
     * @param value 설정할 값
     */
    private void setCellValue(Cell cell, Object value) {
        if (value != null) {
            if (value instanceof Number) {
                cell.setCellValue(((Number) value).doubleValue());
            } else {
                cell.setCellValue(value.toString());
            }
        } else {
            cell.setBlank();
        }
    }

    /**
     * 테두리가 있는 셀 스타일을 생성합니다.
     *
     * @param workbook 워크북
     * @return 테두리 스타일이 적용된 셀 스타일
     */
    private CellStyle createBorderedCellStyle(Workbook workbook) {
        CellStyle cellStyle = workbook.createCellStyle();
        cellStyle.setBorderTop(BorderStyle.THIN);
        cellStyle.setBorderBottom(BorderStyle.THIN);
        cellStyle.setBorderLeft(BorderStyle.THIN);
        cellStyle.setBorderRight(BorderStyle.THIN);
        return cellStyle;
    }

    /**
     * 헤더 셀 스타일을 생성합니다.
     *
     * @param workbook 워크북
     * @return 헤더 셀 스타일
     */
    private CellStyle createHeaderCellStyle(Workbook workbook) {
        CellStyle cellStyle = workbook.createCellStyle();
        Font boldFont = workbook.createFont();
        boldFont.setBold(true);
        boldFont.setFontName("Arial");
        boldFont.setFontHeightInPoints((short) 10);
        cellStyle.setFont(boldFont);
        cellStyle.setBorderTop(BorderStyle.THIN);
        cellStyle.setBorderBottom(BorderStyle.THIN);
        cellStyle.setBorderLeft(BorderStyle.THIN);
        cellStyle.setBorderRight(BorderStyle.THIN);
        return cellStyle;
    }

    /**
     * xlsx 파일을 생성합니다.
     *
     * @param fileheader 파일 헤더 리스트
     * @param filePath   생성할 파일 경로
     * @param month      월 정보
     * @param data       데이터 리스트
     */
    public void generate_XLSX_File(List<String> fileheader, String filePath, String month, List<ExcelColDTO> data) {
        try (Workbook workbook = new SXSSFWorkbook();
             BufferedOutputStream fileOut = new BufferedOutputStream(new FileOutputStream(filePath))) {

            Sheet sheet = workbook.createSheet("DataSheet");
            createHeaderRow(fileheader, sheet, workbook);
            populateDataRows(sheet, month, data);
            workbook.write(fileOut);

            log.info("xlsx 파일이 생성되었습니다.");
        } catch (IOException e) {
            log.info("xlsx 파일 생성 중 오류가 발생했습니다. {}", e);
            throw new RuntimeException(e);
        } catch (IllegalArgumentException e) {
            log.warn("전달된 데이터가 유효하지 않습니다: {}", e.getMessage());
            throw e; // 예외를 다시 던져 호출자에게 알림
        }
    }

    /**
     * xls 파일을 생성합니다.
     *
     * @param fileheader 파일 헤더 리스트
     * @param filePath   생성할 파일 경로
     * @param month      월 정보
     * @param data       데이터 리스트
     */
    public void generate_XLS_File(List<String> fileheader, String filePath, String month, List<ExcelColDTO> data) {
        try (Workbook workbook = new HSSFWorkbook();
             BufferedOutputStream fileOut = new BufferedOutputStream(new FileOutputStream(filePath))) {

            Sheet sheet = workbook.createSheet("DataSheet");
            createHeaderRow(fileheader, sheet, workbook);
            populateDataRows(sheet, month, data);
            workbook.write(fileOut);

            log.info("xls 파일이 생성되었습니다.");
        } catch (IOException e) {
            throw new RuntimeException("xls 파일 생성 중 오류가 발생했습니다.", e);
        } catch (IllegalArgumentException e) {
            log.warn("전달된 데이터가 유효하지 않습니다: {}", e.getMessage());
            throw e; // 예외를 다시 던져 호출자에게 알림
        }
    }
}
