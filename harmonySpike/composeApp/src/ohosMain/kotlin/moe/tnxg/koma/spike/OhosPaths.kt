package moe.tnxg.koma.spike

import kotlinx.cinterop.*

/**
 * OHOS 应用沙箱路径探测：K/N 进程与 ArkTS 应用同 uid，直接访问 app 沙箱目录。
 * 候选路径按优先级探测，取第一个可写的。
 */
internal object OhosPaths {

    /** 应用 files 目录（探测失败返回 null，此时服务层退化为仅内存）。 */
    val filesDir: String? by lazy {
        candidates.firstOrNull { ensureDir(it) && canWrite(it) }
    }

    private val candidates = listOf(
        "/data/app/el2/100/base/moe.tnxg.koma/haps/entry/files",
        "/data/app/el2/100/base/moe.tnxg.koma/files",
        "/data/app/el2/100/base/moe_tnxg_koma/haps/entry/files",
        "/data/app/el2/100/base/moe_tnxg_koma/files",
    )

    private fun ensureDir(dir: String): Boolean = try {
        var created = ""
        for (segment in dir.split('/').filter { it.isNotEmpty() }) {
            created += "/$segment"
            platform.posix.mkdir(created, 0x1FFu.convert())
        }
        true
    } catch (_: Exception) {
        false
    }

    private fun canWrite(dir: String): Boolean = try {
        val probe = "$dir/.koma-probe"
        val fd = platform.posix.open(probe, platform.posix.O_WRONLY or platform.posix.O_CREAT or platform.posix.O_TRUNC, 384)
        if (fd >= 0) {
            val payload = "ok".encodeToByteArray()
            platform.posix.write(fd, payload.refTo(0), payload.size.convert())
            platform.posix.close(fd)
            platform.posix.unlink(probe)
            true
        } else false
    } catch (_: Exception) {
        false
    }

    /** 同步读整个文件（仅小文件：登录会话/设置 JSON 均 < 100KB）。 */
    fun readBytes(path: String): ByteArray? = try {
        val fd = platform.posix.open(path, platform.posix.O_RDONLY, 0)
        if (fd < 0) null
        else {
            val out = ArrayDeque<Byte>()
            val buffer = ByteArray(16 * 1024)
            while (true) {
                val n = platform.posix.read(fd, buffer.refTo(0), buffer.size.convert())
                if (n <= 0L) break
                for (i in 0 until n.toInt()) out.add(buffer[i])
            }
            out.toByteArray()
        }.also { platform.posix.close(fd) }
    } catch (_: Exception) {
        null
    }

    /** 同步写整个文件，权限 0600（会话/设置均为敏感数据）。 */
    fun writeBytes(path: String, bytes: ByteArray): Boolean = try {
        val fd = platform.posix.open(path, platform.posix.O_WRONLY or platform.posix.O_CREAT or platform.posix.O_TRUNC, 384)
        if (fd < 0) false
        else {
            var written = 0
            var ok = true
            while (written < bytes.size && ok) {
                val n = platform.posix.write(fd, bytes.refTo(written), (bytes.size - written).convert())
                if (n <= 0L) ok = false else written += n.toInt()
            }
            platform.posix.close(fd)
            ok
        }
    } catch (_: Exception) {
        false
    }

    fun delete(path: String): Boolean = try { platform.posix.unlink(path) == 0 } catch (_: Exception) { false }
}
