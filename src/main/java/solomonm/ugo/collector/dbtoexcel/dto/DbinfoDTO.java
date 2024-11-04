package solomonm.ugo.collector.dbtoexcel.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DbinfoDTO {
    private String i_day;
    private String i_d;
    private String road_name;
    private String dir_name;
    private String st_name;
    private String ed_name;
    private Float distance;
    private Double speed;
    private Integer road_idx;
    private Integer link_idx;
    private Integer axis_idx;

}
