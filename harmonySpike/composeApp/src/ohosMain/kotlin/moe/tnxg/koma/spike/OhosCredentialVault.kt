package moe.tnxg.koma.spike

import kotlinx.cinterop.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import moe.tnxg.koma.assetstore.*
import moe.tnxg.koma.platform.SavedCredentials

/** Asset Store encrypts the secret for this app; no password or key is written to settings. */
internal class OhosCredentialVault {
    private val mutex = Mutex()
    private val alias = "koma.xjvut.credentials.v1".encodeToByteArray()

    suspend fun load(): SavedCredentials? = withContext(Dispatchers.Default) {
        mutex.withLock {
            memScoped {
                val query = allocArray<Asset_Attr>(2)
                query[0].bytes(ASSET_TAG_ALIAS, alias, this)
                query[1].number(ASSET_TAG_RETURN_TYPE, ASSET_RETURN_ALL)
                val result = alloc<Asset_ResultSet> { count = 0u; results = null }
                val status = OH_Asset_Query(query, 2u, result.ptr)
                if (status == ASSET_NOT_FOUND.toInt()) return@memScoped null
                checkStatus(status, "读取")
                try {
                    check(result.count == 1u) { "保存的教务凭据无效，请重新登录" }
                    val secret = OH_Asset_ParseAttr(result.results, ASSET_TAG_SECRET)?.pointed?.value?.blob
                        ?: error("保存的教务凭据无效，请重新登录")
                    val bytes = secret.data?.readBytes(secret.size.toInt())
                        ?: error("保存的教务凭据无效，请重新登录")
                    try {
                        val record = Json.parseToJsonElement(bytes.decodeToString()).jsonObject
                        SavedCredentials(record.getValue("username").jsonPrimitive.content,
                            record.getValue("password").jsonPrimitive.content)
                    } finally { bytes.fill(0) }
                } finally { OH_Asset_FreeResultSet(result.ptr) }
            }
        }
    }

    suspend fun save(credentials: SavedCredentials?) = withContext(Dispatchers.Default) {
        mutex.withLock {
            memScoped {
                if (credentials == null) {
                    val query = alloc<Asset_Attr>()
                    query.bytes(ASSET_TAG_ALIAS, alias, this)
                    val status = OH_Asset_Remove(query.ptr, 1u)
                    if (status != ASSET_NOT_FOUND.toInt()) checkStatus(status, "删除")
                } else {
                    val secret = buildJsonObject {
                        put("username", credentials.username)
                        put("password", credentials.password)
                    }.toString().encodeToByteArray()
                    try {
                        require(secret.size <= 1024) { "教务凭据过长，无法安全保存" }
                        val attrs = allocArray<Asset_Attr>(5)
                        attrs[0].bytes(ASSET_TAG_ALIAS, alias, this)
                        attrs[1].bytes(ASSET_TAG_SECRET, secret, this)
                        attrs[2].number(ASSET_TAG_ACCESSIBILITY, ASSET_ACCESSIBILITY_DEVICE_UNLOCKED)
                        attrs[3].number(ASSET_TAG_SYNC_TYPE, ASSET_SYNC_TYPE_NEVER)
                        attrs[4].number(ASSET_TAG_CONFLICT_RESOLUTION, ASSET_CONFLICT_OVERWRITE)
                        checkStatus(OH_Asset_Add(attrs, 5u), "保存")
                    } finally { secret.fill(0) }
                }
            }
        }
    }

    private fun Asset_Attr.bytes(key: UInt, bytes: ByteArray, scope: MemScope) {
        tag = key
        value.blob.size = bytes.size.toUInt()
        value.blob.data = scope.allocArrayOf(bytes).reinterpret()
    }

    private fun Asset_Attr.number(key: UInt, number: UInt) {
        tag = key
        value.u32 = number
    }

    private fun checkStatus(status: Int, action: String) {
        check(status == ASSET_SUCCESS.toInt()) { "无法${action}教务密码，请解锁设备后重试（$status）" }
    }
}
