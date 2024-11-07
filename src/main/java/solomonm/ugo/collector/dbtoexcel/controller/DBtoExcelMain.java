package solomonm.ugo.collector.dbtoexcel.controller;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import solomonm.ugo.collector.dbtoexcel.dto.ExcelColDTO;
import solomonm.ugo.collector.dbtoexcel.services.ExcelInfoService;
import solomonm.ugo.collector.dbtoexcel.config.PreviousMonthConfig;

import java.io.File;
import java.util.List;

@Slf4j
@Component
@PropertySource(value = "classpath:application.yml", encoding = "UTF-8")
public class DBtoExcelMain implements ApplicationRunner {

    private final ExcelInfoService excelInfoService;

    @Value("${filegen.file-path}")
    String filepath;
    @Value("${filegen.file-name}")
    String filename;
    @Value("${filegen.file-extension}")
    String fileExtension;
    @Value("${filegen.file-header}")
    List<String> fileheader;

    public DBtoExcelMain(ExcelInfoService excelInfoService) {
        this.excelInfoService = excelInfoService;
    }

    private String prepareFilePath() {
        return String.format("%s%s%s%s월.%s",
                filepath,
                File.separator,
                filename,
                PreviousMonthConfig.lastMonth_MM,
                fileExtension);
    }

    private boolean validateData(List<ExcelColDTO> dbData) {
        if (dbData.isEmpty()) {
            log.warn("로드된 데이터가 없습니다");
            return false;
        } else {
            log.info("데이터가 성공적으로 로드되었습니다. 로드된 데이터 개수: {}", dbData.size());
            return true;
        }
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("---------------------------------------------------------> [ START ]");

        String filePath = prepareFilePath();
        String fileName = filename + PreviousMonthConfig.lastMonth_MM + "월." + fileExtension;
        
        log.info(fileName + "이세요");

        List<ExcelColDTO> dbData;
        dbData = excelInfoService.selectData();

        if (validateData(dbData)) {
            excelInfoService.fileMake(fileheader, dbData, filePath, fileName, fileExtension);
        }else{
            log.warn("데이터가 존재하지 않습니다.");
        }

    }
}
