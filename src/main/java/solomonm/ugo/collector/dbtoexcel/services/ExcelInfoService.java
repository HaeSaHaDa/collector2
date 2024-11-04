package solomonm.ugo.collector.dbtoexcel.services;

import solomonm.ugo.collector.dbtoexcel.dto.ExcelColDTO;

import java.util.List;

public interface ExcelInfoService {

    List<ExcelColDTO> selectData();

    void fileMake(List<String> fileheader, List<ExcelColDTO> dbData, String filePath, String extension);

}
