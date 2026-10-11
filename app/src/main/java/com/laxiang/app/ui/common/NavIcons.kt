package com.laxiang.app.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * 底部导航与页面用的矢量图标集。
 *
 * 之前用 emoji（🏠📊📖⚙️）当图标，问题有两个：
 * 1. 不同系统/字体下 emoji 长得不一样，观感不可控；
 * 2. 选中态无法跟随主题色变化，和 Material3 的导航选中逻辑脱节。
 *
 * 这里全部用 Canvas 手绘路径，尺寸统一 24dp、线宽统一 1.8dp，
 * 颜色由调用方传入，能自然跟随 colorScheme。
 */
private const val ICON_SIZE = 24f
private const val STROKE_WIDTH = 1.8f

@Composable
private fun IconCanvas(
    tint: Color,
    modifier: Modifier = Modifier,
    draw: androidx.compose.ui.graphics.drawscope.DrawScope.(Float) -> Unit
) {
    Canvas(modifier = modifier.size(24.dp)) {
        val s = size.minDimension
        draw(s)
    }
}

/** 首页：房子轮廓 */
@Composable
fun IconHome(tint: Color, modifier: Modifier = Modifier) {
    IconCanvas(tint, modifier) { s ->
        val scale = s / ICON_SIZE
        fun p(x: Float, y: Float) = androidx.compose.ui.geometry.Offset(x * scale, y * scale)
        val stroke = Stroke(STROKE_WIDTH * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val roof = Path().apply {
            moveTo(3f, 10f)
            lineTo(12f, 3f)
            lineTo(21f, 10f)
        }
        val body = Path().apply {
            moveTo(5.5f, 9.5f)
            lineTo(5.5f, 20f)
            lineTo(18.5f, 20f)
            lineTo(18.5f, 9.5f)
        }
        val door = Path().apply {
            moveTo(10f, 20f)
            lineTo(10f, 14.5f)
            lineTo(14f, 14.5f)
            lineTo(14f, 20f)
        }
        drawPath(roof, tint, style = stroke)
        drawPath(body, tint, style = stroke)
        drawPath(door, tint, style = stroke)
    }
}

/** 统计：柱状图 */
@Composable
fun IconStats(tint: Color, modifier: Modifier = Modifier) {
    IconCanvas(tint, modifier) { s ->
        val scale = s / ICON_SIZE
        val stroke = Stroke(STROKE_WIDTH * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val axis = Path().apply {
            moveTo(4f * scale, 4f * scale)
            lineTo(4f * scale, 20f * scale)
            lineTo(20f * scale, 20f * scale)
        }
        drawPath(axis, tint, style = stroke)
        listOf(
            Triple(9f, 16f, 12f),
            Triple(13f, 12f, 16f),
            Triple(17f, 8f, 13f)
        ).forEach { (x, top, bottom) ->
            drawLine(
                color = tint,
                start = androidx.compose.ui.geometry.Offset(x * scale, top * scale),
                end = androidx.compose.ui.geometry.Offset(x * scale, bottom * scale),
                strokeWidth = STROKE_WIDTH * scale,
                cap = StrokeCap.Round
            )
        }
    }
}

/** 知识：书本 */
@Composable
fun IconBook(tint: Color, modifier: Modifier = Modifier) {
    IconCanvas(tint, modifier) { s ->
        val scale = s / ICON_SIZE
        val stroke = Stroke(STROKE_WIDTH * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val book = Path().apply {
            moveTo(4f, 5f)
            lineTo(10f, 5f)
            lineTo(12f, 7.5f)
            lineTo(14f, 5f)
            lineTo(20f, 5f)
            lineTo(20f, 18f)
            lineTo(14f, 18f)
            lineTo(12f, 20.5f)
            lineTo(10f, 18f)
            lineTo(4f, 18f)
            close()
        }
        val spine = Path().apply {
            moveTo(12f, 7.5f)
            lineTo(12f, 20.5f)
        }
        drawPath(book, tint, style = stroke)
        drawPath(spine, tint, style = stroke)
    }
}

/** 设置：齿轮（简化为圆 + 外齿点） */
@Composable
fun IconSettings(tint: Color, modifier: Modifier = Modifier) {
    IconCanvas(tint, modifier) { s ->
        val scale = s / ICON_SIZE
        val stroke = Stroke(STROKE_WIDTH * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val center = androidx.compose.ui.geometry.Offset(12f * scale, 12f * scale)
        drawCircle(
            color = tint,
            radius = 3.2f * scale,
            center = center,
            style = stroke
        )
        drawCircle(
            color = tint,
            radius = 7.5f * scale,
            center = center,
            style = stroke
        )
        // 四个方向的外齿短线
        listOf(
            androidx.compose.ui.geometry.Offset(12f, 2.5f) to androidx.compose.ui.geometry.Offset(12f, 4.5f),
            androidx.compose.ui.geometry.Offset(12f, 19.5f) to androidx.compose.ui.geometry.Offset(12f, 21.5f),
            androidx.compose.ui.geometry.Offset(2.5f, 12f) to androidx.compose.ui.geometry.Offset(4.5f, 12f),
            androidx.compose.ui.geometry.Offset(19.5f, 12f) to androidx.compose.ui.geometry.Offset(21.5f, 12f)
        ).forEach { (a, b) ->
            drawLine(
                color = tint,
                start = androidx.compose.ui.geometry.Offset(a.x * scale, a.y * scale),
                end = androidx.compose.ui.geometry.Offset(b.x * scale, b.y * scale),
                strokeWidth = STROKE_WIDTH * scale,
                cap = StrokeCap.Round
            )
        }
    }
}
