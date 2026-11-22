package moe.tnxg.koma.spike

import kotlinx.cinterop.*

/** The host supplies its sandbox path before any platform service is constructed. */
internal object OhosPaths {
    lateinit var filesDir: String
        private set

    fun initialize(path: String) {
        require(path.startsWith('/')) { "应用存储目录无效" }
        filesDir = path
    }

    /** 同步读整个文件；封面缓存也有数 MB，按块直读避免逐字节装箱。 */
    fun readBytes(path: String): ByteArray? = try {
        val fd = platform.posix.open(path, platform.posix.O_RDONLY, 0)
        if (fd < 0) null else readFully(fd)
    } catch (_: Exception) {
        null
    }

    private fun readFully(fd: Int): ByteArray {
        try {
            var out = ByteArray(16 * 1024)
            var total = 0
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val n = platform.posix.read(fd, buffer.refTo(0), buffer.size.convert())
                if (n <= 0L) break
                val count = n.toInt()
                if (total + count > out.size) {
                    var capacity = out.size
                    while (capacity < total + count) capacity = capacity shl 1
                    out = out.copyOf(capacity)
                }
                buffer.copyInto(out, total, 0, count)
                total += count
            }
            return if (total == out.size) out else out.copyOf(total)
        } finally {
            platform.posix.close(fd)
        }
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
