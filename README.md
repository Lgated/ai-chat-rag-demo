# 对话大模型+RAG

## 📖 项目简介

本项目是一个基于 **Spring Boot 3 + Spring AI + React** 的完整 AI 对话/知识库教学项目，演示如何在真实应用中集成和使用 Spring AI 的各种能力。

### 核心特性

- ✅ **大模型对话（Chat）**：基于 Spring AI 的 `ChatClient` 实现智能对话
- ✅ **SSE 流式输出**：完整的前后端流式响应链路，实时显示 AI 回答
- ✅ **Agent 智能体 / FunctionCall**：工具调用能力，智能选择和执行外部工具
- ✅ **Embedding 向量化**：文本向量生成与存储
- ✅ **向量数据库检索**：基于 pgvector 的相似度搜索
- ✅ **RAG 检索增强生成**：文档切分、向量检索、上下文构建
- ✅ **历史消息管理**：会话级别的消息历史与上下文记忆
- ✅ **文档上传与管理**：支持 PDF/Word/Excel/PPT/TXT/Markdown 等多种格式
- ✅ **文档引用展示**：RAG 回答时显示引用的文档片段和来源
- ✅ **图片生成**：基于多模态模型的图片生成能力
- ✅ **图片理解**：图片内容识别与问答

---

## 🎯 功能一览

### 后端功能（Spring Boot + Spring WebFlux + Spring AI）

#### 基础对话
- 普通问答接口（同步 & SSE 流式）
- 会话创建、会话列表、历史消息查询
- 消息持久化存储

#### RAG 能力
- **文本 Embedding 生成**：使用 DashScope `text-embedding-v2` 模型
- **文档切分（Chunking）**：按固定大小切分文档，支持重叠策略
- **向量相似度检索**：基于 pgvector 的高效检索
- **基于文档的回答**：将检索到的文档片段注入 Prompt，生成基于知识的回答

#### Agent / FunctionCall
- **Agent 智能体**：根据用户意图自动选择工具
- **FunctionCall 工具调用**：示例包括天气查询、知识库检索等
- **工具执行结果注入**：自动将工具执行结果注入到大模型上下文

#### 流式输出（SSE）
- Spring WebFlux + `Flux<String>` 流式返回
- Controller 端使用 `ServerSentEvent` 封装事件
- 前端使用 `EventSource` / `fetch + ReadableStream` 解析流
- 支持 POST + SSE 解决长文本请求的 URI 长度限制

#### 文档上传与知识库管理
- **支持格式**：PDF / Word / Excel / PPT / TXT / Markdown
- **文档解析**：使用 Apache Tika 自动识别并提取文本内容
- **元信息存储**：Document 表存储文件名、类型、大小等元信息
- **向量存储**：DocumentChunk 表存储文档分片 + 向量字段（pgvector）
- **引用展示**：RAG 回答时展示「引用片段 + 文档名」

#### 图片生成 / 图片理解（多模态）
- **图片生成**：基于 DashScope/OpenAI 兼容接口的图片生成
- **图片理解**：将图片转为描述，或基于图片内容进行问答

### 前端功能（React + Vite + TypeScript）

#### 会话管理
- **会话列表**：左侧显示所有会话，支持创建新会话
- **聊天窗口**：右侧显示对话历史，支持流式显示 AI 回答
- **模式切换**：普通对话 / RAG 知识库 / Agent 工具三种模式

#### 文档管理
- **顶部 Tab 切换**：`对话` / `文档管理` 两个标签页
- **文档上传**：拖拽或选择文件上传，支持添加描述
- **文档列表**：显示文件名、类型、大小、描述、上传时间
- **文档删除**：支持删除文档（同时删除相关向量数据）
- **引用展示**：RAG 回答时在消息下方展示引用来源

#### 用户体验优化
- **乐观更新**：发送消息时立即显示在界面
- **自动滚动**：AI 回复时自动滚动到底部
- **流式显示**：实时显示 AI 生成的内容，带“思考中…”提示
- **错误处理**：完善的错误提示和异常处理

---

## 📚 知识点梳理

### Spring AI 基础
- **ChatClient 使用**：同步调用（`.call().content()`）与流式调用（`.stream().content()`）
- **EmbeddingModel 配置**：文本向量生成与向量字符串转换
- **DashScope 兼容模式**：使用 OpenAI 兼容接口配置 DashScope 模型

