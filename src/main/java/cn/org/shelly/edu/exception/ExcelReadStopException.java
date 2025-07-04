package cn.org.shelly.edu.exception;

public class ExcelReadStopException extends RuntimeException {
    public ExcelReadStopException() {
        super("中断 Excel 读取流程：学生姓名为空");
    }
}
