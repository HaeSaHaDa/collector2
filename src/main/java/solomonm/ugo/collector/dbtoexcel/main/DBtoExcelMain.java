package solomonm.ugo.collector.dbtoexcel.main;


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

    @Value("${filegen.filepath}")
    private String filepath;
    @Value("${filegen.filename}")
    private String filename;
    @Value("${filegen.fileExtension}")
    private String fileExtension;
    @Value("${filegen.fileheader}")
    private List<String> fileheader;

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

        List<ExcelColDTO> dbData;
        dbData = excelInfoService.selectData();

        if (validateData(dbData)) {
            excelInfoService.fileMake(fileheader, dbData, filePath, fileExtension);
        }else{
            log.warn("데이터가 존재하지 않습니다.");
        }

        log.info("------------------------------------------------------------> [ END ]");
    }
}
