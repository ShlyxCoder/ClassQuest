![Java](https://img.shields.io/badge/Java-17+-ed8b00?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.0+-6db33f?style=flat-square&logo=springboot&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.0+-4479a1?style=flat-square&logo=mysql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-6.0+-dc382d?style=flat-square&logo=redis&logoColor=white)
![Spring AI](https://img.shields.io/badge/Spring%20AI-Latest-6db33f?style=flat-square)
![License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)
# 🎓 ClassQuest - 数字化互动教学平台

## 📖 项目简介

**ClassQuest**是一个综合性的数字化互动教学平台，旨在通过游戏化和协作学习提升课堂参与度。平台结合了棋盘游戏机制与提案式学习活动，创造沉浸式教育体验，促进团队合作、批判性思维和主动参与。

## ✨ 核心功能

### 🎮 棋盘游戏模块
* **多团队跳棋式游戏**: 各小组从棋盘不同端点出发，向中心移动  
* **特殊效果格子**: 踩到特定格子触发特殊效果，影响计分，包括领地割让  
* **多轮次系统**: 四轮游戏，累计计分  
* **持久化游戏状态**: 跨课程无缝恢复游戏进度  
* **实时位置追踪**: 实时更新小组位置和得分  

<p align="center">
  <img src="https://github.com/user-attachments/assets/fc9fe788-e510-46dd-b803-3e653f1ea6b1" width="600" title="棋盘赛效果展示"/>
</p>

<p align="center">
  <img src="https://github.com/user-attachments/assets/f8e3d4ad-2922-41af-9e86-6e05d8ff9fd1" width="600" title="棋盘赛效果展示"/>
</p>

### 💡 提案风暴模块

平台设有完整的三阶段提案投票系统：

**第一轮：淘汰赛**
* 每小轮指定几个小组发出提案
* 提案内容：决定与哪些小组竞争（部分小组可轮空）
* 提案内容：选择积分池分配方式
* 全体小组对竞争方案进行投票
* 获胜提案确定本轮竞争规则和积分分配机制

**第二轮：辩论赛**
* 小组提出新的竞争组合和积分分配提案
* 针对不同提案进行结构化辩论
* 基于辩论表现和同伴评价进行投票
* 获胜提案确定下一轮游戏规则

**第三轮：抢答赛**
* 快节奏问答竞赛环节
* 小组抢答问题获得积分
* 速度和准确性双重加分
* 根据之前提案确定的规则进行最终积分结算

<p align="center">
  <img src="https://github.com/user-attachments/assets/40da1658-dc8d-42a9-b4d4-0da893274cd4" width="600" title="提案赛效果展示"/>
</p>

<p align="center">
  <img src="https://github.com/user-attachments/assets/7627fc0c-5dfb-499e-b641-382ae62f9856" width="600" title="提案赛效果展示"/>
</p>

### 📊 综合计分系统
* **手动调分功能**: 教师可对个人或小组进行加减分
* **Excel成绩导入**: 通过EasyExcel批量导入作业/测验成绩
* **多维度计分**: 跟踪不同活动类型的表现
* **得分日志记录**: 完整的计分事件审计追踪
* **实时排行榜**: 活动过程中的实时排名更新
<p align="center">
  <img src="https://github.com/user-attachments/assets/36ca9257-e615-4676-9493-c34f8f4d10eb" width="600" title="排名展示"/>
</p>

### 🤖 AI智能分析
* **Spring AI集成**: 先进的AI学生表现模式分析
* **个人画像**: 个性化学习洞察和建议
* **团队动力分析**: 小组协作效果评估
* **表现可视化**: 全面的图表和报告
* **预测性学习洞察**: AI驱动的改进建议
<p align="center">
  <img src="https://github.com/user-attachments/assets/08f326fc-ca36-46f8-93ea-0d935ee206dd" width="600" title="AI分析展示"/>
</p>

## 🏗️ 技术架构

### 后端技术栈
* **框架**: Spring Boot 3.x
* **数据库**: MySQL 8.0 + Redis 6.0 缓存
* **认证授权**: Sa-Token 权限认证框架
* **文件处理**: EasyExcel 处理Excel导入导出
* **AI集成**: Spring AI 框架
* **开发语言**: Java 17

### 核心组件
* **游戏状态管理**: 实时游戏会话持久化
* **投票系统**: 安全的提案提交和投票机制
* **计分引擎**: 多源计分聚合和计算
* **分析引擎**: AI驱动的学生表现分析

## 🎯 教育价值

### 对教师的价值
* **参与度分析**: 详细的学生参与洞察
* **灵活评估**: 一个平台多种评价方式
* **效率提升**: 自动化计分和排名系统
* **数据驱动**: AI驱动的教学建议

### 对学生的价值
* **主动学习**: 动手参与而非被动听讲
* **协作技能**: 团队活动促进合作能力
* **批判思维**: 提案和辩论环节鼓励分析思考
* **即时反馈**: 实时计分和表现洞察

## 📋 API接口文档

平台提供完整的RESTful API接口：
* 游戏房间管理和棋盘状态
* 提案提交和投票流程
* 计分管理和Excel导入导出
* 用户认证和团队管理
* 分析数据检索

启动后端服务后访问 `/doc.html` 查看交互式API文档。

## 📧 联系方式

如有问题或需要支持，请联系：
* **邮箱**: 40505282@qq.com

**前端代码仓库：** 本项目前后端分离，前端代码由另一位同事维护。  
点击访问前端代码：[https://github.com/OuterCyrex/digital-edu-platform.git](https://github.com/OuterCyrex/digital-edu-platform.git)
---
*用游戏化的方式，构建互动教育的未来* 🎓✨
