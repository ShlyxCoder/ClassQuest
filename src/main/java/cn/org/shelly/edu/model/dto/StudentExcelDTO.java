package cn.org.shelly.edu.model.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode
public class StudentExcelDTO {

    /**
     * 学生姓名
     */
    @ExcelProperty("学生姓名")
    @ColumnWidth(12)
    private String name;

    /**
     * 学号
     */
    @ExcelProperty("学号")
    @ColumnWidth(12)
    private String sno;

}
