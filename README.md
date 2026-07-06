# IEC104 协议测试工具

基于开源库 [j60870](https://github.com/openmuc/j60870) 开发的 IEC 60870-5-104 协议测试工具，提供图形化界面，支持客户端与服务端双模式，适用于 IEC 104 协议的学习、调试与测试。

**当前版本：v1.0.0**

## 功能特性

### 客户端模式
- **多连接管理**：类似 Navicat 的连接列表，可同时管理多个 IEC 104 服务端连接
- **报文监控**：实时捕获并展示交互的 IEC 104 报文（ASDU 解析）
- **数据视图**：按数据类型分类展示接收到的遥测、遥信数据
- **命令发送**：支持单点命令、设定值命令等控制操作
- **连接配置**：可配置 IP、端口、公共地址（CASDU）、IOA 范围等参数

### 服务端模式
- **模拟 IEC 104 服务端**：监听指定端口，响应客户端连接
- **数据点管理**：支持添加、编辑、批量导入数据点
- **总召唤响应**：自动响应客户端的总召唤请求
- **突发数据发送**：支持手动或定时发送突发数据
- **报文捕获**：记录服务端收发的所有报文

### 通用功能
- **报文保存/加载**：支持 JSON Lines 格式保存和加载报文记录
- **CSV 导出**：将报文记录导出为 CSV 文件便于分析
- **报文详情**：点击报文查看完整的 ASDU 字段解析

## 技术栈

| 组件 | 说明 |
|------|------|
| Java | JDK 1.8+ |
| j60870 | IEC 60870-5-104 协议核心库（已内嵌源码） |
| Swing | 图形用户界面 |
| Maven | 项目构建与依赖管理 |
| Logback | 日志框架 |
| Gson | JSON 数据处理 |
| Lombok | 简化 Java 代码 |

## 快速开始

### 环境要求

- JDK 1.8 或以上
- Maven 3.x（用于构建）

### 运行方式

本项目是 Java Swing 桌面应用，**并非只能以 JAR 包运行**，支持以下三种方式：

#### 方式一：IDE 中直接运行（推荐开发调试）

在 IntelliJ IDEA 中打开项目，直接运行主类 `com.iec104tester.Main` 即可启动图形界面。

#### 方式二：Maven 命令运行

```bash
mvn compile exec:java -Dexec.mainClass="com.iec104tester.Main"
```

#### 方式三：打包为 JAR 运行（推荐分发部署）

```bash
# 编译打包（生成包含所有依赖的 fat JAR）
mvn clean package

# 运行
java -jar target/iec104-tester-1.0.0.jar
```

> 项目使用 `maven-shade-plugin` 打包，生成的 JAR 包含所有依赖，无需额外配置 classpath，双击或命令行均可运行。

## 项目结构

```
IEC104协议测试工具/
├── pom.xml                          # Maven 构建配置
├── src/main/
│   ├── java/com/iec104tester/
│   │   ├── Main.java                # 程序入口
│   │   ├── core/                    # 核心业务逻辑
│   │   │   ├── ClientManager.java   # 客户端连接管理
│   │   │   ├── ServerManager.java   # 服务端生命周期管理
│   │   │   ├── ServerDataModel.java # 服务端数据模型
│   │   │   └── AsduDecoder.java     # ASDU 报文解码器
│   │   ├── ui/                      # 图形界面
│   │   │   ├── MainFrame.java       # 主窗口
│   │   │   ├── ClientPanel.java     # 客户端面板（多连接）
│   │   │   ├── ServerPanel.java     # 服务端面板
│   │   │   ├── MessageTablePanel.java   # 报文列表
│   │   │   ├── MessageDetailPanel.java  # 报文详情
│   │   │   └── ...                  # 各类对话框
│   │   ├── capture/                 # 报文捕获与存储
│   │   └── model/                   # 数据模型
│   └── resources/
│       └── logback.xml              # 日志配置
└── j60870/                          # j60870 协议库源码
```

## 版本历史

| 版本 | 日期 | 说明 |
|------|------|------|
| v1.0.0 | 2026-07 | 初始版本，实现客户端/服务端双模式、报文捕获与解析、多连接管理 |

## 后续规划

- [ ] 报文回放功能
- [ ] 协议异常场景模拟
- [ ] 支持更多 ASDU 类型解析
- [ ] 连接配置导入/导出
- [ ] 暗色主题支持

## 许可证

本项目基于 j60870 开源库开发，遵循其开源协议。
