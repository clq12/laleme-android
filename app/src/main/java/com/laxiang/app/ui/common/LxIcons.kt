package com.laxiang.app.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 矢量图标集（v2.5.0）
 *
 * 为什么要手绘 Canvas 而不是引图标库：
 * 1. 项目已有 `material-icons-core`，但它的图标集不含「布里斯托 7 种形态」
 *    这类领域图形；混用两套图标风格会不一致。
 * 2. 引入 material-icons-extended 会把 APK 体积推高约 1MB+，
 *    违背「不引入不必要的依赖」。
 * 3. Canvas 手绘零依赖、零体积、API 24 全兼容、颜色完全跟随主题。
 *
 * 统一规范：24×24 设计栅格，线宽 1.8dp，圆头圆角；
 * 可指定任意 size，内部按比例缩放。
 */

private const val GRID = 24f
private const val STROKE = 1.8f

/** 图标绘制基础设施：统一坐标系与缩放 */
@Composable
private fun IconBase(
    tint: Color,
    size: Dp,
    modifier: Modifier,
    draw: DrawScope.(Float, Stroke) -> Unit
) {
    Canvas(modifier = modifier.size(size)) {
        val scale = this.size.minDimension / GRID
        val stroke = Stroke(
            width = STROKE * scale,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
        draw(scale, stroke)
    }
}

private fun DrawScope.line(
    tint: Color, scale: Float, stroke: Stroke,
    x1: Float, y1: Float, x2: Float, y2: Float
) = drawLine(
    color = tint,
    start = Offset(x1 * scale, y1 * scale),
    end = Offset(x2 * scale, y2 * scale),
    strokeWidth = stroke.width,
    cap = StrokeCap.Round
)

private fun DrawScope.path(
    tint: Color, scale: Float, stroke: Stroke,
    build: Path.() -> Unit
) {
    val p = Path().apply {
        build()
    }
    // Path 内部点位在主线程构建时仍按 24 栅格写，这里统一缩放
    val m = androidx.compose.ui.graphics.Matrix().apply { scale(scale, scale) }
    p.transform(m)
    drawPath(p, tint, style = stroke)
}

// ══════════════════════════════════════════════
//  导航
// ══════════════════════════════════════════════

/** 首页：房子 */
@Composable
fun LxIconHome(tint: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) =
    IconBase(tint, size, modifier) { s, st ->
        path(tint, s, st) {
            moveTo(3.2f, 10.5f)
            lineTo(12f, 3.5f)
            lineTo(20.8f, 10.5f)
        }
        path(tint, s, st) {
            moveTo(5.6f, 9.6f)
            lineTo(5.6f, 20.2f)
            lineTo(18.4f, 20.2f)
            lineTo(18.4f, 9.6f)
        }
        path(tint, s, st) {
            moveTo(10f, 20.2f)
            lineTo(10f, 14.6f)
            lineTo(14f, 14.6f)
            lineTo(14f, 20.2f)
        }
    }

/** 统计：柱状图 */
@Composable
fun LxIconStats(tint: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) =
    IconBase(tint, size, modifier) { s, st ->
        path(tint, s, st) {
            moveTo(4f, 3.8f)
            lineTo(4f, 20.2f)
            lineTo(20.4f, 20.2f)
        }
        line(tint, s, st, 8.6f, 16.4f, 8.6f, 12.6f)
        line(tint, s, st, 12.6f, 16.4f, 12.6f, 8.6f)
        line(tint, s, st, 16.6f, 16.4f, 16.6f, 14.4f)
    }

/** 知识：书本 */
@Composable
fun LxIconBook(tint: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) =
    IconBase(tint, size, modifier) { s, st ->
        path(tint, s, st) {
            moveTo(3.4f, 5f)
            lineTo(9.2f, 5f)
            lineTo(12f, 7.8f)
            lineTo(14.8f, 5f)
            lineTo(20.6f, 5f)
            lineTo(20.6f, 18.2f)
            lineTo(14.8f, 18.2f)
            lineTo(12f, 21f)
            lineTo(9.2f, 18.2f)
            lineTo(3.4f, 18.2f)
            close()
        }
        line(tint, s, st, 12f, 7.8f, 12f, 21f)
    }

/** 设置：滑杆（比齿轮更符合「调参」语义，且小尺寸下更清晰） */
@Composable
fun LxIconSettings(tint: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) =
    IconBase(tint, size, modifier) { s, st ->
        line(tint, s, st, 4f, 7f, 20f, 7f)
        line(tint, s, st, 4f, 12f, 20f, 12f)
        line(tint, s, st, 4f, 17f, 20f, 17f)
        // 三个滑块手柄
        drawCircle(tint, radius = 2.4f * s, center = Offset(9f * s, 7f * s))
        drawCircle(tint, radius = 2.4f * s, center = Offset(15f * s, 12f * s))
        drawCircle(tint, radius = 2.4f * s, center = Offset(10.5f * s, 17f * s))
    }

// ══════════════════════════════════════════════
//  操作
// ══════════════════════════════════════════════

/** 新增：加号 */
@Composable
fun LxIconAdd(tint: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) =
    IconBase(tint, size, modifier) { s, st ->
        line(tint, s, st, 12f, 5f, 12f, 19f)
        line(tint, s, st, 5f, 12f, 19f, 12f)
    }

/** 返回：左箭头 */
@Composable
fun LxIconBack(tint: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) =
    IconBase(tint, size, modifier) { s, st ->
        line(tint, s, st, 19f, 12f, 5.5f, 12f)
        path(tint, s, st) {
            moveTo(11.5f, 5.5f)
            lineTo(5f, 12f)
            lineTo(11.5f, 18.5f)
        }
    }

/** 右箭头（日历翻月） */
@Composable
fun LxIconChevronRight(tint: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) =
    IconBase(tint, size, modifier) { s, st ->
        path(tint, s, st) {
            moveTo(9.5f, 5f)
            lineTo(16.5f, 12f)
            lineTo(9.5f, 19f)
        }
    }

/** 左箭头（日历翻月） */
@Composable
fun LxIconChevronLeft(tint: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) =
    IconBase(tint, size, modifier) { s, st ->
        path(tint, s, st) {
            moveTo(14.5f, 5f)
            lineTo(7.5f, 12f)
            lineTo(14.5f, 19f)
        }
    }

/** 搜索：放大镜 */
@Composable
fun LxIconSearch(tint: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) =
    IconBase(tint, size, modifier) { s, st ->
        drawCircle(tint, radius = 6.2f * s, center = Offset(10.5f * s, 10.5f * s), style = st)
        line(tint, s, st, 15.2f, 15.2f, 20f, 20f)
    }

/** 清除：圆圈叉 */
@Composable
fun LxIconClose(tint: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) =
    IconBase(tint, size, modifier) { s, st ->
        drawCircle(tint, radius = 8.2f * s, center = Offset(12f * s, 12f * s), style = st)
        line(tint, s, st, 9.2f, 9.2f, 14.8f, 14.8f)
        line(tint, s, st, 14.8f, 9.2f, 9.2f, 14.8f)
    }

/** 删除：垃圾桶 */
@Composable
fun LxIconDelete(tint: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) =
    IconBase(tint, size, modifier) { s, st ->
        line(tint, s, st, 4f, 6.6f, 20f, 6.6f)
        path(tint, s, st) {
            moveTo(9.2f, 6.6f)
            lineTo(9.2f, 4.2f)
            lineTo(14.8f, 4.2f)
            lineTo(14.8f, 6.6f)
        }
        path(tint, s, st) {
            moveTo(6.4f, 6.6f)
            lineTo(7.4f, 20.2f)
            lineTo(16.6f, 20.2f)
            lineTo(17.6f, 6.6f)
        }
        line(tint, s, st, 10.4f, 10f, 10.4f, 17f)
        line(tint, s, st, 13.6f, 10f, 13.6f, 17f)
    }

/** 相机 */
@Composable
fun LxIconCamera(tint: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) =
    IconBase(tint, size, modifier) { s, st ->
        path(tint, s, st) {
            moveTo(3.4f, 8.4f)
            lineTo(7.6f, 8.4f)
            lineTo(9.2f, 5.6f)
            lineTo(14.8f, 5.6f)
            lineTo(16.4f, 8.4f)
            lineTo(20.6f, 8.4f)
            lineTo(20.6f, 18.8f)
            lineTo(3.4f, 18.8f)
            close()
        }
        drawCircle(tint, radius = 3.6f * s, center = Offset(12f * s, 13.4f * s), style = st)
    }

/** 相册：图片 */
@Composable
fun LxIconGallery(tint: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) =
    IconBase(tint, size, modifier) { s, st ->
        path(tint, s, st) {
            moveTo(3.6f, 5.2f)
            lineTo(20.4f, 5.2f)
            lineTo(20.4f, 18.8f)
            lineTo(3.6f, 18.8f)
            close()
        }
        drawCircle(tint, radius = 1.7f * s, center = Offset(8.6f * s, 9.8f * s))
        path(tint, s, st) {
            moveTo(4.6f, 17.4f)
            lineTo(10f, 12.2f)
            lineTo(14f, 15.6f)
            lineTo(16.6f, 13.2f)
            lineTo(19.6f, 16.4f)
        }
    }

/** 计时：秒表 */
@Composable
fun LxIconTimer(tint: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) =
    IconBase(tint, size, modifier) { s, st ->
        drawCircle(tint, radius = 7.4f * s, center = Offset(12f * s, 13.6f * s), style = st)
        line(tint, s, st, 12f, 13.6f, 12f, 9.4f)
        line(tint, s, st, 9.6f, 3.4f, 14.4f, 3.4f)
        line(tint, s, st, 12f, 3.4f, 12f, 5.4f)
        line(tint, s, st, 18.6f, 7f, 20f, 8.4f)
    }

/** 检查：对勾 */
@Composable
fun LxIconCheck(tint: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) =
    IconBase(tint, size, modifier) { s, st ->
        path(tint, s, st) {
            moveTo(5f, 12.6f)
            lineTo(10f, 17.4f)
            lineTo(19.2f, 6.8f)
        }
    }

/** 警告：三角感叹号 */
@Composable
fun LxIconWarning(tint: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) =
    IconBase(tint, size, modifier) { s, st ->
        path(tint, s, st) {
            moveTo(12f, 3.6f)
            lineTo(21.4f, 19.8f)
            lineTo(2.6f, 19.8f)
            close()
        }
        line(tint, s, st, 12f, 9.6f, 12f, 14.2f)
        drawCircle(tint, radius = 1.05f * s, center = Offset(12f * s, 16.7f * s))
    }

/** 上传 / 同步：双箭头循环 */
@Composable
fun LxIconSync(tint: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) =
    IconBase(tint, size, modifier) { s, st ->
        path(tint, s, st) {
            moveTo(4.4f, 11.6f)
            cubicTo(4.8f, 7.4f, 8.2f, 4.4f, 12.2f, 4.6f)
            cubicTo(14.8f, 4.7f, 17f, 6.1f, 18.3f, 8.2f)
        }
        path(tint, s, st) {
            moveTo(19.6f, 12.4f)
            cubicTo(19.2f, 16.6f, 15.8f, 19.6f, 11.8f, 19.4f)
            cubicTo(9.2f, 19.3f, 7f, 17.9f, 5.7f, 15.8f)
        }
        path(tint, s, st) {
            moveTo(18.6f, 4.6f)
            lineTo(18.9f, 8.6f)
            lineTo(14.9f, 8.9f)
        }
        path(tint, s, st) {
            moveTo(5.4f, 19.4f)
            lineTo(5.1f, 15.4f)
            lineTo(9.1f, 15.1f)
        }
    }

/** 存储：数据库 */
@Composable
fun LxIconStorage(tint: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) =
    IconBase(tint, size, modifier) { s, st ->
        path(tint, s, st) {
            moveTo(4f, 6.4f)
            cubicTo(4f, 4.5f, 7.6f, 3.2f, 12f, 3.2f)
            cubicTo(16.4f, 3.2f, 20f, 4.5f, 20f, 6.4f)
            cubicTo(20f, 8.3f, 16.4f, 9.6f, 12f, 9.6f)
            cubicTo(7.6f, 9.6f, 4f, 8.3f, 4f, 6.4f)
            close()
        }
        path(tint, s, st) {
            moveTo(4f, 6.4f)
            lineTo(4f, 17.6f)
            cubicTo(4f, 19.5f, 7.6f, 20.8f, 12f, 20.8f)
            cubicTo(16.4f, 20.8f, 20f, 19.5f, 20f, 17.6f)
            lineTo(20f, 6.4f)
        }
        path(tint, s, st) {
            moveTo(4f, 12f)
            cubicTo(4f, 13.9f, 7.6f, 15.2f, 12f, 15.2f)
            cubicTo(16.4f, 15.2f, 20f, 13.9f, 20f, 12f)
        }
    }

/** 导出：向下箭头入托盘 */
@Composable
fun LxIconDownload(tint: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) =
    IconBase(tint, size, modifier) { s, st ->
        line(tint, s, st, 12f, 3.6f, 12f, 14.4f)
        path(tint, s, st) {
            moveTo(7.4f, 9.8f)
            lineTo(12f, 14.4f)
            lineTo(16.6f, 9.8f)
        }
        path(tint, s, st) {
            moveTo(4.4f, 16.4f)
            lineTo(4.4f, 20.2f)
            lineTo(19.6f, 20.2f)
            lineTo(19.6f, 16.4f)
        }
    }

/** 日历 */
@Composable
fun LxIconCalendar(tint: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) =
    IconBase(tint, size, modifier) { s, st ->
        path(tint, s, st) {
            moveTo(3.6f, 5.6f)
            lineTo(20.4f, 5.6f)
            lineTo(20.4f, 20.2f)
            lineTo(3.6f, 20.2f)
            close()
        }
        line(tint, s, st, 3.6f, 10f, 20.4f, 10f)
        line(tint, s, st, 8.2f, 3.2f, 8.2f, 7.4f)
        line(tint, s, st, 15.8f, 3.2f, 15.8f, 7.4f)
    }

/** 时钟（时长） */
@Composable
fun LxIconClock(tint: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) =
    IconBase(tint, size, modifier) { s, st ->
        drawCircle(tint, radius = 8.4f * s, center = Offset(12f * s, 12f * s), style = st)
        line(tint, s, st, 12f, 12f, 12f, 7.4f)
        line(tint, s, st, 12f, 12f, 15.6f, 13.8f)
    }

/** 备注：多行文本 */
@Composable
fun LxIconNotes(tint: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) =
    IconBase(tint, size, modifier) { s, st ->
        line(tint, s, st, 4.4f, 6.4f, 19.6f, 6.4f)
        line(tint, s, st, 4.4f, 12f, 19.6f, 12f)
        line(tint, s, st, 4.4f, 17.6f, 13.6f, 17.6f)
    }

/** 空状态标记：一片叶子 */
@Composable
fun LxIconLeaf(tint: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) =
    IconBase(tint, size, modifier) { s, st ->
        path(tint, s, st) {
            moveTo(20.4f, 3.6f)
            cubicTo(11.6f, 3.6f, 4.2f, 8.4f, 4.2f, 15.4f)
            cubicTo(4.2f, 17.6f, 5.2f, 19.6f, 6.8f, 20.6f)
            cubicTo(13.8f, 20.6f, 20.4f, 13.4f, 20.4f, 3.6f)
            close()
        }
        line(tint, s, st, 4.6f, 20.4f, 13f, 12f)
    }

// ══════════════════════════════════════════════
//  数据状态
// ══════════════════════════════════════════════

/** 理想状态：圆环内对勾 */
@Composable
fun LxIconIdeal(tint: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) =
    IconBase(tint, size, modifier) { s, st ->
        drawCircle(tint, radius = 8.6f * s, center = Offset(12f * s, 12f * s), style = st)
        path(tint, s, st) {
            moveTo(8f, 12.4f)
            lineTo(11f, 15.4f)
            lineTo(16.2f, 9f)
        }
    }

/** 需要注意：圆环内横线 */
@Composable
fun LxIconCaution(tint: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) =
    IconBase(tint, size, modifier) { s, st ->
        drawCircle(tint, radius = 8.6f * s, center = Offset(12f * s, 12f * s), style = st)
        line(tint, s, st, 8f, 12f, 16f, 12f)
    }

// ══════════════════════════════════════════════
//  布里斯托 7 型形态图示
// ══════════════════════════════════════════════

/**
 * 布里斯托 1–7 型形态图形。
 *
 * 用几何形状表达形态差异（硬球 → 腊肠 → 软块 → 水样），
 * 不依赖 emoji，也不会因系统字体差异而变形。
 * 每种形态都是 24×24 栅格内的实心图形，缩小到 20dp 仍可辨识。
 */
@Composable
fun LxBristolShape(
    type: Int,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) = IconBase(tint, size, modifier) { s, st ->
    when (type) {
        // 1 型：几颗分离的硬球
        1 -> {
            drawCircle(tint, 2.9f * s, Offset(7.4f * s, 8.2f * s))
            drawCircle(tint, 2.9f * s, Offset(16.6f * s, 8.2f * s))
            drawCircle(tint, 2.9f * s, Offset(7.4f * s, 17.2f * s))
            drawCircle(tint, 2.9f * s, Offset(16.6f * s, 17.2f * s))
        }
        // 2 型：凹凸腊肠（三节相连的块）
        2 -> {
            drawCircle(tint, 3.4f * s, Offset(6.6f * s, 12f * s))
            drawCircle(tint, 3.6f * s, Offset(12f * s, 11.4f * s))
            drawCircle(tint, 3.4f * s, Offset(17.4f * s, 12f * s))
            drawCircle(tint, 2.9f * s, Offset(9.4f * s, 12.6f * s))
            drawCircle(tint, 2.9f * s, Offset(14.8f * s, 12.6f * s))
        }
        // 3 型：有裂纹的腊肠（整块 + 两条描边裂纹，尾部留缺口）
        3 -> {
            path(tint, s, st) {
                moveTo(6.6f, 8.8f)
                lineTo(17.4f, 8.8f)
                cubicTo(19.6f, 8.8f, 20.8f, 10.2f, 20.8f, 12f)
                cubicTo(20.8f, 13.8f, 19.6f, 15.2f, 17.4f, 15.2f)
                lineTo(6.6f, 15.2f)
                cubicTo(4.4f, 15.2f, 3.2f, 13.8f, 3.2f, 12f)
                cubicTo(3.2f, 10.2f, 4.4f, 8.8f, 6.6f, 8.8f)
                close()
            }
            // 表面裂纹：两条短线，用对比色描出「裂」的观感
            line(tint, s, st, 9.2f, 8.8f, 9.6f, 12f)
            line(tint, s, st, 14.6f, 15.2f, 15f, 12f)
        }
        // 4 型：光滑香蕉形（理想型，用实心圆角条）
        4 -> {
            drawRoundRect(
                color = tint,
                topLeft = Offset(3.2f * s, 8.2f * s),
                size = androidx.compose.ui.geometry.Size(17.6f * s, 7.6f * s),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.8f * s)
            )
        }
        // 5 型：软质断块（圆角方块 + 间隙）
        5 -> {
            drawRoundRect(
                color = tint,
                topLeft = Offset(4f * s, 7.2f * s),
                size = androidx.compose.ui.geometry.Size(7.4f * s, 7.4f * s),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.6f * s)
            )
            drawRoundRect(
                color = tint,
                topLeft = Offset(13f * s, 6.4f * s),
                size = androidx.compose.ui.geometry.Size(7f * s, 7f * s),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.6f * s)
            )
            drawRoundRect(
                color = tint,
                topLeft = Offset(8.6f * s, 15.4f * s),
                size = androidx.compose.ui.geometry.Size(6.6f * s, 6.4f * s),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.4f * s)
            )
        }
        // 6 型：糊状（不规则边缘的大块）
        6 -> {
            drawCircle(tint, 6.6f * s, Offset(11.4f * s, 12.4f * s))
            drawCircle(tint, 4.4f * s, Offset(17.4f * s, 15.6f * s))
            drawCircle(tint, 3.8f * s, Offset(6.2f * s, 16.8f * s))
        }
        // 7 型：水样（两道波纹 + 上方水滴）
        else -> {
            path(tint, s, st) {
                moveTo(3.4f, 14.2f)
                cubicTo(5.4f, 11.4f, 7.4f, 11.4f, 9.4f, 14.2f)
                cubicTo(11.4f, 17f, 13.4f, 17f, 15.4f, 14.2f)
                cubicTo(17.4f, 11.4f, 19.4f, 11.4f, 21.4f, 14.2f)
            }
            path(tint, s, st) {
                moveTo(3.4f, 19.2f)
                cubicTo(5.4f, 16.4f, 7.4f, 16.4f, 9.4f, 19.2f)
                cubicTo(11.4f, 22f, 13.4f, 22f, 15.4f, 19.2f)
                cubicTo(17.4f, 16.4f, 19.4f, 16.4f, 21.4f, 19.2f)
            }
            // 上方水滴：圆 + 尖顶
            path(tint, s, st) {
                moveTo(12f, 3.4f)
                cubicTo(14.4f, 6.2f, 15.6f, 7.8f, 15.6f, 9.2f)
                cubicTo(15.6f, 11.2f, 14f, 12.6f, 12f, 12.6f)
                cubicTo(10f, 12.6f, 8.4f, 11.2f, 8.4f, 9.2f)
                cubicTo(8.4f, 7.8f, 9.6f, 6.2f, 12f, 3.4f)
                close()
            }
        }
    }
}
