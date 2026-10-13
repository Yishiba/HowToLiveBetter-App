package app.lifeguide.core.ui

/** 调起系统分享面板。各平台入口实现，经 Koin 注入。 */
fun interface Sharer {
    fun share(text: String)
}
