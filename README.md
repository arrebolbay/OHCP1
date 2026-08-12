# 🌾 农业病虫害智能问答助手 (OHCP)

> **OHCP** — Online Pest & Herbicide Consultation Platform

基于 **Spring Boot + Lucene + RAG** 架构的农业病虫害智能问答系统，支持自然语言查询、症状诊断和知识库浏览，覆盖水稻、小麦、玉米、棉花、蔬菜、果树等主要农作物的病虫害防治知识。

---

## ✨ 功能特性

- 🔍 **RAG 智能检索**：Lucene 全文检索引擎（SmartChineseAnalyzer 中文分词）+ JPA 关键词混合匹配
- 🩺 **症状诊断**：根据症状描述推测可能的病虫害类型，给出诊断建议
- 🌾 **多作物覆盖**：知识库覆盖水稻、小麦、玉米、棉花、蔬菜、果树等常见作物
- 📚 **知识分类**：按真菌性病害、细菌性病害、病毒性病害、虫害、草害等分类组织
- 🎯 **精准回答**：返回病原体、分类地位、症状、发生规律、传播途径、防治方案（农业/化学/生物）等完整信息
- 🖥️ **现代 Web 界面**：响应式单页面设计，支持桌面和移动端访问

---

## 🏗 技术架构

```
┌─────────────────────────────────────────┐
│              用户提问 / 症状描述           │
└─────────────────┬───────────────────────┘
                  ▼
┌─────────────────────────────────────────┐
│           RAG 检索增强生成引擎            │
│  ┌─────────────┐  ┌───────────────────┐  │
│  │  Lucene     │  │  JPA 关键词匹配    │  │
│  │  全文检索    │  │  (HSQL LIKE查询)  │  │
│  └──────┬──────┘  └───────┬───────────┘  │
│         └────────┬─────────┘              │
│                  ▼                       │
│           综合评分排序 → Top-K 结果       │
└─────────────────┬───────────────────────┘
                  ▼
┌─────────────────────────────────────────┐
│         Spring Boot REST API             │
│      /api/qa  |  /api/diagnose           │
│      /api/knowledge/crop/{crop}          │
│      /api/knowledge/category/{category}  │
└─────────────────┬───────────────────────┘
                  ▼
┌─────────────────────────────────────────┐
│          H2 嵌入式数据库 + JSON           │
│      (知识入库存储 + 文件备份)              │
└─────────────────────────────────────────┘
```

| 层次 | 技术选型 |
|------|----------|
| 后端框架 | Spring Boot 3.2.5 |
| 持久层 | Spring Data JPA + H2 嵌入式数据库 |
| 全文检索 | Apache Lucene 9.10 + SmartChineseAnalyzer |
| Java 版本 | JDK 17 |
| 构建工具 | Maven |
| 前端 | 原生 HTML5 + CSS3 + JavaScript（SPA） |

---

## 📂 项目结构

```
OHCP/
├── src/main/java/com/pestqa/ohcp/
│   ├── OhcpApplication.java           # Spring Boot 启动类
│   ├── controller/
│   │   └── PestQAController.java      # REST API 控制器
│   ├── entity/
│   │   └── PestKnowledge.java         # 病虫害知识实体
│   ├── repository/
│   │   └── PestKnowledgeRepository.java # JPA 数据访问层
│   └── service/
│       ├── RagSearchService.java      # RAG 检索服务（Lucene+JPA）
│       └── DataInitializer.java       # 数据初始化（JSON→数据库）
├── src/main/resources/
│   ├── application.properties         # 应用配置
│   └── knowledge-base.json            # 病虫害知识库（30+条目）
├── pom.xml                            # Maven 配置
├── index.html                         # 前端界面
├── 启动系统.bat                        # Windows 一键启动
├── compile.bat                        # 编译脚本
└── challenge_1_病虫害问答助手(1).pdf    # 挑战赛要求文档
```

---

## 🚀 快速开始

### 环境要求

- **JDK 17** 或更高版本
- **Maven 3.6+**

### 方式一：一键启动（Windows）

双击 `启动系统.bat`，自动编译并启动服务。

### 方式二：手动构建运行

```bash
# 1. 编译打包
mvn clean package -DskipTests

# 2. 启动服务
java -jar target/ohcp-pest-qa-1.0.0.jar
```

启动后访问：**http://localhost:8080/index.html**

### 方式三：Maven 直接运行

```bash
mvn spring-boot:run
```

---

## 📡 API 文档

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/qa` | 智能问答（body: `{question: "..."}`） |
| `POST` | `/api/diagnose` | 症状诊断（body: `{symptoms: "..."}`） |
| `GET` | `/api/knowledge/crop/{crop}` | 按作物查询（如 `水稻`、`小麦`） |
| `GET` | `/api/knowledge/category/{category}` | 按分类查询（病害/虫害/草害） |
| `GET` | `/api/knowledge/stats` | 知识库统计 |
| `GET` | `/api/knowledge-base` | 获取完整知识库 |

### 问答请求示例

```bash
curl -X POST http://localhost:8080/api/qa \
  -H "Content-Type: application/json" \
  -d '{"question": "水稻纹枯病如何防治"}'
```

### 诊断请求示例

```bash
curl -X POST http://localhost:8080/api/diagnose \
  -H "Content-Type: application/json" \
  -d '{"symptoms": "叶片出现褐色斑点，边缘黄色"}'
```

---

## 📚 知识库覆盖

| 作物 | 覆盖病虫害 |
|------|----------|
| 🌾 水稻 | 纹枯病、稻瘟病、稻飞虱、稻纵卷叶螟、稻水象甲等 |
| 🌿 小麦 | 赤霉病、条锈病、蚜虫等 |
| 🌽 玉米 | 玉米螟、大斑病等 |
| 🧶 棉花 | 棉铃虫、枯萎病等 |
| 🥬 蔬菜 | 菜青虫、霜霉病等 |
| 🍎 果树 | 食心虫、轮纹病等 |

每条知识包含：**病原体/害虫名称**、**分类地位**、**危害症状**、**发生规律**、**传播途径**、**农业防治**、**化学防治**、**生物防治** 和 **综合预防措施**。

---

## 🔧 配置说明

核心配置位于 `src/main/resources/application.properties`：

```properties
# 服务端口
server.port=8080

# H2 数据库（文件存储，重启不丢失）
spring.datasource.url=jdbc:h2:file:./data/ohcp_db

# JPA 自动建表
spring.jpa.hibernate.ddl-auto=update

# RAG 配置
ohcp.rag.top-k=5                        # 检索返回数量
ohcp.rag.rebuild-index-on-startup=true   # 启动时重建索引
```

---

## 📝 开发说明

### RAG 检索流程

1. **Lucene 全文索引**：使用 SmartChineseAnalyzer 对知识库进行中文分词和索引
2. **多字段加权检索**：对 question、aliases、symptoms、pathogen 等字段加权搜索
3. **JPA 辅助匹配**：对 Lucene 可能遗漏的结果进行 SQL LIKE 补充检索
4. **综合排序**：合并两路结果，按相关性得分排序返回 Top-K

### 数据初始化

- 首次启动时自动从 `knowledge-base.json` 导入数据到 H2 数据库
- 数据库已有数据时自动跳过初始化
- 删除 `data/` 目录即可重置数据库

---

## 📄 License

MIT © 2025
