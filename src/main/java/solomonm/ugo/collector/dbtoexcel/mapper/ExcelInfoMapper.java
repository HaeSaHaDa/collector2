package solomonm.ugo.collector.dbtoexcel.mapper;

import org.apache.ibatis.annotations.Mapper;
import solomonm.ugo.collector.dbtoexcel.dto.ExcelColDTO;

import java.util.List;

@Mapper
public interface ExcelInfoMapper {
    List<ExcelColDTO> selectData(
            String lastMonth_yyyyMM,
            String firstDay_yyyyMMdd,
            String lastDay_yyyyMMdd
    );
}
