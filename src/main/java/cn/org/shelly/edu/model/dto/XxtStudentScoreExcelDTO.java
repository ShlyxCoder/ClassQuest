package cn.org.shelly.edu.model.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class XxtStudentScoreExcelDTO {
    @ExcelProperty("学生姓名")
    private String name;
    @ExcelProperty("学号/工号")
    private String sno;
    @ExcelProperty("获得积分")
    private String score;
    @ExcelProperty("提交时间")
    private LocalDateTime time;
}
