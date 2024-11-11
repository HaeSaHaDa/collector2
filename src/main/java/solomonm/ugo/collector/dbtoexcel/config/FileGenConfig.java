package solomonm.ugo.collector.dbtoexcel.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Data
@Validated
@ConfigurationProperties(prefix = "filegen")
@Configuration
public class FileGenConfig {

    /**
     * filegen:
     *   file-path: C:\Users\sixth\Desktop\test
     *   file-name: 연간월평균
     *   file-extension: xls
     *   file-header: MONTH, ROAD_NAME, DIR_NAME, ST_NAME, ED_NAME, DISTANCE, 평일평균, 주말평균, 전체평균
     *   file-regen-time: 5
     */
    private String filePath;
    private String fileName;
    private String fileExtension;
    private List<String> fileHeader;
}
