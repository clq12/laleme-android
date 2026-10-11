package com.laxiang.app.data

import android.content.Context

/**
 * 折叠分组的展开状态记忆。
 *
 * 用 SharedPreferences 而不是 Room：这是纯 UI 偏好，不需要同步到 WebDAV、
 * 也不需要参与备份导出，放进 Room 反而会污染数据表结构。
 *
 * 所有 key 都带 [PREFIX]，避免和后续其他偏好冲突。
 */
class SectionPrefs(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * 读取某个分组是否展开。没有记录时返回 [default]。
     */
    fun isExpanded(key: String, default: Boolean): Boolean =
        prefs.getBoolean(PREFIX + key, default)

    /**
     * 记录某个分组的展开状态。
     */
    fun setExpanded(key: String, expanded: Boolean) {
        prefs.edit().putBoolean(PREFIX + key, expanded).apply()
    }

    companion object {
        private const val PREFS_NAME = "laxiang_sections"
        private const val PREFIX = "expanded_"

        /** 设置页：数据同步 */
        const val SETTINGS_SYNC = "settings_sync"

        /** 设置页：本地存储 */
        const val SETTINGS_STORAGE = "settings_storage"

        /** 设置页：备份导出 */
        const val SETTINGS_BACKUP = "settings_backup"

        /** 统计页：概览与趋势 */
        const val STATS_OVERVIEW = "stats_overview"

        /** 统计页：分布分析 */
        const val STATS_DISTRIBUTION = "stats_distribution"

        /** 统计页：日历 */
        const val STATS_CALENDAR = "stats_calendar"

        /** 首页：今日概览 */
        const val HOME_TODAY = "home_today"
    }
}
