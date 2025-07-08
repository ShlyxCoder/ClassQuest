package cn.org.shelly.edu.mapper;
import cn.org.shelly.edu.model.pojo.Team;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.ArrayList;
import java.util.List;

/**
* @author Shelly6
* @description 针对表【team(小组表（固定分组，存储姓名和总人数）)】的数据库操作Mapper
* @createDate 2025-07-02 10:22:21
* @Entity cn/org/shelly/edu/model/pojo.domain.Team
*/
public interface TeamMapper extends BaseMapper<Team> {

    int updateProposalScoreByCompositeKey(Team team);


    int updateAliveByCompositeKey(Team team);

}




