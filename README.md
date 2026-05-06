# TeamGlowing

TeamGlowing 是一个用于 Minecraft Forge 1.12.2 的小队辅助模组。它提供小队创建、邀请、离队、信息查看、同队发光标记，以及类似新版 Minecraft 定位栏的队友 HUD 指示。

## 功能

- 小队系统
  - 玩家可以创建小队。
  - 队长可以邀请其他玩家加入。
  - 玩家可以接受邀请、离开小队、查看小队信息。
  - 小队数据会保存到服务端存档目录。

- 同队发光标记
  - 同一小队的玩家可以看到彼此的发光效果。
  - 非同队玩家不会看到该发光效果。
  - 发光状态由服务端同步，避免直接影响所有客户端。

- 队友定位栏
  - 在经验条上层显示队友方向图标。
  - 图标方向基于当前摄像机朝向。
  - 队友在摄像机上方时，图标上方显示上箭头。
  - 队友在摄像机下方时，图标下方显示下箭头。
  - 队友在背后时隐藏图标，避免从屏幕一侧跳到另一侧。
  - 按住 Tab 时，在图标上方显示队友名。
  - 图标会根据距离变小，采用 Java 版定位栏距离分档：128、230、332 格。
  - 队友潜行、头戴南瓜、头戴南瓜灯或头戴头颅时，不显示该玩家定位图标。

- 本地化
  - 支持中文和英文语言文件。

## 环境要求

- Minecraft 1.12.2
- Forge 14.23.5.2864
- Java 8
- ForgeGradle 3

## 指令

所有 `/teamglow` 指令普通玩家均可使用，不需要 OP 权限。

### 创建小队

```mcfunction
/teamglow create <小队名>
```

示例：

```mcfunction
/teamglow create alpha
```

### 邀请玩家

```mcfunction
/teamglow invite <玩家名>
```

示例：

```mcfunction
/teamglow invite Steve
```

### 接受邀请

```mcfunction
/teamglow accept
```

### 离开小队

```mcfunction
/teamglow leave
```

如果队长离开，小队会自动转移队长；如果小队没有其他成员，则小队会解散。

### 查看小队信息

```mcfunction
/teamglow info
```

## 玩家身份

模组使用玩家名作为小队成员身份来源，不使用 UUID。内部匹配会对玩家名做大小写不敏感处理。

开发环境中可以在 `build.gradle` 的 `minecraft.runs.client` 里固定用户名：

```gradle
args '--username', 'mckafei'
```

## 数据保存

小队数据会保存到服务端运行目录中的：

```text
teamglowing-parties.json
```

旧版本保存的数据字段会尽量兼容读取。

## 构建

在项目根目录运行：

```powershell
.\gradlew.bat build
```

只检查 Java 编译：

```powershell
.\gradlew.bat compileJava
```

构建产物通常位于：

```text
build/libs/
```

## 开发运行

生成 IntelliJ IDEA 运行配置：

```powershell
.\gradlew.bat genIntellijRuns
```

生成 Eclipse 运行配置：

```powershell
.\gradlew.bat genEclipseRuns
```

运行客户端：

```powershell
.\gradlew.bat runClient
```

运行服务端：

```powershell
.\gradlew.bat runServer
```

## 资源说明

队友定位栏图标贴图复用了 BetterPlayerLocatorBar 风格资源，位于：

```text
src/main/resources/assets/teamglowing/textures/gui/bplb/
```

## 注意事项

- 模组主要面向服务端多人环境。
- 小队身份基于玩家名，因此玩家改名后会被视为另一个玩家。
- 定位栏图标不会显示潜行、头戴南瓜、头戴南瓜灯或头戴头颅的队友。
- 普通玩家可以直接使用小队指令，无需 OP 权限。
