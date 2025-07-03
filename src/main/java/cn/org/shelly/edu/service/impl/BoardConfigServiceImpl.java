package cn.org.shelly.edu.service.impl;

import cn.org.shelly.edu.mapper.BoardConfigMapper;
import cn.org.shelly.edu.model.pojo.BoardConfig;
import cn.org.shelly.edu.service.BoardConfigService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
* @author Shelly6
* @description 针对表【board_config(棋盘配置表：记录格子总数、特殊格子)】的数据库操作Service实现
* @createDate 2025-07-03 20:58:24
*/
@Service
public class BoardConfigServiceImpl extends ServiceImpl<BoardConfigMapper, BoardConfig>
    implements BoardConfigService {

}




