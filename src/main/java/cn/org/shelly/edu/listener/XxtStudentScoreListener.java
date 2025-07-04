package cn.org.shelly.edu.listener;

import cn.org.shelly.edu.exception.ExcelReadStopException;
import cn.org.shelly.edu.model.dto.XxtStudentScoreExcelDTO;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Data
@RequiredArgsConstructor
@Slf4j
public class XxtStudentScoreListener implements ReadListener<XxtStudentScoreExcelDTO> {
    private final List<XxtStudentScoreExcelDTO> dataList = new ArrayList<>();

    @Override
    public void invoke(XxtStudentScoreExcelDTO dto, AnalysisContext context) {
        log.info("解析到一条数据: {}", dto);
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new ExcelReadStopException();
        }
        dataList.add(dto);
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        log.info("Excel 解析完成！");
    }
    public List<XxtStudentScoreExcelDTO> getData() {
        return dataList;
    }
}
