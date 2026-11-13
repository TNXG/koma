package moe.tnxg.koma.platform

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.tnxg.koma.data.education.XjvutEduService
import moe.tnxg.koma.ocr.OcrEngine
import moe.tnxg.koma.platform.migration.LegacyTimetableMigration
import moe.tnxg.koma.platform.notification.AndroidNotificationService
import moe.tnxg.koma.platform.notification.NotificationPermissionHost
import moe.tnxg.koma.platform.photo.LocalPhotoStore
import moe.tnxg.koma.security.CredentialVault
import java.io.File

class AndroidPlatformServices(context: Context) : PlatformServices {
    private val app = context.applicationContext
    private val settings = app.getSharedPreferences("koma_settings", Context.MODE_PRIVATE)
    private val vault = CredentialVault(app)
    override val edu = XjvutEduService(File(app.filesDir, "edu_cookies"))
    override val canRememberCredentials: Boolean get() = vault.isAvailable()
    override val notifications: NotificationService = AndroidNotificationService(
        app, context as? NotificationPermissionHost,
    )
    override val images: ImageServices = LocalPhotoStore(context)

    override fun readSetting(key: String): String? = settings.getString(key, null)

    override fun writeSetting(key: String, value: String?) {
        check(settings.edit().putString(key, value).commit()) { "本地设置保存失败" }
    }

    override suspend fun migrateLegacySettings() {
        LegacyTimetableMigration(app, settings).migrate()
    }

    override suspend fun loadCredentials(): SavedCredentials? = withContext(Dispatchers.IO) {
        vault.load("xjvut")?.let { SavedCredentials(it.username, it.password) }
    }

    override suspend fun saveCredentials(value: SavedCredentials?) = withContext(Dispatchers.IO) {
        if (value == null) vault.remove("xjvut")
        else check(vault.save("xjvut", value.username, value.password)) { "安全凭据保存失败" }
    }

    override suspend fun recognizeCaptcha(dataUrl: String, view: Int): String = withContext(Dispatchers.Default) {
        val encoded = if (dataUrl.startsWith("data:")) dataUrl.substringAfter(',', "") else dataUrl
        val bytes = Base64.decode(encoded, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: throw IllegalArgumentException("图片解不开")
        try {
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            val engine = OcrEngine.shared(app)
            synchronized(engine) { engine.recognize(pixels, bitmap.width, bitmap.height, view) }
        } finally {
            bitmap.recycle()
        }
    }
}
