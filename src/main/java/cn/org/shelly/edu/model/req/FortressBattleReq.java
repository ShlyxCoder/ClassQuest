package cn.org.shelly.edu.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class FortressBattleReq {
    @Schema(description = "游戏ID")
    private Long gameId;
    @Schema(description = "格子ID")
    private Integer tileId;      // 决斗要塞格子ID
    @Schema(description = "发起方小组ID")
    private Long attackerTeamId; // 发起方
    @Schema(description = "被挑战方小组ID")
    private Long defenderTeamId; // 被挑战方
    @Schema(description = "胜利方小组ID")
    private Long winnerTeamId;   // 胜利方ID（系统决定胜者后传）
    @Schema(description = "游戏类型（0=双音节成语, 1=成语抢答）")
    private Integer gameType;    // 0=双音节成语, 1=成语抢答（可校验匹配）
}
