# Dyeing

`Dyeing` 是一个基于 Minecraft `1.20.1` 与 Forge `47.x` 的服务端/客户端同步模组，用于给 `LivingEntity` 添加一层可染色、可缩放、可播放动画的“油漆层”渲染效果。

## 功能说明

- 支持为生物实体添加一层覆盖原模型动作的油漆层。
- 支持静态颜色与静态缩放。
- 支持缩放动画。
- 支持颜色渐变动画。
- 支持同时启用缩放动画和颜色渐变。
- 支持十六进制颜色中的透明度。
- 支持指令修改后即时同步到客户端。
- 支持数据持久化保存到世界存档。

## 支持范围

- 当前仅支持 `LivingEntity`。
- 原版生物、玩家、盔甲架，以及大多数使用常规 `LivingEntityRenderer` 的 Blockbench 生物模型都可用。
- 不支持矿车、抛射物、展示实体等非 `LivingEntity` 实体。

## 指令权限

- 需要权限等级 `2` 或以上。

## 指令总览

- 根指令同时注册了 `/dyeing` 与 `/Dyeing`。
- `/Dyeing add` 下支持 `scale`、`color`、`combo` 补全选项。

## 静态油漆层

```mcfunction
/Dyeing add <实体UUID> <十六进制颜色> [缩放]
```

示例：

```mcfunction
/Dyeing add 123e4567-e89b-12d3-a456-426614174000 FF00FF00
/Dyeing add 123e4567-e89b-12d3-a456-426614174000 80FF0000 1.2
```

说明：

- `实体UUID`：目标生物实体的 UUID。
- `十六进制颜色`：支持 `RRGGBB`、`AARRGGBB`、`#RRGGBB`、`0xAARRGGBB`。
- `缩放`：可选，默认 `1.0`。

## 缩放动画

```mcfunction
/Dyeing add scale <实体UUID> <十六进制颜色> <scale_from> <scale_to> <alpha_from> <alpha_to> <period>
```

示例：

```mcfunction
/Dyeing add scale 123e4567-e89b-12d3-a456-426614174000 FFFF0000 0.8 1.4 0.2 1.0 40
```

说明：

- `十六进制颜色`：缩放动画期间使用的基础颜色。
- `scale_from`：动画起点缩放。
- `scale_to`：动画终点缩放。
- `alpha_from`：动画起点透明度系数，范围 `0.0 ~ 1.0`。
- `alpha_to`：动画终点透明度系数，范围 `0.0 ~ 1.0`。
- `period`：变化周期，单位 `tick`。

## 颜色渐变动画

```mcfunction
/Dyeing add color <实体UUID> <color_from> <color_to> <scale> <period>
```

示例：

```mcfunction
/Dyeing add color 123e4567-e89b-12d3-a456-426614174000 40FF0000 C00000FF 1.0 60
```

说明：

- `color_from`：起点颜色。
- `color_to`：终点颜色。
- `scale`：颜色变化期间使用的固定缩放。
- `period`：变化周期，单位 `tick`。

注意：

- 颜色动画会直接插值 `ARGB`，因此会自动兼容透明度变化。

## 组合动画

```mcfunction
/Dyeing add combo <实体UUID> <color_from> <color_to> <scale_from> <scale_to> <alpha_from> <alpha_to> <scale_period> <color_period>
```

示例：

```mcfunction
/Dyeing add combo 123e4567-e89b-12d3-a456-426614174000 20FFAA00 E000AAFF 0.7 1.4 0.3 1.0 30 80
```

说明：

- 同时启用缩放动画与颜色渐变动画。
- `scale_period` 与 `color_period` 可分别设置。

## 移除油漆层

```mcfunction
/Dyeing remove <实体UUID>
```

示例：

```mcfunction
/Dyeing remove 123e4567-e89b-12d3-a456-426614174000
```

## 动画规则

- 当前动画模式为“单向循环”。
- 每个周期内按照 `A -> B` 平滑过渡。
- 到达周期末尾后，会瞬间回到 `A`，然后开始下一轮。
- `A` 和 `B` 任意一边都可以更大或更小。
- 组合动画时：
  - 颜色由颜色动画计算。
  - 缩放由缩放动画计算。
  - 最终透明度 = 颜色动画得到的 alpha × 缩放动画的 alpha 系数。

## 获取实体 UUID

如果需要给某个实体上色，你需要先拿到它的 UUID。常见方法：

- 使用调试模组或开发工具查看实体 UUID。
- 使用命令选中目标实体后读取 UUID。
- 在开发环境中通过日志、脚本或自定义命令输出 UUID。

本模组要求目标实体当前已加载，否则无法通过 `/Dyeing add ...` 直接写入。

## 开发与构建

在项目目录执行：

```powershell
.\gradlew compileJava
.\gradlew build
```

## 备注

- 本模组不使用第三方依赖。
- 原始 Forge MDK 自带说明保留在 `README.txt`。
