SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for board_config
-- ----------------------------
DROP TABLE IF EXISTS `board_config`;
CREATE TABLE `board_config`  (
                                 `id` bigint(20) NOT NULL AUTO_INCREMENT,
                                 `game_id` bigint(20) NOT NULL COMMENT '所属游戏ID',
                                 `total_tiles` int(11) NOT NULL COMMENT '棋盘总格子数',
                                 `black_swamp_tiles` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '黑沼泽格子编号（逗号分隔）',
                                 `blind_box_tiles` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '盲盒秘境格子编号（逗号分隔）',
                                 `fortress_tiles` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '决斗要塞格子编号（逗号分隔）',
                                 `gold_center_tiles` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '黄金中心格子编号（逗号分隔）',
                                 `opportunity_tiles` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '机会宝地格子编号（逗号分隔）',
                                 `gmt_create` datetime NULL DEFAULT CURRENT_TIMESTAMP,
                                 PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 59 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '棋盘配置表：记录格子总数和特殊格子分布' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for class_student
-- ----------------------------
DROP TABLE IF EXISTS `class_student`;
CREATE TABLE `class_student`  (
                                  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
                                  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '学生姓名',
                                  `sex` tinyint(1) NULL DEFAULT 1 COMMENT '性别(0：女 1：男，默认为1)',
                                  `university_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '学校名称',
                                  `department_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '所属学院',
                                  `major` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '所学专业',
                                  `gmt_create` datetime NULL DEFAULT NULL COMMENT '创建时间',
                                  `gmt_modified` datetime NULL DEFAULT NULL COMMENT '更新时间',
                                  `cid` bigint(20) NOT NULL COMMENT '班级id',
                                  `Is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除(0：未删除 1：已删除)',
                                  `sno` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '学号',
                                  PRIMARY KEY (`id`) USING BTREE,
                                  UNIQUE INDEX `class_student_pk`(`cid`, `sno`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 608 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for classes
-- ----------------------------
DROP TABLE IF EXISTS `classes`;
CREATE TABLE `classes`  (
                            `id` bigint(20) NOT NULL AUTO_INCREMENT,
                            `class_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '班级编码',
                            `course_id` bigint(20) NOT NULL COMMENT '所属课程ID',
                            `current_students` int(11) NULL DEFAULT 0 COMMENT '当前学生数',
                            `status` tinyint(4) NULL DEFAULT 1 COMMENT '状态：1-活跃，0-归档',
                            `t_id` bigint(20) NOT NULL COMMENT '授课教师id',
                            `gmt_create` datetime NULL DEFAULT CURRENT_TIMESTAMP,
                            `gmt_modified` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                            `is_deleted` tinyint(4) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
                            PRIMARY KEY (`id`) USING BTREE,
                            INDEX `idx_course_id`(`course_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '班级表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for course
-- ----------------------------
DROP TABLE IF EXISTS `course`;
CREATE TABLE `course`  (
                           `id` bigint(20) NOT NULL AUTO_INCREMENT,
                           `course_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '课程名称',
                           `course_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '课程编码',
                           `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '课程描述',
                           `t_id` bigint(20) NOT NULL COMMENT '授课教师id',
                           `t_name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '教师名称',
                           `semester` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '学期',
                           `academic_year` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '学年',
                           `status` tinyint(4) NULL DEFAULT 1 COMMENT '状态：1-进行中，0-已结束',
                           `gmt_create` datetime NULL DEFAULT CURRENT_TIMESTAMP,
                           `gmt_modified` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                           `is_deleted` tinyint(4) NOT NULL DEFAULT 0,
                           PRIMARY KEY (`id`) USING BTREE,
                           UNIQUE INDEX `course_code`(`course_code`) USING BTREE,
                           INDEX `idx_t_id`(`t_id`) USING BTREE,
                           INDEX `idx_course_code`(`course_code`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '课程表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for game
-- ----------------------------
DROP TABLE IF EXISTS `game`;
CREATE TABLE `game`  (
                         `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '游戏ID',
                         `cid` bigint(20) NOT NULL COMMENT '教学班id',
                         `student_count` int(11) NOT NULL COMMENT '学生总数',
                         `team_count` int(11) NOT NULL COMMENT '小组总数',
                         `team_member_count` int(11) NOT NULL COMMENT '每组计分人数',
                         `stage` tinyint(4) NOT NULL COMMENT '0-未初始化阶段 1-棋盘赛，2-提案赛',
                         `chess_round` tinyint(4) NULL DEFAULT NULL COMMENT '当前棋盘赛轮次（1~4）',
                         `chess_phase` tinyint(4) NULL DEFAULT NULL COMMENT '0-上传成绩阶段 1-上传领地阶段 2-走棋，3-结算',
                         `proposal_stage` tinyint(4) NULL DEFAULT NULL COMMENT '（0-初始化提案积分 1-选择提案提出轮次 2-每轮提出提案 3-正式游戏）',
                         `proposal_round` tinyint(4) NULL DEFAULT NULL COMMENT '提案赛轮次',
                         `status` tinyint(4) NULL DEFAULT 1 COMMENT '游戏状态：1-进行中，2-已结束',
                         `last_saved_at` datetime NULL DEFAULT NULL COMMENT '上次保存时间',
                         `gmt_create` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                         `gmt_update` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                         PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 84 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '游戏主表：记录游戏基本状态、元信息、阶段进度等' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for permission
-- ----------------------------
DROP TABLE IF EXISTS `permission`;
CREATE TABLE `permission`  (
                               `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
                               `permission_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '权限名称',
                               `key_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '权限关键词(权限认证使用此字段)',
                               `path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '路由路径',
                               `perms` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '参数',
                               `component` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '路由组件',
                               `gmt_create` datetime(6) NULL DEFAULT NULL,
                               `gmt_modified` datetime(6) NULL DEFAULT NULL,
                               `create_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
                               `update_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
                               `is_deleted` tinyint(1) NULL DEFAULT 0,
                               `parent_id` bigint(20) NULL DEFAULT 0 COMMENT '父级权限id',
                               `icon` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '图标',
                               PRIMARY KEY (`id`) USING BTREE,
                               UNIQUE INDEX `key_name`(`key_name`) USING BTREE,
                               INDEX `father`(`parent_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for proposal
-- ----------------------------
DROP TABLE IF EXISTS `proposal`;
CREATE TABLE `proposal`  (
                             `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
                             `round` int(11) NOT NULL COMMENT '第几轮提案',
                             `proposer_team_id` bigint(20) UNSIGNED NOT NULL COMMENT '提出提案的小组ID',
                             `elected_score` int(11) NOT NULL DEFAULT 0 COMMENT '被选举获得的积分',
                             `involved_teams` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '逗号分隔的参赛小组ID列表',
                             `score_distribution` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '28个积分的分配方式，逗号分隔字符串，如 \"10,5,13\"',
                             `selected` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否被选中，0=否，1=是',
                             `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                             `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                             `game_id` bigint(20) NOT NULL COMMENT '游戏id',
                             PRIMARY KEY (`id`) USING BTREE,
                             INDEX `idx_proposer_team_id`(`proposer_team_id`) USING BTREE,
                             INDEX `idx_round`(`round`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 15 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '提案表，存储每轮小组提案信息' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for proposal_round_team_score
-- ----------------------------
DROP TABLE IF EXISTS `proposal_round_team_score`;
CREATE TABLE `proposal_round_team_score`  (
                                              `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                              `game_id` bigint(20) NOT NULL COMMENT '游戏ID',
                                              `round` int(11) NOT NULL COMMENT '提案轮次（1-3）',
                                              `sub_round` int(11) NOT NULL COMMENT '子轮次（每轮内部的小对局，如1、2、3）',
                                              `team_id` bigint(20) NOT NULL COMMENT '小组ID',
                                              `score` int(11) NULL DEFAULT 0 COMMENT '该次子轮中的得分',
                                              `comment` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '老师对该次表现的评语',
                                              `deadline` datetime NULL DEFAULT NULL COMMENT '最晚提交时间',
                                              `gmt_create` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                              `gmt_update` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                              PRIMARY KEY (`id`) USING BTREE,
                                              UNIQUE INDEX `uq_game_round_sub_team`(`game_id`, `round`, `sub_round`, `team_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '提案赛每轮子对局小组得分表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for role
-- ----------------------------
DROP TABLE IF EXISTS `role`;
CREATE TABLE `role`  (
                         `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
                         `role_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '角色名称',
                         `role_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '角色关键词(权限认证字段)',
                         `gmt_create` datetime(6) NULL DEFAULT NULL,
                         `gmt_modified` datetime(6) NULL DEFAULT NULL,
                         `create_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
                         `update_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
                         `is_deleted` tinyint(1) NULL DEFAULT 0,
                         PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for role_permission
-- ----------------------------
DROP TABLE IF EXISTS `role_permission`;
CREATE TABLE `role_permission`  (
                                    `id` bigint(20) NOT NULL AUTO_INCREMENT,
                                    `role_id` bigint(20) NOT NULL,
                                    `permission_id` bigint(20) NOT NULL,
                                    `gmt_create` datetime(6) NULL DEFAULT NULL COMMENT '创建时间',
                                    `gmt_modified` datetime(6) NULL DEFAULT NULL COMMENT '修改时间',
                                    `create_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建者',
                                    `update_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '修改者',
                                    `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除',
                                    `deleted_id` bigint(20) NULL DEFAULT NULL COMMENT '删除id',
                                    PRIMARY KEY (`id`) USING BTREE,
                                    INDEX `per_role`(`permission_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for student_score_log
-- ----------------------------
DROP TABLE IF EXISTS `student_score_log`;
CREATE TABLE `student_score_log`  (
                                      `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '日志ID',
                                      `student_id` bigint(20) NOT NULL COMMENT '学生ID',
                                      `team_id` bigint(20) NOT NULL COMMENT '所属小组ID',
                                      `game_id` bigint(20) NOT NULL COMMENT '所属游戏ID',
                                      `score` int(11) NOT NULL COMMENT '本次得分',
                                      `reason` tinyint(4) NOT NULL COMMENT '原因(1：老师加分，2：老师扣分，3：学习通导入成绩)',
                                      `round` int(11) NULL DEFAULT NULL COMMENT '轮次（可选）',
                                      `phase` tinyint(4) NULL DEFAULT NULL COMMENT '游戏阶段：1-棋盘赛，2-提案赛',
                                      `gmt_create` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '得分时间',
                                      `comment` varchar(655) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '评论',
                                      PRIMARY KEY (`id`) USING BTREE,
                                      INDEX `idx_student_score_log_game`(`game_id`) USING BTREE,
                                      INDEX `idx_student_score_log_student`(`student_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 859 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '个人得分日志表：记录学生每次得分及原因' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for team
-- ----------------------------
DROP TABLE IF EXISTS `team`;
CREATE TABLE `team`  (
                         `id` bigint(20) NOT NULL COMMENT '小组ID',
                         `game_id` bigint(20) NOT NULL COMMENT '所属游戏ID',
                         `leader_id` bigint(20) NOT NULL COMMENT '组长学生ID',
                         `leader_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '组长姓名',
                         `sno` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '组长学号',
                         `total_members` int(11) NOT NULL COMMENT '总人数',
                         `member_score_sum` int(11) NULL DEFAULT 0 COMMENT '棋盘赛 - 成员得分',
                         `board_score_adjusted` int(11) NULL DEFAULT 0 COMMENT '棋盘赛 - 老师手动调整分数',
                         `proposal_score_imported` int(11) NULL DEFAULT 0 COMMENT '提案赛 - 系统计算总分',
                         `proposal_score_adjusted` int(11) NULL DEFAULT 0 COMMENT '提案赛 - 老师手动调整分数',
                         `gmt_create` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                         `gmt_update` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                         `alive` tinyint(4) NOT NULL DEFAULT 1 COMMENT '是否存活（0：淘汰，1：存活）',
                         `eliminated_time` datetime NULL DEFAULT NULL COMMENT '淘汰时间',
                         PRIMARY KEY (`id`, `game_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '小组表：不再使用统一总分字段，改用按来源区分的积分记录' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for team_member
-- ----------------------------
DROP TABLE IF EXISTS `team_member`;
CREATE TABLE `team_member`  (
                                `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '记录ID',
                                `team_id` bigint(20) NOT NULL COMMENT '所属小组ID',
                                `student_id` bigint(20) NOT NULL COMMENT '学生ID',
                                `student_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '学生姓名',
                                `is_leader` tinyint(1) NULL DEFAULT 0 COMMENT '是否为组长',
                                `individual_score` int(11) NULL DEFAULT 0 COMMENT '个人得分',
                                `gmt_create` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
                                `sno` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '学号',
                                `game_id` bigint(20) NOT NULL COMMENT '游戏id',
                                PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1395 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '小组成员表：支持灵活扩展、个体得分、动态统计' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for team_score_log
-- ----------------------------
DROP TABLE IF EXISTS `team_score_log`;
CREATE TABLE `team_score_log`  (
                                   `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '日志ID',
                                   `team_id` bigint(20) NOT NULL COMMENT '小组ID',
                                   `game_id` bigint(20) NOT NULL COMMENT '所属游戏ID',
                                   `score` int(11) NOT NULL COMMENT '本次变动的分数',
                                   `reason` tinyint(4) NOT NULL COMMENT '原因(1：老师加分，2：老师扣分，3：学习通导入成绩)',
                                   `round` int(11) NULL DEFAULT NULL COMMENT '游戏中的轮次（可选）',
                                   `phase` tinyint(4) NULL DEFAULT NULL COMMENT '游戏阶段：1-棋盘赛，2-提案赛',
                                   `gmt_create` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '得分时间',
                                   `comment` varchar(655) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '评论',
                                   `submit_time` datetime NULL DEFAULT NULL COMMENT '最晚提交时间',
                                   PRIMARY KEY (`id`) USING BTREE,
                                   INDEX `idx_team_score_log_game`(`game_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 295 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '小组得分日志表：记录每次得分及来源' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for team_tile_action
-- ----------------------------
DROP TABLE IF EXISTS `team_tile_action`;
CREATE TABLE `team_tile_action`  (
                                     `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '记录ID',
                                     `game_id` bigint(20) NOT NULL COMMENT '游戏ID',
                                     `team_id` bigint(20) NOT NULL COMMENT '小组ID',
                                     `round` int(11) NOT NULL COMMENT '第几轮',
                                     `phase` tinyint(4) NOT NULL COMMENT '阶段：1=选格子，2=效果阶段',
                                     `all_tiles` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '这一轮所有格子编号（逗号分隔）',
                                     `blind_box_tiles` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '盲盒秘境格子编号（逗号分隔）',
                                     `fortress_tiles` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '决斗要塞格子编号（逗号分隔）',
                                     `gold_center_tiles` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '黄金中心格子编号（逗号分隔）',
                                     `opportunity_tiles` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '机会宝地格子编号（逗号分隔）',
                                     `original_tile_count` int(11) NULL DEFAULT NULL COMMENT '该轮选的原始格子数量',
                                     `settled_tile_count` int(11) NULL DEFAULT NULL COMMENT '该轮结算后剩余格子数量',
                                     `gmt_create` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
                                     `gmt_update` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
                                     `selected` tinyint(1) NULL DEFAULT 0 COMMENT '是否已选择（0-未选择，1-已选择）',
                                     PRIMARY KEY (`id`) USING BTREE,
                                     UNIQUE INDEX `uk_game_team_round_phase`(`game_id`, `team_id`, `round`, `phase`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 314 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '小组格子行动汇总表：每轮一条记录，存储该轮所有格子及状态' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
                         `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '用户id',
                         `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户名',
                         `password` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '密码',
                         `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '用户昵称',
                         `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '头像',
                         `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '邮箱地址',
                         `phone` varchar(15) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '手机号',
                         `type` tinyint(4) NOT NULL COMMENT '用户类型(0：普通老师 -1:超级管理老师)',
                         `state` tinyint(4) NULL DEFAULT 1 COMMENT '用户状态(0：禁用 1：启用，默认为1)',
                         `gmt_create` datetime(6) NULL DEFAULT NULL COMMENT '创建时间',
                         `gmt_modified` datetime(6) NULL DEFAULT NULL COMMENT '更新时间',
                         `create_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建者',
                         `update_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '更新者',
                         `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除(0：未删除 1：已删除)',
                         PRIMARY KEY (`id`) USING BTREE,
                         UNIQUE INDEX `phone`(`phone`) USING BTREE,
                         INDEX `username`(`username`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for user_role
-- ----------------------------
DROP TABLE IF EXISTS `user_role`;
CREATE TABLE `user_role`  (
                              `id` bigint(20) NOT NULL AUTO_INCREMENT,
                              `user_id` bigint(20) NOT NULL,
                              `role_id` bigint(20) NOT NULL,
                              `gmt_create` datetime(6) NULL DEFAULT NULL COMMENT '创建时间',
                              `gmt_modified` datetime(6) NULL DEFAULT NULL COMMENT '修改时间',
                              `create_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建者',
                              `update_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '修改者',
                              `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除',
                              PRIMARY KEY (`id`) USING BTREE,
                              INDEX `role_user`(`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;

SET FOREIGN_KEY_CHECKS = 1;
