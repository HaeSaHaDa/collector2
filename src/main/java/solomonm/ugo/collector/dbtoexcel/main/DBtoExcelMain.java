package solomonm.ugo.collector.dbtoexcel.main;


import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import solomonm.ugo.collector.dbtoexcel.services.ExcelInfoService;

@Slf4j
@Component
public class DBtoExcelMain implements ApplicationRunner {
    private final ExcelInfoService excelInfoService;
    public DBtoExcelMain(ExcelInfoService excelInfoService) {
        this.excelInfoService = excelInfoService;
    }

    @Scheduled(cron = "${filegen.filegen-cron}")
    public void start() {
        excelInfoService.fileMake();
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
    }
}
