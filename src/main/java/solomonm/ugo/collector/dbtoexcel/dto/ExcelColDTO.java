package solomonm.ugo.collector.dbtoexcel.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExcelColDTO {
    private String year_month;
    private String road_name;
    private String dir_name;
    private String st_name;
    private String ed_name;
    private Float distance;
    private Double weekDay_avg;
    private Double weekEnd_avg;
    private Double all_avg;


}
