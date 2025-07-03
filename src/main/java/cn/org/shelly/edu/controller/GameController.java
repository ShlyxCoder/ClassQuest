package cn.org.shelly.edu.controller;

import cn.org.shelly.edu.model.req.GameInitReq;
import cn.org.shelly.edu.service.GameService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/game")
@RequiredArgsConstructor
@Tag(name= "游戏管理")
public class GameController {
    private final GameService gameService;
//    @PostMapping("/upload")
//    @Operation(description = "初始化游戏")
//    public Result<> init(@RequestBody GameInitReq req){
//        gameService.init(req);
//
//    }
}
