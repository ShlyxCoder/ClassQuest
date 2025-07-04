package cn.org.shelly.edu.service.impl;
import cn.org.shelly.edu.exception.CustomException;
import cn.org.shelly.edu.mapper.TeamMapper;
import cn.org.shelly.edu.model.pojo.Team;
import cn.org.shelly.edu.service.TeamService;
import com.alibaba.excel.EasyExcelFactory;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
* @author Shelly6
* @description 针对表【team(小组表（固定分组，存储姓名和总人数）)】的数据库操作Service实现
* @createDate 2025-07-02 10:22:21
*/
@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
    implements TeamService {

    @Override
    public void downloadTemplate(int memberCount, HttpServletResponse response) {
        if (memberCount < 0) {
            memberCount = 3;
        }
        List<List<String>> headers = new ArrayList<>();
        // 组号
        headers.add(List.of("组号"));
        // 组长及学号
        headers.add(List.of("组长"));
        headers.add(List.of("组长学号"));
        // 队员及学号
        for (int i = 1; i <= memberCount; i++) {
            headers.add(List.of("队员" + i));
            headers.add(List.of("队员" + i + "学号"));
        }
        List<List<String>> emptyData = new ArrayList<>();
        try {
            String fileName = "分组名单上传模板.xlsx";
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8);
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("UTF-8");
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=" + encodedFileName + "; filename*=UTF-8''" + encodedFileName);
            EasyExcelFactory.write(response.getOutputStream())
                    .head(headers)
                    .sheet("分组名单")
                    .doWrite(emptyData);
        } catch (IOException e) {
            throw new CustomException(e);
        }
    }


}




