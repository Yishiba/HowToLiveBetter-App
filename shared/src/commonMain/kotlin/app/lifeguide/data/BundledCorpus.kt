package app.lifeguide.data

import app.lifeguide.resources.Res
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi

/** 打进安装包的那份正文（:shared:syncCorpus 生成） */
@OptIn(ExperimentalResourceApi::class)
object BundledCorpus {
    private const val BASE = "files/corpus/"

    suspend fun manifest(): CorpusManifest =
        Json.decodeFromString(CorpusManifest.serializer(), read("manifest.json"))

    suspend fun read(path: String): String = Res.readBytes(BASE + path).decodeToString()
}
