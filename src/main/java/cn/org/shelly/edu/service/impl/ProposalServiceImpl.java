package cn.org.shelly.edu.service.impl;

import cn.org.shelly.edu.exception.CustomException;
import cn.org.shelly.edu.mapper.ProposalMapper;
import cn.org.shelly.edu.mapper.TeamMapper;
import cn.org.shelly.edu.mapper.TeamMemberMapper;
import cn.org.shelly.edu.model.dto.ScoreUpdateDTO;
import cn.org.shelly.edu.model.dto.XxtStudentScoreExcelDTO;
import cn.org.shelly.edu.model.pojo.*;
import cn.org.shelly.edu.model.req.*;
import cn.org.shelly.edu.model.resp.*;
import cn.org.shelly.edu.service.*;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;



import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
* @author Shelly6
* @description 针对表【proposal(提案表，存储每轮小组提案信息)】的数据库操作Service实现
* @createDate 2025-07-07 15:03:14
*/
@Service
@Slf4j
@RequiredArgsConstructor
public class ProposalServiceImpl extends ServiceImpl<ProposalMapper, Proposal>
    implements ProposalService {
    private final GameService gameService;
    private final TeamService teamService;
    private final TeamMemberMapper teamMemberMapper;
    private final ProposalRoundTeamScoreService proposalRoundTeamScoreService;
    private final TeamMapper teamMapper;
    @Resource
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;
    private final TeamMemberService teamMemberService;
    private final StudentScoreLogService studentScoreLogService;
    @Override
    public ProposalInitResp init(List<ProposalInitReq> req) {
        if (req == null || req.isEmpty()) {
            throw new CustomException("请求数据为空");
        }
        Long gameId = req.get(0).getGameId();
        Game game = gameService.getById(gameId);
        if (game == null || game.getStage() != 2 || game.getProposalStage() != 0) {
            throw new CustomException("游戏状态异常，无法导入提案积分");
        }
        List<Team> teams = teamService.lambdaQuery()
                .eq(Team::getGameId, gameId)
                .ne(Team::getAlive,2)
                .list();
        if (teams == null || teams.size() != req.size()) {
            throw new CustomException("小组数量异常，无法初始化");
        }
        // 构造 teamId -> Team 映射
        Map<Long, Team> teamMap = teams.stream()
                .collect(Collectors.toMap(Team::getId, t -> t));

        for (ProposalInitReq item : req) {
            Team team = teamMap.get(item.getTeamId());
            if (team != null) {
                team.setProposalScoreImported(item.getInitialScore());
            } else {
                throw new CustomException("找不到小组ID：" + item.getTeamId());
            }
        }
        log.info(teamMap.values().toString());
        for (Team team : teamMap.values()) {
            teamMapper.updateProposalScoreByCompositeKey(team);
        }

        // 标记提案赛进入下一阶段
        game.setLastSavedAt(new Date());
        game.setProposalStage(1);
        boolean gameUpdated = gameService.updateById(game);
        if (!gameUpdated) {
            throw new CustomException("游戏状态更新失败");
        }
        ProposalInitResp resp = new ProposalInitResp();
        resp.setRound(game.getProposalRound());
        int totalTeams = teams.size();
        int base = totalTeams / 3;
        int remainder = totalTeams % 3;

        List<Integer> roundTimes = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            roundTimes.add(base + (i < remainder ? 1 : 0));
        }
        resp.setRoundTimes(roundTimes);
        return resp;
    }

    @Override
    @Transactional
    public void uploadOrder(ProposalOrderReq req) {
        Long gameId = req.getGameId();
        List<List<Long>> roundTeamIds = req.getRoundTeamIds();
        if (gameId == null || roundTeamIds == null || roundTeamIds.size() != 3) {
            throw new CustomException("请求数据格式不正确，应为3轮分组");
        }
        // 获取游戏及小组信息
        Game game = gameService.getById(gameId);
        if (game == null || game.getStage() != 2 || game.getProposalStage() != 1) {
            throw new CustomException("游戏状态异常");
        }

        List<Team> teams = teamService.lambdaQuery()
                .eq(Team::getGameId, gameId)
                .ne(Team::getAlive,2)
                .list();
        Set<Long> allTeamIds = teams.stream().map(Team::getId).collect(Collectors.toSet());
        // 合并全部上传的小组 ID
        Set<Long> uploadedTeamIds = roundTeamIds.stream()
                .flatMap(List::stream)
                .collect(Collectors.toSet());
        // 校验小组数量是否匹配
        if (uploadedTeamIds.size() != allTeamIds.size()) {
            throw new CustomException("上传小组数量不匹配，应为：" + allTeamIds.size());
        }
        // 校验是否有非法小组 ID
        for (Long id : uploadedTeamIds) {
            if (!allTeamIds.contains(id)) {
                throw new CustomException("包含非法小组ID: " + id);
            }
        }
        // 校验是否有重复
        long totalUploadedCount = roundTeamIds.stream().mapToLong(List::size).sum();
        if (totalUploadedCount != uploadedTeamIds.size()) {
            throw new CustomException("存在重复小组ID，请检查");
        }
        List<Proposal> proposals = new ArrayList<>();
        for (int i = 0; i < roundTeamIds.size(); i++) {
            int round = i + 1;
            List<Long> teamIds = roundTeamIds.get(i);
            for (Long teamId : teamIds) {
                Proposal p = new Proposal();
                p.setRound(round);
                p.setProposerTeamId(teamId);
                p.setElectedScore(0);
                p.setGameId(gameId);
                p.setSelected(0);
                proposals.add(p);
            }
        }
        boolean inserted = saveBatch(proposals);
        if (!inserted) {
            throw new CustomException("提案初始化失败");
        }
        game.setProposalStage(2);
        game.setProposalRound(1);
        game.setLastSavedAt(new Date());
        gameService.updateById(game);
    }

    @Override
    public List<List<Long>> getProposalOrder(Long gameId) {
        Game game = gameService.getById(gameId);
        if (game == null || game.getStage() != 2) {
            throw new CustomException("游戏状态异常");
        }
        List<Proposal> proposals = lambdaQuery()
                .eq(Proposal::getGameId, gameId)
                .orderByAsc(Proposal::getRound)
                .list();
        Map<Integer, List<Long>> roundMap = new TreeMap<>();
        for (Proposal p : proposals) {
            roundMap.computeIfAbsent(p.getRound(), k -> new ArrayList<>())
                    .add(p.getProposerTeamId());
        }
        return new ArrayList<>(roundMap.values());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void uploadFirst(ProposalFirstSubmitReq req) {
        Long gameId = req.getGameId();
        Game game = gameService.getById(gameId);
        if (game == null || game.getStage() != 2 || game.getProposalStage() != 2 || game.getProposalRound() != 1) {
            throw new CustomException("游戏状态异常");
        }
        Integer requiredTeamCount = req.getNum();
        List<ProposalFirstSubmitReq.FirstSingleProposal> proposals = req.getProposals();
        if (gameId == null || requiredTeamCount == null || proposals == null || proposals.isEmpty()) {
            throw new CustomException("请求参数不完整");
        }
        List<ProposalFirstSubmitReq.FirstSingleProposal> list = req.getProposals();
        for(ProposalFirstSubmitReq.FirstSingleProposal p : list){
            if(!p.getInvolvedTeamIds().contains(p.getProposerTeamId())){
                throw new CustomException("提案小组ID " + p.getProposerTeamId() + " 不在参与小组中");
            }
        }
        int round = game.getProposalRound();
        for (int i = 0; i < proposals.size(); i++) {
            ProposalFirstSubmitReq.FirstSingleProposal p = proposals.get(i);
            List<Long> involvedTeamIds = p.getInvolvedTeamIds();
            List<Integer> scoreList = p.getScoreDistribution();

            if (involvedTeamIds == null || involvedTeamIds.size() != requiredTeamCount) {
                throw new CustomException("第 " + (i + 1) + " 个提案的参与小组数量不正确，应为 " + requiredTeamCount);
            }
            if (scoreList == null || scoreList.size() != requiredTeamCount) {
                throw new CustomException("第 " + (i + 1) + " 个提案的积分分配数量不正确，应为 " + requiredTeamCount);
            }
            int sum = scoreList.stream().mapToInt(Integer::intValue).sum();
            if (sum != 28) {
                throw new CustomException("第 " + (i + 1) + " 个提案的积分分配总和不是 28，而是 " + sum);
            }
        }
        List<Proposal> existingList = lambdaQuery()
                .eq(Proposal::getGameId, gameId)
                .eq(Proposal::getRound, round)
                .list();
        Map<Long, Proposal> existingMap = existingList.stream()
                .collect(Collectors.toMap(Proposal::getProposerTeamId, Function.identity()));
        Date now = new Date();
        List<Proposal> updateList = new ArrayList<>();
        for (ProposalFirstSubmitReq.FirstSingleProposal p : proposals) {
            Proposal existing = existingMap.get(p.getProposerTeamId());
            if (existing == null) {
                throw new CustomException("未找到 proposerTeamId 为 " + p.getProposerTeamId() + " 的提案记录");
            }
            existing.setInvolvedTeams(p.getInvolvedTeamIds().stream().map(String::valueOf).collect(Collectors.joining(",")));
            existing.setScoreDistribution(p.getScoreDistribution().stream().map(String::valueOf).collect(Collectors.joining(",")));
            existing.setUpdateTime(now);
            updateList.add(existing);
        }
        this.updateBatchById(updateList);
        game.setProposalStage(3);
        game.setLastSavedAt(new Date());
        gameService.updateById(game);
    }



    @Override
    @Transactional(rollbackFor = Exception.class)
    public void uploadSecond(ProposalSecondSubmitReq req) {
        Long gameId = req.getGameId();
        Game game = gameService.getById(gameId);
        if (game == null || game.getStage() != 2 || game.getProposalStage() != 2 || game.getProposalRound() != 2) {
            throw new CustomException("游戏状态异常");
        }
        Integer requiredTeamCount = req.getNum();
        List<ProposalSecondSubmitReq.SecondSingleProposal> proposals = req.getProposals();
        if (gameId == null || requiredTeamCount == null || proposals == null || proposals.isEmpty()) {
            throw new CustomException("请求参数不完整");
        }
        List<ProposalSecondSubmitReq.SecondSingleProposal> list = req.getProposals();
        for(ProposalSecondSubmitReq.SecondSingleProposal p : list){
            if(!p.getProTeamIds().contains(p.getProposerTeamId()) && !p.getConTeamIds().contains(p.getProposerTeamId())){
                throw new CustomException("提案小组ID " + p.getProposerTeamId() + " 不在参与小组中");
            }
        }
        int round = game.getProposalRound();
        // 校验每个提案
        for (int i = 0; i < proposals.size(); i++) {
            ProposalSecondSubmitReq.SecondSingleProposal p = proposals.get(i);
            List<Long> proTeamIds = p.getProTeamIds();
            List<Long> conTeamIds = p.getConTeamIds();
            if (proTeamIds == null || conTeamIds == null) {
                throw new CustomException("第 " + (i + 1) + " 个提案正反方小组不能为空");
            }
            if (proTeamIds.size() != conTeamIds.size()) {
                throw new CustomException("第 " + (i + 1) + " 个提案正反方小组数量不一致");
            }
            if (proTeamIds.size() != requiredTeamCount) {
                throw new CustomException("第 " + (i + 1) + " 个提案正反方小组数量应为 " + requiredTeamCount);
            }
        }
        // 查询当前轮次提案
        List<Proposal> existingList = lambdaQuery()
                .eq(Proposal::getGameId, gameId)
                .eq(Proposal::getRound, round)
                .list();
        Map<Long, Proposal> existingMap = existingList.stream()
                .collect(Collectors.toMap(Proposal::getProposerTeamId, Function.identity()));
        List<Proposal> updateList = new ArrayList<>();
        for (ProposalSecondSubmitReq.SecondSingleProposal p : proposals) {
            Proposal existing = existingMap.get(p.getProposerTeamId());
            if (existing == null) {
                throw new CustomException("未找到 proposerTeamId 为 " + p.getProposerTeamId() + " 的提案记录");
            }
            // 合并正反方为一个列表，顺序不能变
            List<String> allInvolved = new ArrayList<>();
            p.getProTeamIds().forEach(id -> allInvolved.add(String.valueOf(id)));
            p.getConTeamIds().forEach(id -> allInvolved.add(String.valueOf(id)));
            existing.setInvolvedTeams(String.join(",", allInvolved));
            updateList.add(existing);
        }
        this.updateBatchById(updateList);
        game.setProposalStage(3);
        game.setLastSavedAt(new Date());
        gameService.updateById(game);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void uploadThird(ProposalThirdSubmitReq req) {
        Long gameId = req.getGameId();
        Game game = gameService.getById(gameId);
        if (game == null || game.getStage() != 2 || game.getProposalStage() != 2 || game.getProposalRound() != 3) {
            throw new CustomException("游戏状态异常");
        }

        Integer requiredTeamCount = req.getNum();
        List<ProposalThirdSubmitReq.SingleProposal> proposals = req.getProposals();
        if (gameId == null || requiredTeamCount == null || proposals == null || proposals.isEmpty()) {
            throw new CustomException("请求参数不完整");
        }
        List<ProposalThirdSubmitReq.SingleProposal> list = req.getProposals();
        for(ProposalThirdSubmitReq.SingleProposal p : list){
            if(!p.getInvolvedTeamIds().contains(p.getProposerTeamId())){
                throw new CustomException("提案小组ID " + p.getProposerTeamId() + " 不在参与小组中");
            }
        }
        int round = game.getProposalRound();
        // 校验提案内容
        for (int i = 0; i < proposals.size(); i++) {
            ProposalThirdSubmitReq.SingleProposal p = proposals.get(i);
            List<Long> involvedTeamIds = p.getInvolvedTeamIds();
            if (involvedTeamIds == null || involvedTeamIds.size() != requiredTeamCount) {
                throw new CustomException("第 " + (i + 1) + " 个提案的参与小组数量应为 " + requiredTeamCount);
            }
        }
        // 查询当前轮次提案
        List<Proposal> existingList = lambdaQuery()
                .eq(Proposal::getGameId, gameId)
                .eq(Proposal::getRound, round)
                .list();
        Map<Long, Proposal> existingMap = existingList.stream()
                .collect(Collectors.toMap(Proposal::getProposerTeamId, Function.identity()));

        Date now = new Date();
        List<Proposal> updateList = new ArrayList<>();

        for (ProposalThirdSubmitReq.SingleProposal p : proposals) {
            Proposal existing = existingMap.get(p.getProposerTeamId());
            if (existing == null) {
                throw new CustomException("未找到 proposerTeamId 为 " + p.getProposerTeamId() + " 的提案记录");
            }

            existing.setInvolvedTeams(p.getInvolvedTeamIds().stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(",")));
            existing.setUpdateTime(now);
            updateList.add(existing);
        }
        this.updateBatchById(updateList);
        game.setProposalStage(3);
        game.setLastSavedAt(new Date());
        gameService.updateById(game);
    }

    @Override
    public List<ProposalTeamStatusResp> getProposalAllOrder(Long gameId) {
        Game game = gameService.getById(gameId);
        if (game == null || (game.getStage() != 2 && game.getStage() != 3)) {
            throw new CustomException("游戏状态异常");
        }
        List<Team> teams = teamService.lambdaQuery()
                .eq(Team::getGameId, gameId)
                .list();
        // 构造响应列表并计算总分
        List<ProposalTeamStatusResp> resultList = teams.stream()
                .map(team -> {
                    int totalScore = (team.getProposalScoreImported() == null ? 0 : team.getProposalScoreImported())
                            + (team.getProposalScoreAdjusted() == null ? 0 : team.getProposalScoreAdjusted());
                    return new ProposalTeamStatusResp()
                            .setTeamId(team.getId())
                            .setName(team.getLeaderName())
                            .setScore(totalScore);
                })
                .sorted(Comparator.comparingInt(ProposalTeamStatusResp::getScore).reversed())
                .toList();
        // 设置排名
        int rank = 1;
        for (int i = 0; i < resultList.size(); i++) {
            if (i > 0 && resultList.get(i).getScore().equals(resultList.get(i - 1).getScore())) {
                resultList.get(i).setRank(resultList.get(i - 1).getRank());
            } else {
                resultList.get(i).setRank(rank);
            }
            rank++;
        }
        Set<Long> outTeamIds = teamService.lambdaQuery()
                .eq(Team::getGameId, gameId)
                .eq(Team::getAlive, 2)
                .list()
                .stream()
                .map(Team::getId)
                .collect(Collectors.toSet());
        return resultList.stream()
                .filter(resp -> !outTeamIds.contains(resp.getTeamId()))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VoteWinnerResp uploadVote(ProposalVoteReq req) {
        Long gameId = req.getGameId();
        Integer round = req.getRound();

        Game game = gameService.getById(gameId);
        if (game == null || game.getStage() != 2 || game.getProposalStage() != 3) {
            throw new CustomException("游戏状态异常");
        }
        // 获取所有本轮提案
        List<Proposal> proposals = lambdaQuery()
                .eq(Proposal::getGameId, gameId)
                .eq(Proposal::getRound, round)
                .list();
        if (proposals == null || proposals.isEmpty()) {
            throw new CustomException("未找到该轮次提案");
        }
        Map<Long, Proposal> proposalMap = proposals.stream()
                .collect(Collectors.toMap(Proposal::getId, Function.identity()));

        // 获取所有小组
        Map<Long, Team> teamMap = teamService.lambdaQuery()
                .eq(Team::getGameId, gameId)
                .ne(Team::getAlive, 2)
                .list()
                .stream()
                .collect(Collectors.toMap(Team::getId, Function.identity()));

        // 投票列表
        List<ProposalVoteItem> votes = req.getVotes();
        if (votes == null || votes.isEmpty()) {
            throw new CustomException("投票信息不能为空");
        }

        // 提案 -> 累计得分
        Map<Long, Integer> proposalScoreMap = new HashMap<>();

        // 处理每条投票记录
        for (ProposalVoteItem vote : votes) {
            Long fromTeamId = vote.getTeamId();
            Long toProposalId = vote.getProposalId();
            Integer score = vote.getScore();

            if (!proposalMap.containsKey(toProposalId)) {
                throw new CustomException("提案ID非法：" + toProposalId);
            }
            if (!teamMap.containsKey(fromTeamId)) {
                throw new CustomException("投票小组ID非法：" + fromTeamId);
            }
            if (score == null || score < 0) {
                throw new CustomException("投票积分非法：" + score);
            }

            // 从投票小组扣分
            Team fromTeam = teamMap.get(fromTeamId);
            int beforeScore = fromTeam.getProposalScoreImported() == null ? 0 : fromTeam.getProposalScoreImported();
            if (beforeScore < score) {
                throw new CustomException("小组【" + fromTeam.getLeaderName() + "】积分不足");
            }
            fromTeam.setProposalScoreImported(beforeScore - score);
            // 被投小组累计得分
            proposalScoreMap.merge(toProposalId, score, Integer::sum);
        }
        // 保存更新后的队伍积分
        for (Team team : teamMap.values()) {
            teamMapper.updateProposalScoreByCompositeKey(team);
        }
        // 找出得分最高的提案ID
        Long winnerProposalId = proposalScoreMap.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElseThrow(() -> new CustomException("未找到得票最高的提案"))
                .getKey();
        Integer winnerScore = proposalScoreMap.get(winnerProposalId);
        Proposal winnerProposal = proposalMap.get(winnerProposalId);
        List<Proposal> proposalsToUpdate = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : proposalScoreMap.entrySet()) {
            Long proposalId = entry.getKey();
            Integer score = entry.getValue();
            Proposal proposal = proposalMap.get(proposalId);
            if (proposal != null) {
                proposal.setElectedScore(score);
                proposal.setSelected(proposalId.equals(winnerProposalId) ? 1 : 0);
                proposalsToUpdate.add(proposal);
            }
        }
        this.updateBatchById(proposalsToUpdate);
        if (round == 1 && winnerProposal.getInvolvedTeams() != null) {
            Set<Long> involvedTeamIds = Arrays.stream(winnerProposal.getInvolvedTeams().split(","))
                    .map(Long::valueOf)
                    .collect(Collectors.toSet());
            // 设置未参与队伍 alive = -1
            List<Team> toEliminate = teamMap.values().stream()
                    .filter(team -> !involvedTeamIds.contains(team.getId()) && team.getAlive() == 1)
                    .toList();
            for (Team team : toEliminate) {
                teamService.lambdaUpdate()
                        .eq(Team::getId, team.getId())
                        .eq(Team::getGameId, gameId)
                        .set(Team::getAlive, -1)
                        .update();
            }
        }
        // 构造返回结果
        VoteWinnerResp resp = new VoteWinnerResp();
        resp.setProposalId(winnerProposalId);
        resp.setScore(winnerScore);
        resp.setRound(round);
        resp.setTeamId(winnerProposal.getProposerTeamId());
        resp.setTeam(Arrays.stream(winnerProposal.getInvolvedTeams().split(","))
                .map(Integer::valueOf)
                .toList());

        if (winnerProposal.getScoreDistribution() != null) {
            resp.setAllocations(Arrays.stream(winnerProposal.getScoreDistribution().split(","))
                    .map(Integer::valueOf)
                    .toList());
        }
        Team proposerTeam = teamMap.get(winnerProposal.getProposerTeamId());
        if (proposerTeam != null) {
            resp.setLeaderName(proposerTeam.getLeaderName());
        }
        game.setProposalStage(4);
        game.setLastSavedAt(new Date());
        gameService.updateById(game);
        return resp;
    }

    @Override
    public VoteWinnerResp listSelected(Long gameId, Integer round) {
        Game game = gameService.getById(gameId);
        if (game == null || game.getStage() != 2 || game.getProposalStage() < 3) {
            throw new CustomException("游戏状态异常");
        }
        Proposal proposal = lambdaQuery()
                .eq(Proposal::getGameId, gameId)
                .eq(Proposal::getRound, round)
                .eq(Proposal::getSelected, 1)
                .one();
        if (proposal == null) {
            throw new CustomException("未找到该轮次已选择的提案");
        }
        VoteWinnerResp resp = new VoteWinnerResp();
        resp.setProposalId(proposal.getId());
        resp.setScore(proposal.getElectedScore());
        resp.setRound(round);
        resp.setTeamId(proposal.getProposerTeamId());
        resp.setTeam(Arrays.stream(proposal.getInvolvedTeams().split(","))
                .map(Integer::valueOf)
                .toList());
        if (proposal.getScoreDistribution() != null) {
            resp.setAllocations(Arrays.stream(proposal.getScoreDistribution().split(","))
                    .map(Integer::valueOf)
                    .toList());
        }
        Team proposerTeam = teamService.lambdaQuery()
                .eq(Team::getGameId, gameId)
                .eq(Team::getId, proposal.getProposerTeamId())
                .one();
        if (proposerTeam != null) {
            resp.setLeaderName(proposerTeam.getLeaderName());
        }
        return resp;
    }

    @Override
    public List<TeamScoreRankResp> upload(MultipartFile file, Long gameId) {
        Game game = gameService.getById(gameId);
        if (game == null || game.getStage() != 2 || game.getProposalStage() < 4) {
            throw new CustomException("游戏状态异常");
        }
        List<XxtStudentScoreExcelDTO> scores = gameService.validateAndParseFile(file);
        // 3. 根据上传成绩构建：teamId -> 成绩列表 映射
        Map<Long, List<XxtStudentScoreExcelDTO>> groupMap = gameService.buildTeamGroupMap(scores, gameId, game.getCid());
        // 4. 获取该游戏所有小组映射：teamId -> Team
        Map<Long, Team> teamMap = teamService.lambdaQuery()
                .eq(Team::getGameId, gameId)
                .list().stream().collect(Collectors.toMap(Team::getId, t -> t));
        // 5. 计算本轮小组得分，更新累计得分，并生成排行榜响应
        int maxCount = game.getTeamMemberCount();
        List<TeamScoreRankResp> resultList = new ArrayList<>();
        List<ScoreUpdateDTO> updateList = new ArrayList<>();
        List<ProposalRoundTeamScore> teamScoreList = new ArrayList<>();
        int round = Optional.ofNullable(
                        proposalRoundTeamScoreService.lambdaQuery()
                                .select(ProposalRoundTeamScore::getSubRound)
                                .eq(ProposalRoundTeamScore::getGameId, gameId)
                                .orderByDesc(ProposalRoundTeamScore::getSubRound)
                                .last("limit 1")
                                .one()
                )
                .map(ProposalRoundTeamScore::getSubRound)
                .orElse(0);
        log.info("先前轮次：{}", round);
        for (Map.Entry<Long, List<XxtStudentScoreExcelDTO>> entry : groupMap.entrySet()) {
            Long teamId = entry.getKey();
            List<XxtStudentScoreExcelDTO> teamScores = entry.getValue();
            ProposalRoundTeamScore teamScore = new ProposalRoundTeamScore();
            teamScore.setGameId(gameId);
            teamScore.setTeamId(teamId);
            teamScore.setRound(game.getProposalRound());
            teamScore.setSubRound(round+1);
            // 计算 topN 分数总和
            List<Integer> topScores = teamScores.stream()
                    .map(dto -> {
                        try {
                            return Integer.parseInt(dto.getScore());
                        } catch (NumberFormatException e) {
                            return 0;
                        }
                    })
                    .sorted(Comparator.reverseOrder())
                    .limit(maxCount)
                    .toList();
            int thisRoundScore = topScores.stream().mapToInt(Integer::intValue).sum();
            // 获取最新提交时间
            LocalDateTime latestTime = teamScores.stream()
                    .map(XxtStudentScoreExcelDTO::getTime)
                    .filter(Objects::nonNull)
                    .max(LocalDateTime::compareTo)
                    .orElse(LocalDateTime.MIN);
            teamScore.setScore(thisRoundScore);
            teamScore.setDeadline(latestTime);
            // 更新 Team 总分
            Team team = teamMap.get(teamId);
            if (team != null) {
                // 构造排名返回对象
                TeamScoreRankResp resp = new TeamScoreRankResp();
                resp.setTeamId(teamId);
                resp.setTeamName(team.getLeaderName());
                resp.setThisRoundScore(thisRoundScore);
                resp.setSubmitTime(latestTime);
                resp.setStatus(team.getAlive());
                resultList.add(resp);
            }
            // 收集成员得分，用于批量更新
            for (XxtStudentScoreExcelDTO dto : teamScores) {
                if (org.apache.commons.lang3.StringUtils.isNotBlank(dto.getSno())) {
                    ScoreUpdateDTO param = new ScoreUpdateDTO();
                    param.setSno(dto.getSno());
                    try {
                        param.setAddScore(Integer.parseInt(dto.getScore()));
                    } catch (NumberFormatException e) {
                        param.setAddScore(0);
                    }
                    updateList.add(param);
                }
            }
            teamScoreList.add(teamScore);
        }
        // 排序
        resultList.sort(
                Comparator.comparing(TeamScoreRankResp::getStatus)
                        .thenComparing(TeamScoreRankResp::getThisRoundScore, Comparator.reverseOrder())
                        .thenComparing(TeamScoreRankResp::getSubmitTime)
        );
        // 批量更新成员分数
        if (!updateList.isEmpty()) {
            for (ScoreUpdateDTO dto : updateList) {
                teamMemberMapper.addScore(dto.getSno(), game.getId(), dto.getAddScore());
            }
        }
        log.info("上传成绩成功，更新小组得分：{}", teamScoreList);
        proposalRoundTeamScoreService.saveBatch(teamScoreList);
        // 获取所有学生信息，或许可以放入异步？
        List<TeamMember> members = teamMemberService.lambdaQuery()
                .eq(TeamMember::getGameId, gameId)
                .list();
        // 构建 SNO -> TeamMember 映射
        Map<String, TeamMember> memberMap = members.stream()
                .collect(Collectors.toMap(TeamMember::getSno, m -> m, (a, b) -> a));
        threadPoolTaskExecutor.execute(() -> {
            List<StudentScoreLog> studentLogs = scores.stream()
                    .map(dto -> {
                        TeamMember member = memberMap.get(dto.getSno());
                        if (member == null) {
                            // 可选：记录日志或忽略
                            return null;
                        }
                        StudentScoreLog log = new StudentScoreLog();
                        log.setStudentId(member.getStudentId());
                        log.setTeamId(member.getTeamId());
                        log.setGameId(gameId);
                        log.setScore(parseScore(dto.getScore()));
                        log.setReason(3);
                        log.setPhase(2);
                        log.setRound(game.getChessRound());
                        log.setComment("第" + game.getChessRound() + "轮提案赛中获得成绩，实际得分为" + dto.getScore() + "分（来源：学习通）");
                        return log;
                    })
                    .filter(Objects::nonNull)
                    .toList();
            studentScoreLogService.saveBatch(studentLogs);
        });
        Set<Long> outTeamIds = teamService.lambdaQuery()
                .eq(Team::getGameId, gameId)
                .eq(Team::getAlive, 2)
                .list()
                .stream()
                .map(Team::getId)
                .collect(Collectors.toSet());
        return resultList.stream()
                .filter(resp -> !outTeamIds.contains(resp.getTeamId()))
                .toList();

    }

@Override
public ProposalFirstSettleResp outTeam(OutTeamReq req) {
    Game game = gameService.getById(req.getGameId());
    if (game == null) {
        throw new CustomException("无效的gameId");
    }
    if (game.getStage() != 2 || game.getProposalRound() != 1 || game.getProposalStage() != 4) {
        throw new CustomException("当前阶段不能淘汰小组");
    }

    // 查询当前存活小组数量
    long aliveCount = teamService.lambdaQuery()
            .eq(Team::getGameId, req.getGameId())
            .eq(Team::getAlive, 1)
            .count();

    // 如果一开始就只剩一个存活小组，直接返回，说明已完成结算
    if (aliveCount <= 1) {
        ProposalFirstSettleResp resp = new ProposalFirstSettleResp();
        resp.setTriggered(false);
        resp.setDetails(Collections.emptyList());
        return resp;
    }

    // 校验淘汰数量合法性
    if (req.getTeamIds().size() >= aliveCount) {
        throw new CustomException("淘汰数量过多，将导致所有小组淘汰，当前存活小组数：" + aliveCount);
    }

    // 查询要淘汰的小组实体
    List<Team> teams = teamService.lambdaQuery()
            .eq(Team::getGameId, req.getGameId())
            .in(Team::getId, req.getTeamIds())
            .list();
    if (teams.size() != req.getTeamIds().size()) {
        Set<Long> existingIds = teams.stream().map(Team::getId).collect(Collectors.toSet());
        List<Long> invalidIds = req.getTeamIds().stream()
                .filter(id -> !existingIds.contains(id))
                .toList();
        throw new CustomException("无效的团队ID：" + invalidIds);
    }

    // 更新淘汰状态
    Date now = new Date();
    for (Team team : teams) {
        if (team.getAlive() == 0) {
            throw new CustomException("团队已淘汰：" + team.getId());
        }
        team.setAlive(0);
        team.setEliminatedTime(now);
        teamMapper.updateAliveByCompositeKey(team);
    }

    // 再次查询剩余存活小组
    long leftAlive = teamService.lambdaQuery()
            .eq(Team::getGameId, req.getGameId())
            .eq(Team::getAlive, 1)
            .count();

    // 如果只剩一个存活小组，触发结算
    if (leftAlive <= 1) {
        Proposal proposal = lambdaQuery()
                .eq(Proposal::getGameId, req.getGameId())
                .eq(Proposal::getRound, 1)
                .eq(Proposal::getSelected, 1)
                .one();
        if (proposal == null) {
            throw new CustomException("未找到选中的提案，无法结算");
        }

        // 提取参赛小组和分配积分
        List<Long> involvedTeamIds = Arrays.stream(proposal.getInvolvedTeams().split(","))
                .map(Long::valueOf).toList();
        List<Integer> scoreList = Arrays.stream(proposal.getScoreDistribution().split(","))
                .map(Integer::valueOf).toList();

        if (involvedTeamIds.size() != scoreList.size()) {
            throw new CustomException("积分分配数量与参赛队伍数量不一致");
        }

        // 获取淘汰排名
        List<ProposalOutStatusResp> ranks = getFirstRank(req.getGameId());
        log.info("获取小组排名结果：{}", ranks);
        Map<Long, Integer> teamToRank = ranks.stream()
                .filter(resp -> resp.getRank() != null)
                .collect(Collectors.toMap(
                        ProposalOutStatusResp::getTeamId,
                        ProposalOutStatusResp::getRank
                ));

        // 根据排名对 involvedTeams 排序
        List<Long> sortedTeams = involvedTeamIds.stream()
                .sorted(Comparator.comparingInt(teamToRank::get))
                .toList();
        log.info("结算排序结果：{}", sortedTeams.stream()
                .map(id -> String.format("teamId=%d, rank=%d", id, teamToRank.get(id)))
                .toList());
        // 批量查小组并累加分数
        List<Team> teamList = teamService.lambdaQuery()
                .eq(Team::getGameId, req.getGameId())
                .in(Team::getId, involvedTeamIds)
                .list();
        Map<Long, Team> teamMap = teamList.stream().collect(Collectors.toMap(Team::getId, t -> t));
        List<Team> updateList = new ArrayList<>();
        List<ProposalFirstSettleResp.ScoreDetail> resultDetails = new ArrayList<>();

        for (int i = 0; i < sortedTeams.size(); i++) {
            Long teamId = sortedTeams.get(i);
            Integer addScore = scoreList.get(i);
            Team team = teamMap.get(teamId);
            if (team == null) continue;

            int before = Optional.ofNullable(team.getProposalScoreImported()).orElse(0);
            team.setProposalScoreImported(before + addScore);
            updateList.add(team);

            ProposalFirstSettleResp.ScoreDetail detail = new ProposalFirstSettleResp.ScoreDetail();
            detail.setTeamId(teamId);
            detail.setLeaderName(team.getLeaderName());
            detail.setAddScore(addScore);
            detail.setFinalScore(before + addScore);
            resultDetails.add(detail);
        }

        for(Team team : updateList){
            teamMapper.updateProposalScoreByCompositeKey(team);
        }
        // 输出日志
        log.info("【提案赛结算完成】gameId={}, 分配详情={}", req.getGameId(), resultDetails);

        // 返回结果
        ProposalFirstSettleResp resp = new ProposalFirstSettleResp();
        resp.setTriggered(true);
        resp.setDetails(resultDetails);
        gameService.lambdaUpdate()
                .eq(Game::getId, req.getGameId())
                .set(Game::getProposalRound, 2)
                .set(Game::getProposalStage, 2)
                .update();
        return resp;
    }
    // 未触发结算
    ProposalFirstSettleResp resp = new ProposalFirstSettleResp();
    resp.setTriggered(false);
    resp.setDetails(Collections.emptyList());
    return resp;
}




    @Override
    public List<ProposalOutStatusResp> getFirstRank(Long gameId) {
        Game game = gameService.getById(gameId);
        if (game == null) {
            throw new CustomException("无效的 gameId");
        }
        if (game.getStage() != 2 || game.getProposalRound() < 1) {
            throw new CustomException("当前阶段不能查看淘汰赛小组排名");
        }
        List<Team> teamList = teamService.lambdaQuery()
                .eq(Team::getGameId, gameId)
                .list();
        List<ProposalOutStatusResp> aliveList = new ArrayList<>();
        List<ProposalOutStatusResp> outList = new ArrayList<>();
        List<ProposalOutStatusResp> unjoinedList = new ArrayList<>();

        for (Team team : teamList) {
            ProposalOutStatusResp resp = new ProposalOutStatusResp();
            resp.setTeamId(team.getId());
            resp.setName(team.getLeaderName());
            resp.setAlive(team.getAlive());
            resp.setOutTime(team.getEliminatedTime());

            if (team.getAlive() == 1) {
                aliveList.add(resp);
            } else if (team.getAlive() == 0) {
                outList.add(resp);
            } else {
                unjoinedList.add(resp);
            }
        }
        // 1. 所有存活的队伍 -> rank = 1
        for (ProposalOutStatusResp alive : aliveList) {
            alive.setRank(1);
        }
        // 2. 淘汰队伍按时间升序排序
        outList.sort((a, b) -> {
            if (a.getOutTime() == null && b.getOutTime() != null) return 1;
            if (a.getOutTime() != null && b.getOutTime() == null) return -1;
            if (a.getOutTime() == null) return 0;
            return b.getOutTime().compareTo(a.getOutTime());
        });
        // 3. 淘汰的排名从存活之后开始排
        int rank = aliveList.isEmpty() ? 1 : 2;
        for (ProposalOutStatusResp out : outList) {
            out.setRank(rank++);
        }
        List<ProposalOutStatusResp> result = new ArrayList<>();
        result.addAll(aliveList);
        result.addAll(outList);
        result.addAll(unjoinedList);
        Set<Long> outTeamIds = teamService.lambdaQuery()
                .eq(Team::getGameId, gameId)
                .eq(Team::getAlive, 2)
                .list()
                .stream()
                .map(Team::getId)
                .collect(Collectors.toSet());
        return result.stream()
                .filter(resp -> !outTeamIds.contains(resp.getTeamId()))
                .toList();

    }


    @Override
    public DebateEvaluationResp evaluateDebateScore(DebateEvaluationReq req) {
        Game game = gameService.getById(req.getGameId());
        if (game == null || game.getStage() != 2 || game.getProposalRound() != 2 || game.getProposalStage() != 4) {
            throw new CustomException("游戏状态异常");
        }
        Proposal proposal = lambdaQuery()
                .eq(Proposal::getId, req.getProposalId())
                .eq(Proposal::getGameId, req.getGameId())
                .eq(Proposal::getSelected, 1)
                .one();
        if (proposal == null) {
            throw new CustomException("提案不存在或未被选中");
        }
        // 解析参与小组 ID
        String[] teamIds = proposal.getInvolvedTeams().split(",");
        if (teamIds.length % 2 != 0) {
            throw new CustomException("参与小组数不是偶数，无法分组");
        }

        int half = teamIds.length / 2;
        List<Integer> proTeamIds = Arrays.stream(teamIds, 0, half)
                .map(Integer::parseInt)
                .toList();
        List<Integer> conTeamIds = Arrays.stream(teamIds, half, teamIds.length)
                .map(Integer::parseInt)
                .toList();
        // 计算平均分
        double avgPro = req.getStudentScores().stream()
                .mapToInt(DebateEvaluationReq.StudentGroupScore::getScorePro)
                .average().orElse(0.0);
        double avgCon = req.getStudentScores().stream()
                .mapToInt(DebateEvaluationReq.StudentGroupScore::getScoreCon)
                .average().orElse(0.0);

        double finalPro = req.getTeacherScorePro() * 0.6 + avgPro * 0.4;
        double finalCon = req.getTeacherScoreCon() * 0.6 + avgCon * 0.4;
        DebateEvaluationResp resp = new DebateEvaluationResp();
        resp.setProFinalScore(finalPro);
        resp.setConFinalScore(finalCon);
        if (finalPro > finalCon) {
            resp.setIds(proTeamIds);
        } else if (finalCon > finalPro) {
            resp.setIds(conTeamIds);
        } else {
            resp.setIds(Collections.emptyList());
        }
        List<Integer> ids = resp.getIds();
        gameService.lambdaUpdate()
                .eq(Game::getId, req.getGameId())
                .set(Game::getProposalStage, 2)
                .set(Game::getProposalRound, 3)
                .update();
        if (ids.isEmpty()) {
            // 平局，不更新小组得分
            return resp;
        }
        Map<Long, Team>  teamMap = teamService.lambdaQuery()
                .eq(Team::getGameId, req.getGameId())
                .in(Team::getId, ids)
                .list()
                .stream()
                .collect(Collectors.toMap(Team::getId, team -> team));
        int bonus = 8;
        List<Team> updateList = new ArrayList<>();
        for (Integer teamId : ids) {
            Team team = teamMap.get(teamId.longValue());
            if (team != null) {
                Integer oldScore = team.getProposalScoreImported();
                team.setProposalScoreImported((oldScore != null ? oldScore : 0) + bonus);
                updateList.add(team);
            }
        }
        for(Team team : updateList){
            teamMapper.updateProposalScoreByCompositeKey(team);
        }
        return resp;
    }

    @Override
    public List<ProposalRoundTeamScoreResp> getRoundScoreStatus(Long gameId) {
        Game game = gameService.getById(gameId);
        if (game == null || game.getStage() != 2 || game.getProposalRound() != 3) {
            throw new CustomException("游戏状态异常");
        }
        Proposal proposal = lambdaQuery()
                .eq(Proposal::getGameId, gameId)
                .eq(Proposal::getRound, 3)
                .eq(Proposal::getSelected, 1)
                .one();
        if (proposal == null || proposal.getInvolvedTeams() == null || proposal.getInvolvedTeams().isEmpty()) {
            throw new CustomException("未找到第三轮已选中的提案或参赛小组为空");
        }
        // 解析逗号分隔的参赛小组ID
        List<Long> involvedTeamIds = Arrays.stream(proposal.getInvolvedTeams().split(","))
                .map(Long::valueOf)
                .toList();
        // 查询这批小组的所有加减分日志
        List<ProposalRoundTeamScore> allScores = proposalRoundTeamScoreService.lambdaQuery()
                .eq(ProposalRoundTeamScore::getGameId, gameId)
                .eq(ProposalRoundTeamScore::getRound, 3)
                .in(ProposalRoundTeamScore::getTeamId, involvedTeamIds)
                .list();

        // 按 teamId 分组累计分数，没日志的默认为0
        Map<Long, Integer> teamTotalScore = allScores.stream()
                .collect(Collectors.groupingBy(ProposalRoundTeamScore::getTeamId,
                        Collectors.summingInt(s -> s.getScore() != null ? s.getScore() : 0)));

        // 保证所有参赛小组都有记录，没有的补0
        for (Long tid : involvedTeamIds) {
            teamTotalScore.putIfAbsent(tid, 0);
        }
        // 查询这些小组的基本信息
        List<Team> teams = teamService.lambdaQuery()
                .eq(Team::getGameId, gameId)
                .in(Team::getId, involvedTeamIds)
                .list();
        Map<Long, Team> teamMap = teams.stream().collect(Collectors.toMap(Team::getId, t -> t));
        // 转成返回对象列表，并根据分数倒序排序
        List<ProposalRoundTeamScoreResp> respList = teamTotalScore.entrySet().stream()
                .map(e -> {
                    ProposalRoundTeamScoreResp r = new ProposalRoundTeamScoreResp();
                    r.setTeamId(e.getKey());
                    r.setScore(e.getValue());
                    Team team = teamMap.get(e.getKey());
                    r.setLeaderName(team != null ? team.getLeaderName() : "未知");
                    return r;
                })
                .sorted(Comparator.comparingInt(ProposalRoundTeamScoreResp::getScore).reversed())
                .toList();
        // 计算排名，处理并列
        int rank = 0, realRank = 0, prevScore = Integer.MIN_VALUE;
        for (ProposalRoundTeamScoreResp r : respList) {
            realRank++;
            if (!r.getScore().equals(prevScore)) {
                rank = realRank;
                prevScore = r.getScore();
            }
            r.setRank(rank);
        }
        Set<Long> outTeamIds = teamService.lambdaQuery()
                .eq(Team::getGameId, gameId)
                .eq(Team::getAlive, 2)
                .list()
                .stream()
                .map(Team::getId)
                .collect(Collectors.toSet());

        return respList.stream()
                .filter(resp -> !outTeamIds.contains(resp.getTeamId()))
                .toList();

    }

    @Override
    public void adjustScore(ProposalRoundScoreAdjustReq req) {
       Game game = gameService.getById(req.getGameId());
       if (game == null || game.getStage() != 2 || game.getProposalRound() != 3 || game.getProposalStage() != 4) {
           throw new CustomException("游戏状态异常");
       }
        ProposalRoundTeamScore record = new ProposalRoundTeamScore();
        record.setGameId(req.getGameId());
        record.setRound(3); // 第三轮固定
        record.setTeamId(req.getTeamId());
        record.setScore(req.getScore());
        record.setComment(req.getComment());
        proposalRoundTeamScoreService.save(record);
    }

    @Override
    public List<ProposalRoundTeamScoreResp> settleThirdRoundBuzzCompetition(Long gameId) {
        Game game = gameService.getById(gameId);
        if (game == null || game.getStage() != 2 || game.getProposalRound() != 3 || game.getProposalStage() != 4) {
            throw new CustomException("游戏状态异常，不允许结算第三轮抢答赛");
        }
        Proposal proposal = lambdaQuery()
                .eq(Proposal::getGameId, gameId)
                .eq(Proposal::getRound, 3)
                .eq(Proposal::getSelected, 1)
                .one();

        if (proposal == null) {
            throw new CustomException("未找到第三轮选中的提案");
        }

        List<Long> involvedTeamIds = Arrays.stream(proposal.getInvolvedTeams().split(","))
                .map(Long::valueOf)
                .toList();

        if (involvedTeamIds.isEmpty()) {
            throw new CustomException("提案中无参赛小组");
        }

        // 查找抢答记录
        List<ProposalRoundTeamScore> scoreLogs = proposalRoundTeamScoreService.lambdaQuery()
                .eq(ProposalRoundTeamScore::getGameId, gameId)
                .eq(ProposalRoundTeamScore::getRound, 3)
                .in(ProposalRoundTeamScore::getTeamId, involvedTeamIds)
                .list();

        // 统计总分（无记录的视为0分）
        Map<Long, Integer> teamScoreMap = new HashMap<>();
        for (Long teamId : involvedTeamIds) {
            teamScoreMap.put(teamId, 0); // 默认 0 分
        }
        for (ProposalRoundTeamScore score : scoreLogs) {
            teamScoreMap.merge(score.getTeamId(), Optional.ofNullable(score.getScore()).orElse(0), Integer::sum);
        }
        // 排名排序
        List<Map.Entry<Long, Integer>> sortedEntries = teamScoreMap.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .toList();
        // 排名分数规则
        Map<Integer, Integer> rankScoreMap = Map.of(
                1, 10,
                2, 8,
                3, 6
        );
        // 查询小组信息
        List<Team> teams = teamService.lambdaQuery()
                .eq(Team::getGameId, gameId)
                .in(Team::getId, involvedTeamIds)
                .list();
        Map<Long, Team> teamMap = teams.stream().collect(Collectors.toMap(Team::getId, t -> t));
        // 执行加分，组装返回对象
        List<ProposalRoundTeamScoreResp> respList = new ArrayList<>();
        int rank = 0;
        int realRank = 0;
        Integer prevScore = null;

        for (int i = 0; i < sortedEntries.size(); i++) {
            Map.Entry<Long, Integer> entry = sortedEntries.get(i);
            Long teamId = entry.getKey();
            Integer buzzScore = entry.getValue();

            realRank++;
            if (!Objects.equals(prevScore, buzzScore)) {
                rank = realRank;
                prevScore = buzzScore;
            }

            Integer addScore = rankScoreMap.getOrDefault(rank, 0);

            Team team = teamMap.get(teamId);
            if (team == null) continue;

            int before = Optional.ofNullable(team.getProposalScoreImported()).orElse(0);
            team.setProposalScoreImported(before + addScore);
            teamMapper.updateProposalScoreByCompositeKey(team);
            ProposalRoundTeamScoreResp r = new ProposalRoundTeamScoreResp();
            r.setTeamId(teamId);
            r.setLeaderName(team.getLeaderName());
            r.setScore(before + addScore);
            r.setRank(rank);
            respList.add(r);
        }
        log.info("【抢答赛结算完成】gameId={}, 前三名结果={}", gameId, respList.stream().limit(3).toList());
        game.setStage(3);
        game.setStatus(2);
        game.setLastSavedAt(new Date());
        gameService.updateById(game);
        Set<Long> outTeamIds = teamService.lambdaQuery()
                .eq(Team::getGameId, gameId)
                .eq(Team::getAlive, 2)
                .list()
                .stream()
                .map(Team::getId)
                .collect(Collectors.toSet());
        return respList.stream().filter(resp -> !outTeamIds.contains(resp.getTeamId())).limit(3).toList();
    }

    @Override
    public List<ProposalCommonResp> proposalList(Long gameId, Integer round) {
        Game game = gameService.getById(gameId);
        if (game == null || game.getStage() != 2 || game.getProposalStage() < 2) {
            throw new CustomException("游戏状态异常");
        }
        List<Proposal> proposals = lambdaQuery()
                .eq(Proposal::getGameId, gameId)
                .eq(Proposal::getRound, round)
                .list();
        Map<Long, Team> teamMap = teamService.lambdaQuery()
                .eq(Team::getGameId, gameId)
                .in(Team::getId, proposals.stream().map(Proposal::getProposerTeamId).toList())
                .list()
                .stream()
                .collect(Collectors.toMap(Team::getId, t -> t));
        List<ProposalCommonResp> result = new ArrayList<>();
        for (Proposal proposal : proposals) {
            ProposalCommonResp resp = new ProposalCommonResp();
            resp.setId(proposal.getId());
            resp.setProposerTeamId(proposal.getProposerTeamId());
            resp.setVoteCount(proposal.getElectedScore());
            resp.setProposerTeamName(teamMap.get(proposal.getProposerTeamId()).getLeaderName());
            resp.setInvolvedTeamIds(
                    Optional.ofNullable(proposal.getInvolvedTeams())
                            .filter(str -> !str.isBlank())
                            .map(str -> Arrays.stream(str.split(",")).map(Long::valueOf).toList())
                            .orElse(Collections.emptyList())
            );
            resp.setScoreDistribution(
                    Optional.ofNullable(proposal.getScoreDistribution())
                            .filter(str -> !str.isBlank())
                            .map(str -> Arrays.stream(str.split(",")).map(this::parseScore).toList())
                            .orElse(Collections.emptyList())
            );
            resp.setIsSelected(proposal.getSelected() == 1);
            result.add(resp);
        }
        return result;


    }

    @Override
    public void adjustGlobalScore(ProposalScoreAdjustReq req) {
        Game game = gameService.getById(req.getGameId());
        if (game == null || game.getStage() != 2) {
            throw new CustomException("游戏状态异常");
        }
        Team team = teamService.lambdaQuery()
                .eq(Team::getGameId, req.getGameId())
                .eq(Team::getId, req.getTeamId())
                .one();
        if (team == null) {
            throw new CustomException("小组不存在");
        }
        team.setProposalScoreImported(req.getScore() + team.getProposalScoreImported());
        teamMapper.updateProposalScoreByCompositeKey(team);
        log.info("【管理员手动调整提案赛分数】gameId={}, teamId={}, score={}", req.getGameId(), req.getTeamId(), req.getScore());
    }

    @Override
    public List<Long> listNeedScore(Long gameId) {
        Game game = gameService.getById(gameId);
        if (game == null || game.getStage() != 2) {
            throw new CustomException("游戏状态异常");
        }
        List<Long> teamIds = teamService.lambdaQuery()
                .eq(Team::getGameId, gameId)
                .ne(Team::getAlive, 2)
                .list()
                .stream()
                .map(Team::getId)
                .toList();
        Set<Long> involvedTeamIds = lambdaQuery()
                .eq(Proposal::getGameId, gameId)
                .eq(Proposal::getRound, 2)
                .eq(Proposal::getSelected, 1)
                .select(Proposal::getInvolvedTeams)
                .list()
                .stream()
                .map(Proposal::getInvolvedTeams)
                .filter(str -> str != null && !str.isBlank())
                .flatMap(str -> Arrays.stream(str.split(",")).map(String::trim).map(Long::valueOf))
                .collect(Collectors.toSet());
        return teamIds.stream()
                .filter(id -> !involvedTeamIds.contains(id))
                .toList();
    }

    @Override
    public List<Long> scoreList(Long gameId, Integer type) {
        Game game = gameService.getById(gameId);
        if (game == null || game.getStage() != 2) {
            throw new CustomException("游戏状态异常");
        }
        List<Long> all = gameService.getTeamRank(game)
                .stream()
                .map(TeamRankResp::getTeamId)
                .toList();
        var valid = teamService.lambdaQuery()
                .eq(Team::getGameId, gameId)
                .select(Team::getId)
                .eq(Team::getAlive,2)
                .list()
                .stream()
                .map(Team::getId)
                .toList();
        if(type == 1){
            return all.stream()
                    .filter(valid::contains)
                    .sorted()
                    .toList();
        }
        return all.stream()
                .filter(valid::contains)
                .toList();
    }

    @Override
    public ProposalDetailResp getDetail(Long proposalId) {
        Proposal proposal = lambdaQuery()
                .eq(Proposal::getId, proposalId)
                .one();
        ProposalDetailResp resp = new ProposalDetailResp();
        resp.setVoteCount(proposal.getElectedScore());
        resp.setRound(proposal.getRound());
        resp.setId(proposal.getId());
        resp.setTeamId(proposal.getProposerTeamId());
        resp.setTeamName(teamService.lambdaQuery()
                .eq(Team::getId, proposal.getProposerTeamId())
                .eq(Team::getGameId, proposal.getGameId())
                .one()
                .getLeaderName());
        resp.setInvolvedTeamIds(Arrays.stream(proposal.getInvolvedTeams().split(","))
                .map(Long::valueOf)
                .toList());
        if (proposal.getScoreDistribution() != null) {
            resp.setScoreDistribution(Arrays.stream(proposal.getScoreDistribution().split(","))
                    .map(Integer::valueOf)
                    .toList());
        }
        return resp;
    }


    private int parseScore(String raw) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (Exception e) {
            return 0;
        }
    }


}



