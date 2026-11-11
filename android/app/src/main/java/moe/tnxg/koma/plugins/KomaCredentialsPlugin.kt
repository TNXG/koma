package moe.tnxg.koma.plugins

import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import moe.tnxg.koma.security.CredentialVault

/**
 * 凭据存储（契约见 src/platform/credentials.ts）。
 * save 的不可用用错误码 UNAVAILABLE 表达，其余失败用 SAVE_FAILED；
 * load 解密失败按 found=false 返回，并已删除坏条目。
 */
@CapacitorPlugin(name = "KomaCredentials")
class KomaCredentialsPlugin : Plugin() {
    private lateinit var vault: CredentialVault

    override fun load() {
        vault = CredentialVault(context)
    }

    @PluginMethod
    fun isAvailable(call: PluginCall) {
        call.resolve(JSObject().put("available", vault.isAvailable()))
    }

    @PluginMethod
    fun save(call: PluginCall) {
        val schoolId = call.getString("schoolId")?.trim()
        val username = call.getString("username")
        val password = call.getString("password")
        if (schoolId.isNullOrEmpty() || username.isNullOrEmpty() || password.isNullOrEmpty())
            return call.reject("schoolId/username/password 不能为空", "INVALID_ARGUMENT")
        if (!vault.isAvailable())
            return call.reject("凭据存储不可用", "UNAVAILABLE")
        if (!vault.save(schoolId, username, password))
            return call.reject("凭据保存失败", "SAVE_FAILED")
        call.resolve()
    }

    @PluginMethod
    fun load(call: PluginCall) {
        val schoolId = call.getString("schoolId")?.trim()
        if (schoolId.isNullOrEmpty())
            return call.reject("缺少 schoolId", "INVALID_ARGUMENT")
        val credential = if (vault.isAvailable()) vault.load(schoolId) else null
        val result = JSObject().put("found", credential != null)
        if (credential != null) {
            result.put("username", credential.username)
            result.put("password", credential.password)
        }
        call.resolve(result)
    }

    @PluginMethod
    fun remove(call: PluginCall) {
        val schoolId = call.getString("schoolId")?.trim()
        if (schoolId.isNullOrEmpty())
            return call.reject("缺少 schoolId", "INVALID_ARGUMENT")
        vault.remove(schoolId)
        call.resolve()
    }
}
