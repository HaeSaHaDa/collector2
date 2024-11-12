package solomonm.ugo.collector.dbtoexcel.services.imple;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import solomonm.ugo.collector.dbtoexcel.config.FileGenConfig;
import solomonm.ugo.collector.dbtoexcel.config.PreviousMonthConfig;
import solomonm.ugo.collector.dbtoexcel.dto.ExcelColDTO;
import solomonm.ugo.collector.dbtoexcel.mapper.ExcelInfoMapper;
import solomonm.ugo.collector.dbtoexcel.services.ExcelInfoService;
import solomonm.ugo.collector.dbtoexcel.util.ExcelFileGenerator;

import java.io.File;
import java.util.List;


@Slf4j
@Service
public class ExcelinfoServiceImpl implements ExcelInfoService {

    private final ExcelInfoMapper excelInfoMapper;
    private final ExcelFileGenerator excelFileGenerator;
    private final FileGenConfig fileGenConfig;

    private String filePath;
    private String fileName;
    private String fileExtension;
    private List<String> fileheader;
    private int fileRegenTime;


    /**
     * 생성자 - 설정 파일의 설정값들을 인스턴스 변수에 할당
     *
     * @param excelInfoMapper    데이터베이스에서 데이터를 수집하는 Mapper
     * @param excelFileGenerator Excel 파일을 생성하는 클래스
     * @param fileGenConfig      파일 경로, 이름 등의 설정값을 담고 있는 설정 객체
     */
    public ExcelinfoServiceImpl(ExcelInfoMapper excelInfoMapper, ExcelFileGenerator excelFileGenerator, FileGenConfig fileGenConfig) {
        this.excelInfoMapper = excelInfoMapper;
        this.excelFileGenerator = excelFileGenerator;
        this.fileGenConfig = fileGenConfig;

        // 설정값을 인스턴스 변수에 저장
        filePath = fileGenConfig.getFilePath();
        fileName = fileGenConfig.getFileName();
        fileExtension = fileGenConfig.getFileExtension();
        fileheader = fileGenConfig.getFileHeader();
        fileRegenTime = fileGenConfig.getFileRegenTime();
    }

    /**
     * 데이터베이스에서 데이터를 수집하는 메서드
     *
     * @return 수집된 데이터 목록
     */
    @Override
    public List<ExcelColDTO> selectData() {
        log.info("db로부터 데이터 수집.");

        return excelInfoMapper.selectData(
                PreviousMonthConfig.lastMonth_yyyyMM,
                PreviousMonthConfig.firstDay_yyyyMMdd,
                PreviousMonthConfig.lastDay_yyyyMMdd
        );
    }

    /**
     * Excel 파일 생성 메서드
     * - 데이터 유효성 검사를 수행하고 유효한 경우 파일 생성
     */
    @Transactional
    @Override
    public void fileMake() {
        try {
            log.info("---------------------------------------------------------> [ START ]");

            // 파일 이름 및 경로 설정
            String filename = fileName + PreviousMonthConfig.lastMonth_MM + "월." + fileExtension;
            String filepath = String.format("%s%s%s",
                    filePath,
                    File.separator,
                    filename
                    );
            log.info("파일 생성 경로: {}", filepath);

            // 데이터베이스에서 데이터 로드
            List<ExcelColDTO> dbData = selectData();

            // 데이터 유효성 검사 후 파일 생성 메서드 호출
            if (!dbData.isEmpty()) {
                log.info("데이터가 성공적으로 로드되었습니다. 로드된 데이터 개수: {}", dbData.size());
                excelFileGenerator.generateFile(
                        fileheader,
                        filepath,
                        filename,
                        PreviousMonthConfig.lastMonth_yyyyMM,
                        dbData,
                        fileExtension,
                        fileRegenTime
                );
            } else {
                log.warn("데이터가 존재하지 않습니다.");
            }

        } catch (
                Exception e) {
            log.error("파일 생성 중 오류 발생: {}", e.getMessage());
        }
    }
}
