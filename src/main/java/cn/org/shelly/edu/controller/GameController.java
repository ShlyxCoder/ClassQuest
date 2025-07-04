package cn.org.shelly.edu.controller;

import cn.org.shelly.edu.common.Result;
import cn.org.shelly.edu.model.pojo.BoardConfig;
import cn.org.shelly.edu.model.req.BoardInitReq;
import cn.org.shelly.edu.model.req.GameInitReq;
import cn.org.shelly.edu.model.resp.TeamUploadResp;
import cn.org.shelly.edu.service.BoardConfigService;
import cn.org.shelly.edu.service.GameService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/game")
@RequiredArgsConstructor
@Tag(name= "游戏管理")
public class GameController {
    private final GameService gameService;
    private final BoardConfigService boardConfigService;
    @PostMapping("/upload")
    @Operation(description = "上传游戏分组导入")
    public Result<TeamUploadResp> init(GameInitReq req){
       return Result.success(gameService.init(req));
    }
    @PostMapping("/board/init")
    @Operation(summary = "初始化棋盘配置")
    public Result<Void> initBoardConfig(@RequestBody @Valid BoardInitReq req) {
        BoardConfig config = new BoardConfig();
        config.setGameId(req.getGameId());
        config.setTotalTiles(req.getTotalTiles());
        config.setBlackSwampTiles(listToStr(req.getBlackSwampTiles()));
        config.setBlindBoxTiles(listToStr(req.getBlindBoxTiles()));
        config.setFortressTiles(listToStr(req.getFortressTiles()));
        config.setGoldCenterTiles(listToStr(req.getGoldCenterTiles()));
        config.setOpportunityTiles(listToStr(req.getOpportunityTiles()));
        boardConfigService.save(config);
        return Result.success();
    }

    private String listToStr(List<Integer> list) {
        return list == null ? null : list.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }


}
