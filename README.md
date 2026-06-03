# Dyeing

`Dyeing` 是一个基于 Minecraft `1.20.1` 与 Forge `47.x` 的服务端/客户端同步模组，用于给实体添加可染色、可缩放、可播放动画的油漆渲染效果，既支持生物模型表面的油漆层，也支持绑定实体的长方体区域表面油漆。

## 功能说明

- 支持为生物实体添加一层覆盖原模型动作的油漆层。
- 支持静态颜色与静态缩放。
- 支持每种油漆层单独设置 XYZ 三轴偏移。
- 支持缩放动画。
- 支持颜色渐变动画。
- 支持同时启用缩放动画和颜色渐变。
- 支持为所有动画设置播放次数，默认 `-1` 表示无限循环。
- 支持为所有动画设置播放结束行为：回到起点、停在终点、自动移除。
- 支持绑定实体并渲染随实体移动的长方体区域表面油漆。
- 支持区域绕实体 Y 轴旋转（顺时针/逆时针，无限循环/指定次数）。
- 支持十六进制颜色中的透明度。
- 支持指令修改后即时同步到客户端。
- 支持数据持久化保存到世界存档。

## 支持范围

- 模型表面油漆层仅支持 `LivingEntity`。
- 原版生物、玩家、盔甲架，以及大多数使用常规 `LivingEntityRenderer` 的 Blockbench 生物模型都可用。
- 绑定区域表面油漆可绑定任意已加载实体，只要客户端当前能拿到该实体。

## 指令权限

- 需要权限等级 `2` 或以上。

## 指令总览

- 现在只保留一个根指令：`/dyeing`。
- `/dyeing paint add` 下支持 `static`、`scale`、`color`、`combo` 四个补全选项。
- 新增 `/dyeing area add ...` 与 `/dyeing area remove ...` 用于区域表面油漆。
- `/dyeing paint remove` 用于移除实体模型油漆层。
- `/dyeing remove all` 用于同时移除油漆层和区域油漆。

## 静态油漆层

```mcfunction
/dyeing paint add static <实体UUID> <十六进制颜色> [缩放] [offset_x offset_y offset_z]
```

示例：

```mcfunction
/dyeing paint add static 123e4567-e89b-12d3-a456-426614174000 FF00FF00
/dyeing paint add static 123e4567-e89b-12d3-a456-426614174000 80FF0000 1.2
```

说明：

- `实体UUID`：目标生物实体的 UUID。
- `十六进制颜色`：支持 `RRGGBB`、`AARRGGBB`、`#RRGGBB`、`0xAARRGGBB`。
- `缩放`：可选，默认 `1.0`。
- `offset_x offset_y offset_z`：可选，默认 `0 0 0`，会在默认油漆层位置基础上追加三轴偏移。

## 缩放动画

```mcfunction
/dyeing paint add scale <实体UUID> <十六进制颜色> <scale_from> <scale_to> <alpha_from> <alpha_to> <period> [offset_x offset_y offset_z] [play_count [end_action]]
```

示例：

```mcfunction
/dyeing paint add scale 123e4567-e89b-12d3-a456-426614174000 FFFF0000 0.8 1.4 0.2 1.0 40
/dyeing paint add scale 123e4567-e89b-12d3-a456-426614174000 FFFF0000 0.8 1.4 0.2 1.0 40 1.0 0.0 0.0 3 remove
```

说明：

- `十六进制颜色`：缩放动画期间使用的基础颜色。
- `scale_from`：动画起点缩放。
- `scale_to`：动画终点缩放。
- `alpha_from`：动画起点透明度系数，范围 `0.0 ~ 1.0`。
- `alpha_to`：动画终点透明度系数，范围 `0.0 ~ 1.0`。
- `period`：变化周期，单位 `tick`。
- `offset_x offset_y offset_z`：可选，默认 `0 0 0`。
- `play_count`：可选，默认 `-1`，表示无限次；填 `1` 或更大整数表示播放指定次数。
- `end_action`：可选，默认 `end`，可选值为 `start`、`end`、`remove`。

## 颜色渐变动画

```mcfunction
/dyeing paint add color <实体UUID> <color_from> <color_to> <scale> <period> [offset_x offset_y offset_z] [play_count [end_action]]
```

示例：

```mcfunction
/dyeing paint add color 123e4567-e89b-12d3-a456-426614174000 40FF0000 C00000FF 1.0 60
/dyeing paint add color 123e4567-e89b-12d3-a456-426614174000 40FF0000 C00000FF 1.0 60 2 end
```

说明：

- `color_from`：起点颜色。
- `color_to`：终点颜色。
- `scale`：颜色变化期间使用的固定缩放。
- `period`：变化周期，单位 `tick`。
- `offset_x offset_y offset_z`：可选，默认 `0 0 0`。
- `play_count`：可选，默认 `-1`。
- `end_action`：可选，默认 `end`，可选值为 `start`、`end`、`remove`。