### WebFlux & SSE 流式
- **Flux<String> 流式返回**：Spring WebFlux 响应式流处理
- **ServerSentEvent 封装**：使用 SSE 标准格式返回流式数据
- **前端流式解析**：`EventSource` 原生 API 与 `fetch + ReadableStream` 方案
- **POST + SSE**：解决长文本请求的 URI 长度限制问题

### RAG / Embedding / 向量数据库
- **文本分片策略**：chunk size 与 overlap 的平衡
- **Embedding 存储**：pgvector 的 `vector` 类型
- **相似度检索**：pgvector 的 `<->` 距离运算符
- **上下文构建**：将检索到的文档片段拼接到 Prompt 中

### Agent / FunctionCall
- **Tool 接口定义**：实现 `Tool` 接口定义外部工具
- **FunctionCall 机制**：大模型自动选择并调用工具
- **工具执行与结果注入**：工具执行后自动将结果写回上下文

### 会话与历史消息
- **实体关联设计**：`Conversation` 与 `Message` 的一对多关系
- **历史消息拼接**：将最近 N 条消息拼接到 Prompt 中
- **Token 限制处理**：历史消息截断策略，防止超出模型上下文长度

### 多模态（图片）
- **图片生成**：Prompt → 图片生成模型 → URL/Base64 返回
- **图片理解**：图片 + 问题 → 多模态模型 → 描述/答案

### 前端工程实践
- **React Hooks**：`useState`、`useEffect`、`useRef` 的使用
- **流式消息处理**：实时更新 UI，处理 SSE 事件流
- **API 封装**：模块化的 API 调用封装（`api.ts`）
- **路由/页面切换**：简单的页面级路由或 Tab 切换

---

## 📝 文档地址


- **项目文档**（docs 目录）
  - `docs/后续开发计划.md`
  - `docs/文档上传和引用功能实现方案.md`
  - `docs/RAG引用信息返回方案.md`
  - `docs/完整代码实现.md`

---

## 🛠️ 运行环境

### 必需环境
- **JDK**：17+
- **Node.js**：18+
- **包管理工具**：npm / pnpm / yarn（推荐使用 npm）

### 数据库
- **MySQL 8** 或 **PostgreSQL + pgvector**（存储会话和消息数据）

### 大模型平台
- **DashScope API KEY**（或其他 OpenAI 兼容平台）

---

## 🐳 基础环境搭建


### . PostgreSQL + pgvector（向量数据库）

```bash
docker run -d \
  --name pg-ai \
  -e POSTGRES_PASSWORD=xxxxx \
  -e POSTGRES_DB=ai_chat \
  -p 5432:5432 \
  pgvector/pgvector:pg16
```

进入容器并启用 pgvector 扩展：

```bash
docker exec -it pg-ai psql -U postgres -d ai_chat

# 在 psql 中执行
CREATE EXTENSION IF NOT EXISTS vector;
```

---

## ⚙️ 配置说明

### 后端配置 `application.yml`

在 `src/main/resources/application.yml` 中配置以下内容：

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ai_chat
    username: xxx
    password: xxx
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: update   # 开发阶段自动建表
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.PostgreSQLDialect

  # Spring AI 配置（DashScope 兼容模式）
  ai:
    openai:
      api-key: sk-xxxxxx  # 替换为你的 DashScope API KEY
      base-url: https://dashscope.aliyuncs.com/compatible-mode
      chat:
        options:
          model: qwen1.5-110b-chat
          temperature: 0.7
      embedding:
        options:
          model: text-embedding-v2

  # 文件上传配置
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 50MB

# 应用配置
app:
  upload:
    dir: uploads  # 文档上传目录
