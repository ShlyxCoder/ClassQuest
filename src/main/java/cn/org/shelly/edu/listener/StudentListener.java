package cn.org.shelly.edu.listener;

import cn.org.shelly.edu.exception.CustomException;
import cn.org.shelly.edu.model.dto.StudentExcelDTO;
import cn.org.shelly.edu.service.ClassStudentService;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@RequiredArgsConstructor
public class StudentListener implements ReadListener<StudentExcelDTO> {

    private final ClassStudentService classStudentService;
    private final Long id;

    // 使用原子类保证线程安全
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicInteger failCount = new AtomicInteger(0);

    @Override
    public void invoke(StudentExcelDTO studentExcelDTO, AnalysisContext analysisContext) {
        try {
            boolean result = Boolean.TRUE.equals(classStudentService.createStudent(studentExcelDTO, id));
            if (result) {
                successCount.incrementAndGet();
            } else {
                failCount.incrementAndGet();
            }
        } catch (Exception e) {
            failCount.incrementAndGet();
            log.error("导入失败：{}, 错误信息：{}", studentExcelDTO, e.getMessage());
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        log.info("学生导入完成：成功 {} 条，失败 {} 条", successCount.get(), failCount.get());
        // 可选：如果你需要根据结果抛出异常
        if (successCount.get() == 0) {
            throw new CustomException("全部上传失败");
        }
    }
    public int getSuccessCount() {
        return successCount.get();
    }

    public int getFailCount() {
        return failCount.get();
    }
}
