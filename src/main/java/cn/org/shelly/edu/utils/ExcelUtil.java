package cn.org.shelly.edu.utils;

import cn.org.shelly.edu.enums.CodeEnum;
import cn.org.shelly.edu.exception.CustomException;
import cn.org.shelly.edu.handler.CustomSheetWriteHandler;
import com.alibaba.excel.EasyExcelFactory;
import com.alibaba.excel.write.builder.ExcelWriterBuilder;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;

/**
 * Excel 工具类
 *
 * @author shelly
 * @date 2024/03/14
 */
@SuppressWarnings("rawtypes")
@Slf4j
public class ExcelUtil {

    private ExcelUtil(){

    }
    /**
     * 写出 Excel
     *
     * @param response 响应
     * @param name     名字
     * @param clazz    克拉兹
     * @param secures  保证
     * @param config   配置
     */
    public static void write(HttpServletResponse response, String name, Class<?> clazz, List secures, Consumer<ExcelWriterBuilder> config) {
        try {
            var write = getExcelWriterBuilder(response, name, EasyExcelFactory.write(response.getOutputStream(), clazz));
            config.accept(write);
            write.autoCloseStream(Boolean.FALSE).sheet(name).doWrite(secures);
        } catch (Exception e) {
            throw new CustomException(CodeEnum.EXPORT_FAILED);
        }
    }

    private static ExcelWriterBuilder getExcelWriterBuilder(HttpServletResponse response, String name, ExcelWriterBuilder response1) {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        // 这里URLEncoder.encode可以防止中文乱码 当然和easyexcel没有关系
        String fileName = URLEncoder.encode(name, StandardCharsets.UTF_8).replace("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=" + fileName + ".xlsx");
        // 这里需要设置不关闭流
        return response1.registerWriteHandler(new CustomSheetWriteHandler());
    }

    public static void write(HttpServletResponse response, String name, Class<?> clazz, List secures) {
        try {
            var write = getExcelWriterBuilder(response, name, EasyExcelFactory.write(response.getOutputStream(), clazz));
            write.autoCloseStream(Boolean.FALSE).sheet(name).doWrite(secures);
        } catch (Exception e) {
            log.error("写出 Excel 异常: {}", e.getMessage(), e);
            throw new CustomException(CodeEnum.EXPORT_FAILED);
        }
    }

}
