# Git 和 GitHub 使用指南

## 📋 目录
1. [初次设置（一次性）](#初次设置一次性)
2. [首次提交到 GitHub](#首次提交到-github)
3. [日常开发提交流程](#日常开发提交流程)
4. [常用 Git 命令](#常用-git-命令)
5. [常见问题解决](#常见问题解决)

---

## 🚀 初次设置（一次性）

### 步骤 1：检查是否已安装 Git

打开 PowerShell 或 CMD，执行：

```bash
git --version
```

**如果没有安装**，去 [Git 官网](https://git-scm.com/download/win) 下载安装 Windows 版本。

### 步骤 2：配置 Git 用户信息（首次使用需要）

```bash
git config --global user.name "你的GitHub用户名"
git config --global user.email "你的GitHub邮箱"
```

例如：
```bash
git config --global user.name "zhangsan"
git config --global user.email "zhangsan@example.com"
```

### 步骤 3：初始化本地 Git 仓库

**在项目根目录执行**（`d:\java\data\test_easyJava`）：

```bash
cd d:\java\data\test_easyJava
git init
```

这会创建一个 `.git` 文件夹（隐藏文件夹）。

### 步骤 4：检查 .gitignore 文件

确保项目根目录有 `.gitignore` 文件（我已经为你创建好了）。

这个文件会告诉 Git 哪些文件不需要提交，比如：
- `target/`（编译后的文件）
- `.idea/`（IDE 配置文件）
- `*.log`（日志文件）
- `.env`（敏感信息，如 API Key）

---

## 📤 首次提交到 GitHub

### 步骤 1：在 GitHub 上创建仓库

1. 登录 [GitHub](https://github.com)
2. 点击右上角的 `+` 号，选择 `New repository`
3. 填写仓库信息：
   - **Repository name**: `ai-chat-rag-demo`（或你喜欢的名字）
   - **Description**: `AI聊天室项目 - 包含RAG、Agent、SSE流式输出等功能`
   - **Visibility**: 选择 `Public`（公开）或 `Private`（私有）
   - **⚠️ 不要勾选** `Add a README file`、`Add .gitignore`、`Choose a license`（因为本地已有）
4. 点击 `Create repository`

### 步骤 2：添加所有文件到暂存区

```bash
git add .
```

这个命令会把当前目录下所有文件（除了 .gitignore 中排除的）添加到暂存区。

**查看暂存区状态**：
```bash
git status
```

应该能看到类似这样的输出：
```
Changes to be committed:
  (use "git rm --cached <file>..." to unstage)
        new file:   pom.xml
        new file:   src/main/java/...
        ...
```

### 步骤 3：创建首次提交

```bash
git commit -m "初始化项目：Spring Boot + WebFlux + PostgreSQL 基础框架"
```

`-m` 后面是提交信息，描述这次提交做了什么。

### 步骤 4：连接远程仓库

在 GitHub 仓库页面，你会看到一个类似的 URL：
- HTTPS: `https://github.com/你的用户名/ai-chat-rag-demo.git`
- SSH: `git@github.com:你的用户名/ai-chat-rag-demo.git`

**推荐使用 HTTPS**（更简单，不需要配置 SSH 密钥）。

执行：
```bash
git remote add origin https://github.com/你的用户名/ai-chat-rag-demo.git
```

例如：
```bash
git remote add origin https://github.com/zhangsan/ai-chat-rag-demo.git
```

**查看远程仓库**：
```bash
git remote -v
```

应该显示：
```
origin  https://github.com/你的用户名/ai-chat-rag-demo.git (fetch)
origin  https://github.com/你的用户名/ai-chat-rag-demo.git (push)
```

### 步骤 5：推送到 GitHub

```bash
git branch -M main
git push -u origin main
```

- `git branch -M main`：将当前分支重命名为 `main`（GitHub 默认主分支名）
- `git push -u origin main`：推送到远程仓库，`-u` 设置上游分支

**首次推送可能需要登录**：
- 如果提示输入用户名和密码，用户名填你的 GitHub 用户名
- 密码需要填 **Personal Access Token**（不是 GitHub 登录密码）

#### 如何获取 Personal Access Token？

1. GitHub → 右上角头像 → `Settings`
2. 左侧菜单最下方 → `Developer settings`
3. `Personal access tokens` → `Tokens (classic)`
4. `Generate new token` → `Generate new token (classic)`
5. 填写：
   - **Note**: `我的AI项目`（随便填，用于标识）
   - **Expiration**: 选择过期时间（如 90 days）
   - **Select scopes**: 勾选 `repo`（全部权限）
6. 点击 `Generate token`
7. **⚠️ 复制生成的 token**（只显示一次，要保存好！）
8. 推送时，密码填这个 token

---

## 📝 日常开发提交流程

每次完成一个功能或一天的工作后，按以下步骤提交：

### 标准提交流程

```bash
# 1. 查看当前修改的文件
git status

# 2. 添加修改的文件到暂存区
git add .

# 3. 提交（写清楚这次做了什么）
git commit -m "完成功能：Spring AI 集成，实现基础对话功能"

# 4. 推送到 GitHub
git push
```

### 提交信息规范（建议）

好的提交信息应该清晰描述做了什么：

```
✅ 好的提交信息：
- "完成功能：Spring AI 集成，实现基础对话功能"
- "添加功能：前端聊天界面，支持发送消息和显示回答"
- "修复bug：解决懒加载序列化问题"
- "优化：改进RAG检索性能，限制topK为5"

❌ 不好的提交信息：
- "更新"
- "fix"
- "修改"
- "123"
```

### 查看提交历史

```bash
git log
```

按 `q` 键退出。

查看简洁版本：
```bash
git log --oneline
```

---

## 🛠️ 常用 Git 命令

### 查看状态

```bash
# 查看当前工作区和暂存区状态
git status

# 查看具体的修改内容
git diff
```

### 撤销操作

```bash
# 撤销工作区的修改（还未 add）
git checkout -- 文件名

# 撤销暂存区的文件（已经 add，但还未 commit）
git reset HEAD 文件名

# 修改最后一次提交信息
git commit --amend -m "新的提交信息"
```

### 分支操作

```bash
# 查看所有分支
git branch

# 创建新分支
git branch 分支名

# 切换分支
git checkout 分支名

# 创建并切换分支
git checkout -b 功能分支名

# 合并分支（在 main 分支执行）
git merge 功能分支名
```

### 拉取远程更新

如果多人协作，或在不同电脑上开发：

```bash
# 拉取远程最新代码
git pull
```

---

## ⚠️ 重要注意事项

### 1. 不要提交敏感信息

**永远不要提交**：
- API Key
- 密码
- `.env` 文件
- `application-local.yml`（如果有敏感配置）

**已经在 .gitignore 中排除了这些文件**，但要注意：
- 如果不小心提交了敏感信息，要立即修改并重新生成密钥
- 可以在 GitHub 仓库设置中删除敏感提交历史（但比较麻烦）

### 2. 提交前检查

提交前执行 `git status`，确认：
- ✅ 没有包含敏感文件
- ✅ 没有包含编译文件（`target/`）
- ✅ 没有包含 IDE 配置文件（`.idea/`）

### 3. 提交频率建议

- **每天至少提交一次**（完成的功能）
- **每个功能完成就提交**（而不是等几天）
- **遇到重大问题前先提交**（方便回退）

---

## 🐛 常见问题解决

### 问题 1：推送时提示 "Authentication failed"

**原因**：用户名或密码（token）错误

**解决**：
```bash
# 清除保存的凭证
git config --global --unset credential.helper

# Windows 凭据管理器：控制面板 → 凭据管理器 → Windows 凭据 → 删除 GitHub 相关凭证
```

然后重新推送，输入正确的用户名和 token。

### 问题 2：推送时提示 "failed to push some refs"

**原因**：远程仓库有新的提交，本地没有

**解决**：
```bash
# 先拉取远程更新
git pull origin main

# 如果有冲突，解决冲突后再推送
git push
```

### 问题 3：不小心提交了敏感信息

**解决方法 1：修改 .gitignore 后重新提交**

```bash
# 1. 添加到 .gitignore
echo "敏感文件" >> .gitignore

# 2. 从 Git 中删除（但保留本地文件）
git rm --cached 敏感文件

# 3. 提交
git commit -m "移除敏感文件"

# 4. 推送
git push
```

**注意**：历史记录中仍然有敏感信息，如果已经推送，建议：
- 修改所有相关密码/密钥
- 或考虑使用 `git filter-branch` 清理历史（复杂）

### 问题 4：想回退到之前的提交

```bash
# 查看提交历史
git log --oneline

# 回退到指定提交（保留工作区修改）
git reset --soft 提交ID的前7位

# 或完全回退（丢弃工作区修改）
git reset --hard 提交ID的前7位
```

---

## 📅 推荐的日常工作流程

### 开始一天的工作

```bash
# 1. 拉取最新代码（如果多人协作）
git pull

# 2. 查看当前状态
git status
```

### 完成一个功能后

```bash
# 1. 查看修改
git status
git diff

# 2. 添加修改
git add .

# 3. 提交
git commit -m "完成功能：XXX"

# 4. 推送
git push
```

### 结束一天的工作

```bash
# 确保所有代码都已提交和推送
git status  # 应该显示 "nothing to commit"

git push    # 确保已推送到远程
```

---

## 🎯 快速参考命令列表

```bash
# 初始化仓库（仅首次）
git init

# 添加文件
git add .
git add 文件名

# 提交
git commit -m "提交信息"

# 推送
git push

# 拉取
git pull

# 查看状态
git status

# 查看历史
git log
git log --oneline

# 连接远程仓库（仅首次）
git remote add origin https://github.com/用户名/仓库名.git

# 查看远程仓库
git remote -v
```

---

## 📚 额外学习资源

- [Git 官方文档](https://git-scm.com/doc)
- [GitHub 官方指南](https://docs.github.com/)
- [Git 可视化学习工具](https://learngitbranching.js.org/)（推荐，非常好用！）

---

## ✅ 检查清单

首次设置完成后，确认：

- [ ] Git 已安装并配置用户名和邮箱
- [ ] 本地仓库已初始化（`git init`）
- [ ] `.gitignore` 文件已创建并正确配置
- [ ] GitHub 仓库已创建
- [ ] 远程仓库已连接（`git remote add origin`）
- [ ] 首次代码已推送（`git push -u origin main`）
- [ ] 可以正常推送和拉取代码

---

**完成以上步骤后，你的项目就已经在 GitHub 上了！每天完成功能后，记得提交和推送代码。🚀**

