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
import java.io.File;
import java.io.FileOutputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@EnableScheduling
public class ExcelFileGenerator {
    private final ExceptionSender exceptionSender;
    private final TaskScheduler taskScheduler;
    private boolean retry = true; // 재시도 플래그를 처음에는 true로 설정


    public ExcelFileGenerator(ExceptionSender exceptionSender, TaskScheduler taskScheduler) {
        this.exceptionSender = exceptionSender;
        this.taskScheduler = taskScheduler;
    }

    /**
     * 주어진 데이터를 기반으로 Excel 파일을 생성하는 메서드입니다.
     * 넘겨받은 extension에 따라 Workbook() 객체 생성을 달리합니다.(xlsx: SXSSFWorkbook/ xls,cell: HSSFWorkbook)
     * 실패 시 다시 한 번 재시도한 후 생성이 불가능하면 exception에 대해 메일을 발송합니다.
     *
     * @param fileHeader Excel 파일의 헤더 목록
     * @param filePath   생성할 파일의 경로
     * @param fileName   생성할 파일의 이름
     * @param month      데이터의 기준이 되는 월 정보
     * @param dbData     파일에 기록할 데이터 리스트
     * @param extension  파일의 확장자 ("xlsx" 또는 "xls" 또는 "cell")
     */
    public void generateFile(
            List<String> fileHeader,
            String filePath,
            String fileName,
            String month,
            List<ExcelColDTO> dbData,
            String extension,
            int fileRegenTime
    ) {
        try (BufferedOutputStream fileOut = new BufferedOutputStream(new FileOutputStream(filePath));
             Workbook workbook = "xlsx".equals(extension) ? new SXSSFWorkbook() : new HSSFWorkbook()) {

            // 시트 및 헤더 생성
            Sheet sheet = workbook.createSheet("Data");
            int rowIndex = 0;

            // 헤더와 데이터를 동시에 생성하여 통합된 셀 스타일 적용
            CellStyle headerStyle = createCellStyle(workbook, true);  // 헤더 스타일 생성
            CellStyle dataStyle = createCellStyle(workbook, false);  // 데이터 스타일 생성

            Row headerRow = sheet.createRow(rowIndex++);
            for (int i = 0; i < fileHeader.size(); i++) {
                Cell cell = headerRow.createCell(i);

                // 첫 번째 셀에는 특정 날짜 정보 삽입
                cell.setCellValue(i == 0 ? "'" + PreviousMonthConfig.lastMonth_yyyyMM + "'" : fileHeader.get(i));
                // 헤더 스타일 적용
                cell.setCellStyle(headerStyle);
            }

            // 데이터 행 생성 및 각 셀에 데이터와 스타일 적용
            for (ExcelColDTO dto : dbData) {
                Row dataRow = sheet.createRow(rowIndex++);
                int colIndex = 0;

                // 데이터 값들을 셀에 삽입
                for (Object value :
                        Arrays.asList(
                                month, dto.getRoad_name(), dto.getDir_name(), dto.getSt_name(), dto.getEd_name(),
                                dto.getDistance(), dto.getWeekDay_avg(), dto.getWeekEnd_avg(), dto.getAll_avg())
                ) {
                    Cell cell = dataRow.createCell(colIndex++);

                    // 데이터 값에 따라 셀 값 설정
                    if (value instanceof Number) {
                        cell.setCellValue(((Number) value).doubleValue());
                    } else {
                        cell.setCellValue(value == null ? "" : value.toString());
                    }
                    cell.setCellStyle(dataStyle);
                }
            }

            // 파일 저장
            workbook.write(fileOut);

            log.info("{} 파일이 생성되었습니다. ", fileName);
            log.info("------------------------------------------------------------> [ END ]");

        } catch (Exception e) {

            Instant fileRegenTimeInst = Instant.now().plusSeconds(fileRegenTime);
            ZonedDateTime localDateTime = fileRegenTimeInst.atZone(ZoneId.systemDefault());

            // 재시도 플래그 설정하여 한 번만 재시도 예약
            if (retry) {
                retry = false;  // 재시도 수행이 되지 않게 설정
                log.error("{} 파일 생성 중 오류가 발생하여 5분 뒤에 재시도 합니다. 오류: {}", fileName, e.getMessage());
                log.info("다음 재시도 시각: {}", localDateTime);
                // 재시도 예약
                taskScheduler.schedule(
                        () -> generateFile(fileHeader, filePath, fileName, month, dbData, extension, fileRegenTime),
                        fileRegenTimeInst // 5분 후 재시도
                );

            } else {
                // 재시도 후에도 실패 시 예외를 메일로 전송
                String title = fileName + " 파일 생성 실패";
                String content = String.format("%s%s%s \n -> %s",
                        "다음과 같은 이유로 [ ",
                        fileName,
                        " ] 파일 생성에 실패했습니다.",
                        e.getMessage()
                );
                exceptionSender.exceptionSender(title, content);
            }

        }
    }

    /**
     * 셀 스타일을 생성합니다.
     *
     * @param workbook 엑셀 워크북 객체
     * @param isHeader 헤더 스타일 여부
     * @return 설정된 셀 스타일
     */
    private CellStyle createCellStyle(Workbook workbook, boolean isHeader) {
        CellStyle style = workbook.createCellStyle();
        if (isHeader) {
            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            boldFont.setFontName("Arial");
            boldFont.setFontHeightInPoints((short) 10);
            style.setFont(boldFont);
        }
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }
}
