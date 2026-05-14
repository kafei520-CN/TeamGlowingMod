# TeamGlowing

TeamGlowing 现已迁移为 `Fabric 1.21.1` 模组，并将 `fabric.mod.json` 的 Minecraft 依赖范围放宽到 `1.21 ~ 1.21.11`。

## 功能

- 小队系统
  - 玩家可以创建小队。
  - 队长可以邀请其他玩家加入。
  - 玩家可以接受邀请、离开小队、查看小队信息。
  - 小队数据会保存到服务端存档目录。
- 同队发光标记
  - 同一小队的玩家可以看到彼此的发光效果。
  - 非同队玩家不会看到该发光效果。
  - 发光状态由服务端按玩家视角单独同步。
- 队友定位栏 HUD
  - 在经验条上层显示队友方向图标。
  - 图标方向基于当前摄像机朝向。
  - 队友在摄像机上方时，图标上方显示上箭头。
  - 队友在摄像机下方时，图标下方显示下箭头。
  - 队友在背后时隐藏图标，避免从屏幕一侧跳到另一侧。
  - 按住 Tab 时，在图标上方显示队友名。
  - 图标会根据距离变小，采用 Java 版定位栏距离分档：128、230、332 格。
  - 队友潜行、头戴雕刻南瓜、南瓜灯或任意头颅时，不显示该玩家定位图标。
- 本地化
  - 支持 `zh_cn` 与 `en_us`。

## 环境要求

- Minecraft `1.21.1`
- Fabric Loader `0.16.14+`
- Fabric API `0.110.0+1.21.1`
- Java `21`

## 指令

所有 `/teamglow` 指令普通玩家均可使用，不需要 OP 权限。

```mcfunction
/teamglow create <小队名>
/teamglow invite <玩家名>
/teamglow accept
/teamglow leave
/teamglow info
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

运行客户端：

```powershell
.\gradlew.bat runClient
```

运行服务端：

```powershell
.\gradlew.bat runServer
```

构建产物通常位于：

```text
build/libs/
```

## 说明

- 当前工程使用 Fabric Loom 构建，而不是 ForgeGradle。
- 模组主要逻辑仍以服务端为主，客户端负责 HUD 与定位包接收。
- 小队身份仍基于玩家名，大小写不敏感，不使用 UUID 作为主身份键。
