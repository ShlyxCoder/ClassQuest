package cn.org.shelly.edu.handler;

import com.alibaba.excel.write.handler.SheetWriteHandler;
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
import com.alibaba.excel.write.metadata.holder.WriteWorkbookHolder;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.xssf.streaming.SXSSFSheet;

/**
 * 自定义工作表写入处理程序
 *
 * @author shelly
 * @date 2024/03/15
 */
public class CustomSheetWriteHandler implements SheetWriteHandler {

    // 设置100列column
    private static final Integer COLUMN = 100;

    @Override
    public void beforeSheetCreate(WriteWorkbookHolder writeWorkbookHolder, WriteSheetHolder writeSheetHolder) {
        // do nothing
    }

    @Override
    public void afterSheetCreate(WriteWorkbookHolder writeWorkbookHolder, WriteSheetHolder writeSheetHolder) {
        for (int i = 0; i < COLUMN; i++) {
            // 设置为文本格式
            SXSSFSheet sxssfSheet = (SXSSFSheet) writeSheetHolder.getSheet();
            CellStyle cellStyle = writeWorkbookHolder.getCachedWorkbook().createCellStyle();
            // 设置文本居中显示
            cellStyle.setAlignment(HorizontalAlignment.CENTER);
            // 49为文本格式
            cellStyle.setDataFormat((short) 49);
            // i为列，一整列设置为文本格式
            sxssfSheet.setDefaultColumnStyle(i, cellStyle);
        }
    }
}

