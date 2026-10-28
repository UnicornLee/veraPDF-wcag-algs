# 2026-10-28 15:13 rebase 到 origin/integration 并同步双远端

## 一、任务目标

1. 将当前分支 `valueonline-20260722` rebase 到 `origin/integration`
2. 有冲突则解决（语义可判的自解，拿不定的交用户确认）
3. 推送远程仓库（origin 与 repo-b **两个都要**）
4. `mvn install` 到本地仓库

## 二、环境与仓库信息

| 项目 | 值 |
|---|---|
| 仓库根目录 | `D:\Code\JavaCode\opendataloader-pdf-parse\veraPDF-wcag-algs` |
| 当前分支 | `valueonline-20260722` |
| origin | `git@github.com:UnicornLee/veraPDF-wcag-algs.git` |
| repo-b | `ssh://git@git.valueonline.cn:9999/xiaoboli/veraPDF-wcag-algs.git` |
| Git 可执行文件 | `D:\Applications\Git\cmd\git.exe` |
| Maven | 3.9.14（`D:\Applications\apache-maven-3.9.14`） |
| Maven 默认 JDK | 11.0.0.2（`D:\Applications\Java\jdk-11.0.0.2`） |
| IDE SDK | JDK 17（`D:/Applications/Java/jdk-17.0.0.1`） |
| 编译级别 | `maven.compiler.source/target = 8` |
| 本地 Maven 仓库 | `D:\Maven_Repo`（**非默认 ~/.m2**） |
| 产物坐标 | `org.verapdf:wcag-algorithms:1.31.33-valueonline` |
| parent | `org.verapdf:verapdf-parent:1.31.1` |

## 三、执行前状态

- `HEAD` = `05ec00d`，工作区 clean，与 `origin/valueonline-20260722` 一致
- `git fetch --all --prune` 后：`repo-b/valueonline-20260722` 由 `bc8d144` 快进到 `05ec00d`
- 本地分支 `integration` 跟踪 `origin/integration`，位于 `c037bca`

### 关键前置核对（强推安全阀）

| 检查项 | 结果 |
|---|---|
| `HEAD..origin/valueonline-20260722` | 空（远端无独有提交） |
| `HEAD..repo-b/valueonline-20260722` | 空（远端无独有提交） |
| `origin/integration` vs `repo-b/integration` | 后者落后，缺 `c037bca` |
| merge-base（HEAD 与 origin/integration） | `4bd8797` |

结论：两个远端都没有需要保护的独有提交，可安全 `--force-with-lease`。

## 四、基线选择

- `origin/integration` = `c037bca` *Add equals and hachCode to StreamInfo*（仅改动 `StreamInfo.java`，+14 行）
- `repo-b/integration` = `4bd8797`（旧）
- **选择 `origin/integration` 作为 rebase 基线**，因为它包含 repo-b 尚无的提交

## 五、本地待重放提交（10 个，rebase 前 → 后哈希）

| 旧哈希 | 新哈希 | 说明 |
|---|---|---|
| 05ec00d | 2f0ad35 | chore(pom): add Vera development repository and update distribution management |
| 387252c | c849a52 | chore(pom): update repository details to use stock release repository |
| bc8d144 | 50e6df5 | chore(build): bump version to 1.31.33-valueonline for private wcag-algorithms artifact |
| f3ba7ef | 4d51904 | fix(TableBorder): extend short right horizontal lines…（防内容丢失/多余列） |
| 95b43a8 | 7ca412a | fix(TableBorder): correct below-cell reference in processMergedCells…（防 AIOOBE） |
| 7b041b5 | cef1898 | fix: add explicit JPEG2000 dependencies and force-load ImageReader SPI |
| ac973db | 6c83678 | fix: add defensive clamping and logging in getPageSubImage（防 RasterFormatException） |
| 681c97c | 4b7afa1 | feat: fix regression in table border detection for partial-border tables |
| babc3b5 | 20a991e | feat: output path + ShapeChunk 序列化 + TableBorderCell 背景色 + 图表/公式识别 |
| 1e7dcd0 | cb43696 | LinkChunk 添加 strokeColor 和有线表格逻辑修改 |

### 涉及文件（9 个，+606 / -65）

```
 .gitignore                                                        |   1 +
 pom.xml                                                           |  10 +-
 .../entities/content/LineChunk.java                               |  36 +++-
 .../entities/tables/tableBorders/TableBorder.java                 | 234 +++++++++-
 .../entities/tables/tableBorders/TableBorderCell.java             | 114 +++---
 .../consumers/LinesPreprocessingConsumer.java                     |  68 +++-
 .../semanticalgorithms/utils/ImagesUtils.java                     |  39 ++-
 .../algorithms/entities/content/LineChunkTest.java                |  43 ++++
 .../TableBorderDanglingLineTest.java                              | 126 ++++++++++
```

## 六、冲突处理结果

**零冲突。** `git rebase origin/integration` 输出：`Rebasing (1/10)...(10/10) Successfully rebased and updated refs/heads/valueonline-20260722`。

**原因**：上游自 merge-base 起只有 1 个提交，且仅改 `StreamInfo.java`；本地 10 个提交涉及的文件集与上游**无交集**，Git 可完全自动合并，因此不存在需要语义判断的合并点，也**没有采用任何"取一侧"的取舍**。

预置的冲突处理规则（本次未触发，留作后续参考）：

