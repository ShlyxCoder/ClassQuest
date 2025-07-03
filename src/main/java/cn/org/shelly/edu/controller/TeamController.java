package cn.org.shelly.edu.controller;

import cn.org.shelly.edu.exception.CustomException;
import cn.org.shelly.edu.service.TeamService;
import cn.org.shelly.edu.utils.ExcelUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.EasyExcelFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/team")
@RequiredArgsConstructor
@Tag(name= "小组管理")
public class TeamController {

    private final TeamService teamService;
    @GetMapping("/template")
    @Operation(summary = "获取小组上传excel模板")
    public void downloadTemplate(
            @Schema(description = "小组最大人数")
            @RequestParam(name = "memberCount", required = false, defaultValue = "3") int memberCount,
            HttpServletResponse response) {
        if (memberCount < 0) {
            memberCount = 3;
        }
        List<List<String>> headers = new ArrayList<>();
        headers.add(List.of("组号"));
        headers.add(List.of("组长"));
        for (int i = 1; i <= memberCount; i++) {
            headers.add(List.of("队员" + i));
        }
        String fileName = URLEncoder.encode("分组名单上传模板.xlsx", StandardCharsets.UTF_8);
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename*=UTF-8''" + fileName);
        List<List<String>> emptyData = new ArrayList<>();
        try {
            EasyExcelFactory.write(response.getOutputStream())
                    .head(headers)
                    .sheet("分组名单")
                    .doWrite(emptyData);
        } catch (IOException e) {
            throw new CustomException(e);
        }
    }
}
