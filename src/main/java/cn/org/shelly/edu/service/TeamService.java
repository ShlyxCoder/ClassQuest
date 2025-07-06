package cn.org.shelly.edu.service;
import cn.org.shelly.edu.model.pojo.Team;
import cn.org.shelly.edu.model.req.ScoreUpdateReq;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

/**
* @author Shelly6
* @description 针对表【team(小组表（固定分组，存储姓名和总人数）)】的数据库操作Service
* @createDate 2025-07-02 10:22:21
*/
public interface TeamService extends IService<Team> {

    void downloadTemplate(int memberCount, HttpServletResponse response);

}
