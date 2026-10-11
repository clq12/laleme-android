package com.laxiang.app.ui.common

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 系统栏（状态栏 / 导航栏）避让工具 —— v2.5.3
 *
 * 背景：App 用 `enableEdgeToEdge()` 让内容铺满整屏（沉浸式），
 * 好处是背景色能一直延伸到底部手势条，视觉更完整；
 * 代价是**内容不会自动避开系统栏**——首页头部因此被状态栏压住，
 * 左上角的 App 图标和时间叠在一起。
 *
 * 为什么不用 Scaffold 的 `contentWindowInsets` 一把梭：
 * 本 App 的页面自己就是「滚动容器 + 顶部大标题」的结构
 * （LazyColumn 里第一条 item 是标题，不是 TopAppBar），
 * Scaffold 那套 inset 是给「有 TopAppBar / 有 bottomBar 的骨架屏」设计的，
 * 直接套用会和 NavigationBar 自己的 inset 叠加，底部多留一截空白。
 *
 * 所以这里的做法是：**顶部由内容自己加状态栏高度的首行留白，
 * 底部由 NavigationBar 处理导航条 inset**，各管一段，不重复也不遗漏。
 * 这样「滚动时内容能滚到状态栏下面」（沉浸式保留），
 * 但静止/初始状态不会被遮住任何一行文字。
 */
object LxInsets {

    /**
     * 状态栏高度。横屏状态下部分机型返回 0 或很小值，属正常。
     *
     * 注意：这里**不能**标 `@ReadOnlyComposable` ——
     * `asPaddingValues()` 内部会读取 LocalDensity，属于可组合读取，
     * 加了注解编译器会直接报「只能调用其它 @ReadOnlyComposable」。
     */
    val statusBarHeight: Dp
        @Composable
        get() = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    /**
     * 页面首行需要预留的顶部高度 = 状态栏 + 一段呼吸留白。
     *
     * 为什么固定加 16dp：状态栏下方紧贴一个 44dp 的图标块或 22sp 的标题，
     * 视觉上会「顶天」，加一段统一留白后各页节奏一致（与 LxSpacing.lg 对齐）。
     */
    val pageTopPadding: Dp
        @Composable
        get() = statusBarHeight + 16.dp

    /**
     * 底部安全高度（手势条 / 虚拟导航键）。
     *
     * 供 CameraScreen / EditorScreen 这类**没有 NavigationBar 兜底**的全屏页使用；
     * 有底部导航的页面不要用这个，否则会和 NavigationBar 的 inset 叠加。
     */
    val navigationBarHeight: Dp
        @Composable
        get() = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
}