```

### 前端配置

前端 API 地址在 `frontend/src/api.ts` 中配置：

```typescript
const api = axios.create({
  baseURL: 'http://localhost:8080/api/chat',  // 后端 API 地址
  headers: {
    'Content-Type': 'application/json',
  },
});
```

---

## 🚀 运行步骤

### 1. 克隆代码

```bash
git clone https://github.com/你的账号/你的仓库名.git
cd 你的仓库名
```

### 2. 后端启动（IDEA）

1. **使用 IntelliJ IDEA 打开项目根目录**
2. **等待依赖下载完成**（Maven 或 Gradle）
3. **修改配置文件**
   - 打开 `src/main/resources/application.yml`
   - 配置 DashScope API KEY
   - 配置数据库连接（PostgreSQL/MySQL）
   - 配置 Redis-Stack / Neo4j（如使用）

4. **处理生成的源码目录**（如使用 MapStruct 等）
   - 在 `target/generated-sources/annotations` 右键
   - 选择 `Mark Directory as` → `Generated Sources Root`

5. **运行启动类**
   - 找到 `src/main/java/com/example/demo/TestEasyJavaApplication.java`
   - 右键 → `Run 'TestEasyJavaApplication'`

后端启动成功后，默认监听：**`http://localhost:8080`**

### 3. 前端启动

```bash
cd frontend

# 安装依赖
npm install

# 启动开发服务器（确保后端已启动在 8080 端口）
npm run dev
```

前端启动成功后，默认访问：**`http://localhost:5174`**

---

## 📖 使用说明

### 对话功能

1. **创建会话**：点击左侧 `+ 新会话` 按钮
2. **选择模式**：
   - **普通对话**：直接与大模型对话
   - **RAG 知识库**：基于上传的文档回答问题（会显示引用来源）
   - **Agent 工具**：使用工具增强的智能对话
3. **发送消息**：在输入框输入问题，点击发送或按 Enter
4. **查看回答**：AI 回答会以流式方式实时显示

### 文档管理

1. **切换到文档管理**：点击顶部 `📚 文档管理` 标签
2. **上传文档**：
   - 点击文件选择按钮或拖拽文件
   - 支持格式：PDF、Word、Excel、PPT、TXT、Markdown
   - 可选择填写文档描述
   - 点击 `上传文档` 按钮
3. **查看文档列表**：上传成功后，文档会自动显示在列表中
4. **删除文档**：点击文档列表中的 `删除` 按钮

### RAG 问答

1. **切换到 RAG 模式**：在对话页面选择 `📚 RAG知识库` 模式
2. **提问**：输入与已上传文档相关的问题
3. **查看引用**：AI 回答下方会显示引用的文档片段和来源

---

## 🎓 技术栈

### 后端
- **Spring Boot 3.3.0**
- **Spring AI 1.0.0-M4**
- **Spring WebFlux**（响应式编程）
- **Spring Data JPA**
- **PostgreSQL + pgvector**（向量数据库）
- **Apache Tika**（文档解析）
- **Lombok**（简化代码）

### 前端
- **React 19**
- **TypeScript 5.9**
- **Vite 7**（构建工具）
- **Axios**（HTTP 客户端）
- **EventSource / Fetch API**（SSE 流式处理）

---

## 📂 项目结构

```
项目根目录/
├── src/main/java/com/example/demo/
│   ├── config/              # 配置类（AiConfig, CorsConfig）
│   ├── controller/          # REST 控制器
│   ├── domain/              # 实体类（Conversation, Message, Document, DocumentChunk）
│   ├── repository/          # 数据访问层
│   ├── service/             # 业务逻辑接口
│   │   └── Impl/           # 业务逻辑实现
│   └── common/              # 通用类（Result, Exception）
├── src/main/resources/
│   └── application.yml      # 应用配置
├── frontend/
│   ├── src/
│   │   ├── api.ts          # API 封装
│   │   ├── pages/          # 页面组件
│   │   │   ├── ChatPage.tsx
│   │   │   └── DocumentPage.tsx
│   │   └── App.tsx         # 根组件
│   └── package.json
└── docs/                    # 项目文档
```

---

## 🔮 后续扩展建议

- [ ] 接入更多大模型（OpenAI、DeepSeek、Moonshot 等）
- [ ] 多租户 / 多知识库切换功能
- [ ] Graph RAG 图可视化页面
- [ ] 文档存储改为 MinIO/OSS 对象存储
- [ ] 增加权限系统与用户登录
- [ ] 支持流式对话的中断功能
- [ ] 对话导出（PDF/Word/JSON）
- [ ] 知识库的版本管理


---

## 👥 贡献

欢迎提交 Issue 和 Pull Request！

---

## 📮 联系方式

> 2659294465@qq.com

---

**⭐ 如果这个项目对你有帮助，欢迎 Star！**