注意：

- 颜色动画会直接插值 `ARGB`，因此会自动兼容透明度变化。

## 组合动画

```mcfunction
/dyeing paint add combo <实体UUID> <color_from> <color_to> <scale_from> <scale_to> <alpha_from> <alpha_to> <scale_period> <color_period> [offset_x offset_y offset_z] [scale_play_count [scale_end_action [color_play_count [color_end_action]]]]
```

示例：

```mcfunction
/dyeing paint add combo 123e4567-e89b-12d3-a456-426614174000 20FFAA00 E000AAFF 0.7 1.4 0.3 1.0 30 80
/dyeing paint add combo 123e4567-e89b-12d3-a456-426614174000 20FFAA00 E000AAFF 0.7 1.4 0.3 1.0 30 80 5 end 2 remove
```

说明：

- 同时启用缩放动画与颜色渐变动画。
- `scale_period` 与 `color_period` 可分别设置。
- `offset_x offset_y offset_z`：可选，默认 `0 0 0`。
- `scale_play_count`：可选，默认 `-1`。
- `scale_end_action`：可选，默认 `end`。
- `color_play_count`：可选，默认 `-1`。
- `color_end_action`：可选，默认 `end`。

## 偏移说明

- 偏移是相对于油漆层默认渲染位置追加的本地三轴位移。
- 三个值分别对应 `X`、`Y`、`Z`。
- 不填写时默认都是 `0`。
- 偏移会与静态、缩放动画、颜色渐变、组合动画一起生效。

## 区域表面油漆

区域表面油漆会绑定一个实体，以该实体当前坐标为原点，`from` 和 `to` 两组 XYZ 偏移定义一个轴对齐长方体，渲染这个长方体的 6 个外表面。

- 区域会实时跟随绑定实体移动。
- `scale` 语义为整体放缩，会同时缩放 `from/to` 两组偏移。
- 当前实现为每个绑定实体最多保存一个区域表面油漆。
- 区域命令的参数顺序固定为：`实体UUID -> from_x/y/z -> to_x/y/z -> 颜色或动画参数`。
- 为减少区域表面闪烁，区域 6 个外表面现在会做极小外扩，并避免同一平面重复绘制。

### 区域静态模式

```mcfunction
/dyeing area add static <实体UUID> <from_x> <from_y> <from_z> <to_x> <to_y> <to_z> <十六进制颜色> [缩放] [rotation_period [rotation_mode]]
```

示例：

```mcfunction
/dyeing area add static 123e4567-e89b-12d3-a456-426614174000 -1 0 -1 1 2 1 66FFAAFF
/dyeing area add static 123e4567-e89b-12d3-a456-426614174000 -1 0 -1 1 2 1 66FFAAFF 1.0 40 -1
```

说明：

- `rotation_period`：可选，默认 `0`（关闭旋转）。正数=顺时针旋转一圈的 tick 数；负数=逆时针旋转一圈的 tick 数。
- `rotation_mode`：可选，默认 `-1`（无限循环）。`-1`=无限旋转；正数=旋转指定次数后停止。

### 区域缩放动画

```mcfunction
/dyeing area add scale <实体UUID> <from_x> <from_y> <from_z> <to_x> <to_y> <to_z> <十六进制颜色> <scale_from> <scale_to> <alpha_from> <alpha_to> <period> [play_count [end_action]] [rotation_period [rotation_mode]]
```

示例：

```mcfunction
/dyeing area add scale 123e4567-e89b-12d3-a456-426614174000 -1 0 -1 1 2 1 88FF0000 0.8 1.2 0.2 0.9 40
/dyeing area add scale 123e4567-e89b-12d3-a456-426614174000 -1 0 -1 1 2 1 88FF0000 0.8 1.2 0.2 0.9 40 3 remove
/dyeing area add scale 123e4567-e89b-12d3-a456-426614174000 -1 0 -1 1 2 1 88FF0000 0.8 1.2 0.2 0.9 40 3 remove -20 5
```

说明：

- `play_count`：可选，默认 `-1`。
- `end_action`：可选，默认 `end`，可选值为 `start`、`end`、`remove`。
- `rotation_period`：可选，默认 `0`（关闭）。正数顺时针，负数逆时针。
- `rotation_mode`：可选，默认 `-1`（无限）。正数为指定旋转次数。

### 区域颜色渐变

```mcfunction
/dyeing area add color <实体UUID> <from_x> <from_y> <from_z> <to_x> <to_y> <to_z> <color_from> <color_to> <scale> <period> [play_count [end_action]] [rotation_period [rotation_mode]]
```

