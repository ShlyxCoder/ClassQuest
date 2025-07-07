package cn.org.shelly.edu.controller;

import cn.org.shelly.edu.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name= "提案赛管理")
@RequiredArgsConstructor
@RequestMapping("/proposal")
public class ProposalController {

    @GetMapping("/pre/rank")
    @Operation(summary = "获取棋盘赛结算排名")
    public Result<?> getPreRank(){
        return Result.success();
    }
    @PostMapping("/init")
    @Operation(summary = "初始化提案赛")
    public Result<?> init(){
        return Result.success();
    }
    @PostMapping("/order")
    @Operation(summary = "上传提案赛选择排序")
    public Result<?> uploadOrder(){
        return Result.success();
    }
    @PostMapping("/upload")
    @Operation(summary = "每轮上传提案")
    public Result<?> upload(){
        return Result.success();
    }
    @PostMapping("/vote")
    @Operation(summary = "上传提案赛投票")
    public Result<?> uploadVote(){
        return Result.success();
    }


}
