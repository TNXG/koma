package moe.tnxg.koma.data.education.xjvut

import okhttp3.HttpUrl

internal object Endpoints {
    const val CAS = "https://qyrz.xjvut.edu.cn"
    const val LOGIN = "$CAS/cas/login"
    const val JW = "https://jw.xjvut.edu.cn:6082"
    const val SSO = "$JW/sso/zfiotlogin?url=kbcx/xskbcxMobile_cxXskbcxIndex.html?gnmkdm=Y253510&layout=default"
    const val COURSES = "$JW/jwglxt/kbcx/xskbcxMobile_cxXsKb.html"
    const val WEEKS = "$JW/jwglxt/kbcx/xskbcxMobile_cxZc.html"
    const val TIMES = "$JW/jwglxt/kbcx/xskbcx_cxRjc.html?gnmkdm=N2151"
    fun isIndex(url: HttpUrl): Boolean = url.isHttps && url.host == "jw.xjvut.edu.cn" && url.port == 6082 &&
        url.encodedPath.endsWith("/xskbcxMobile_cxXskbcxIndex.html")
}