- `pom.xml` 的 `<version>` 保留 `1.31.33-valueonline`
- `pom.xml` 的 `jackson.version` 保留 `2.22.3`（勿回退到上游的 2.22.1）
- `pom.xml` 的 parent / repositories / distributionManagement 取并集，`vera-dev`、`stock` 均保留
- `BoundingBox.equals()` 等几何比较保留 `< EPSILON` 写法
- 纯 import / 空行 / 注释位置冲突取并集
- `src/test/resources/files/**` 两边文件全留
- `StaticContainers.java`、`ListDetectionConsumer.java`、`ListLabelsUtils.java` 类"同一功能两套重构"冲突不上自动判定，列给用户定

## 七、rebase 后校验

| 校验项 | 结果 |
|---|---|
| `git grep -n "<<<<<<<"` | 无输出（无冲突标记残留） |
| `git diff --stat backup/... HEAD` | 仅 `StreamInfo.java` +14 行（来自上游 `c037bca`），**本地改动一行未丢** |
| `StreamInfo.hashCode()` | 存在（第 189 行），确认上游改动已生效 |

## 八、构建与本地安装

```powershell
mvn clean install -B
```

| 结果项 | 值 |
|---|---|
| 结果 | **BUILD SUCCESS** |
| 测试 | 345 个，Failures 0 / Errors 0 / Skipped 0 |
| 耗时 | 47.124 s，完成于 15:13:58 +08:00 |
| jar | `target\wcag-algorithms-1.31.33-valueonline.jar`（347,051 字节） |
| 安装位置 | `D:\Maven_Repo\org\verapdf\wcag-algorithms\1.31.33-valueonline\`（jar + pom） |
| JaCoCo | 覆盖 136 个类，report 正常生成 |

新增测试类的用例均通过：`TableBorderDanglingLineTest`(3)、`TableBordersTests`(18)、`ListTests`(32)、`AccumulatedNodeSemanticsCheckerTests`(59)。

## 九、推送结果（--force-with-lease）

```powershell
git push --force-with-lease origin valueonline-20260722   # 05ec00d...2f0ad35 (forced update)
git push --force-with-lease repo-b  valueonline-20260722  # 05ec00d...2f0ad35 (forced update)
```

- 两个远端均成功强推，**未出现 `stale info`、未使用裸 `--force`**
- repo-b 返回提示：可发起 PR `https://git.valueonline.cn/xiaoboli/veraPDF-wcag-algs/compare/integration...valueonline-20260722`
- `repo-b` 的 SSH 输出含 post-quantum key exchange 警告，属服务端未升级，不影响操作

## 十、最终状态

| 位置 | HEAD |
|---|---|
| 本地 `valueonline-20260722` | `2f0ad35` |
| `origin/valueonline-20260722` | `2f0ad35` |
| `repo-b/valueonline-20260722` | `2f0ad35` |

`git status -sb` 无 ahead/behind，三处完全一致。备份分支 `backup/valueonline-20260722-before-rebase`（@`05ec00d`）已删除，`git branch` 仅剩 `integration` 与 `valueonline-20260722`。

## 十一、可复用经验

1. **Windows PowerShell 下 `git rebase` 的坑**：无 `head` / `tail` 命令（需用 `Select-Object -First/-Last` 或 `Get-Content -Tail`）；`git ... | cat` 会因 `cat` 是 `Get-Content` 别名报参数绑定失败，查看输出请用 `git --no-pager`。
2. **强推三连核对**：`git fetch --all --prune` → `git log --oneline HEAD..<remote>/<branch>`（远端有无独有提交）→ 两远端基线谁更新。本次正是靠第 2 步确认可以安全强推；历史上曾因跳过该步导致远端独有改动被抹除的风险。
3. **rebase 后的可信校验法**：`git diff --stat <备份分支> HEAD`，输出应当**只有上游带来的差异**；出现预期之外的文件说明合并有语义丢失。
4. **先验证再推送**：`mvn clean install` 全绿后再 push，避免把不可构建的提交推到两个远端。
5. **基线取较新者**：`origin/integration` 领先 `repo-b/integration` 一个提交，rebase 到 `origin/integration` 可一次性覆盖两边。
6. **备份分支成本极低**：rebase 前 `git branch backup/<branch>-before-rebase`，出问题秒级回退；本次确认无误后按用户要求删除。

## 十二、本次涉及的核心文件

- `pom.xml`（版本 `1.31.33-valueonline`、`vera-dev` 仓库、`stock` 分发仓库、jackson 2.22.3）
- `src/main/java/org/verapdf/wcag/algorithms/entities/content/LineChunk.java`
- `src/main/java/org/verapdf/wcag/algorithms/entities/tables/tableBorders/TableBorder.java`
- `src/main/java/org/verapdf/wcag/algorithms/entities/tables/tableBorders/TableBorderCell.java`
- `src/main/java/org/verapdf/wcag/algorithms/semanticalgorithms/consumers/LinesPreprocessingConsumer.java`
- `src/main/java/org/verapdf/wcag/algorithms/semanticalgorithms/utils/ImagesUtils.java`
- `src/main/java/org/verapdf/wcag/algorithms/semanticalgorithms/utils/StreamInfo.java`（上游新增 equals/hashCode）
- `src/test/java/org/verapdf/wcag/algorithms/entities/content/LineChunkTest.java`
- `src/test/java/org/verapdf/wcag/algorithms/semanticalgorithms/TableBorderDanglingLineTest.java`