示例：

```mcfunction
/dyeing area add color 123e4567-e89b-12d3-a456-426614174000 -2 0 -2 2 3 2 30FF0000 B00000FF 1.0 60
/dyeing area add color 123e4567-e89b-12d3-a456-426614174000 -2 0 -2 2 3 2 30FF0000 B00000FF 1.0 60 4 start
/dyeing area add color 123e4567-e89b-12d3-a456-426614174000 -2 0 -2 2 3 2 30FF0000 B00000FF 1.0 60 4 start 30 -1
```

说明：

- `play_count`：可选，默认 `-1`。
- `end_action`：可选，默认 `end`，可选值为 `start`、`end`、`remove`。
- `rotation_period`：可选，默认 `0`（关闭）。正数顺时针，负数逆时针。
- `rotation_mode`：可选，默认 `-1`（无限）。正数为指定旋转次数。

### 区域组合动画

```mcfunction
/dyeing area add combo <实体UUID> <from_x> <from_y> <from_z> <to_x> <to_y> <to_z> <color_from> <color_to> <scale_from> <scale_to> <alpha_from> <alpha_to> <scale_period> <color_period> [scale_play_count [scale_end_action [color_play_count [color_end_action]]]] [rotation_period [rotation_mode]]
```

示例：

```mcfunction
/dyeing area add combo 123e4567-e89b-12d3-a456-426614174000 -1 0 -1 1 2 1 20FFAA00 E000AAFF 0.7 1.4 0.3 1.0 30 80
/dyeing area add combo 123e4567-e89b-12d3-a456-426614174000 -1 0 -1 1 2 1 20FFAA00 E000AAFF 0.7 1.4 0.3 1.0 30 80 3 end 1 remove
/dyeing area add combo 123e4567-e89b-12d3-a456-426614174000 -1 0 -1 1 2 1 20FFAA00 E000AAFF 0.7 1.4 0.3 1.0 30 80 3 end 1 remove -40 3
```

说明：

- `scale_play_count`：可选，默认 `-1`。
- `scale_end_action`：可选，默认 `end`。
- `color_play_count`：可选，默认 `-1`。
- `color_end_action`：可选，默认 `end`。
- `rotation_period`：可选，默认 `0`（关闭）。正数顺时针，负数逆时针。
- `rotation_mode`：可选，默认 `-1`（无限）。正数为指定旋转次数。

### 移除区域油漆

```mcfunction
/dyeing area remove <实体UUID>
```

示例：

```mcfunction
/dyeing area remove 123e4567-e89b-12d3-a456-426614174000
```

## 移除油漆层

```mcfunction
/dyeing paint remove <实体UUID>
```

示例：

```mcfunction
/dyeing paint remove 123e4567-e89b-12d3-a456-426614174000
```

## 移除全部

```mcfunction
/dyeing remove all <实体UUID>
```

示例：

```mcfunction
/dyeing remove all 123e4567-e89b-12d3-a456-426614174000
```

## 移除区域或全部说明

- `/dyeing paint remove <实体UUID>`：仅移除实体模型表面的油漆层。
- `/dyeing area remove <实体UUID>`：仅移除绑定实体的区域表面油漆。
- `/dyeing remove all <实体UUID>`：同时移除油漆层和区域表面油漆。

## 动画规则

- 当前动画模式为“单向循环”。
- 每个周期内按照 `A -> B` 平滑过渡。
- 无限播放时，到达周期末尾后会瞬间回到 `A`，然后开始下一轮。
- `A` 和 `B` 任意一边都可以更大或更小。
- `play_count=-1` 表示无限次。
- 有限播放结束后：
  - `start`：回到起点 `A`。
  - `end`：停在终点 `B`。
  - `remove`：自动移除这条油漆效果。
- 组合动画时：
  - 颜色由颜色动画计算。
  - 缩放由缩放动画计算。
  - 最终透明度 = 颜色动画得到的 alpha × 缩放动画的 alpha 系数。
  - 若 `combo` 中任一子动画的结束行为为 `remove` 且达到播放次数，该组合油漆会整体移除。

## 获取实体 UUID

如果需要给某个实体上色，你需要先拿到它的 UUID。常见方法：

- 使用调试模组或开发工具查看实体 UUID。
- 使用命令选中目标实体后读取 UUID。
- 在开发环境中通过日志、脚本或自定义命令输出 UUID。

本模组要求目标实体当前已加载，否则无法通过 `/dyeing paint add ...` 或 `/dyeing area add ...` 直接写入。

## 开发与构建

在项目目录执行：

```powershell
.\gradlew compileJava
.\gradlew build
```

## 备注

- 本模组不使用第三方依赖。
- 原始 Forge MDK 自带说明保留在 `README.txt`。
