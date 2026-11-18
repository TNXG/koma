package moe.tnxg.koma.data.education.xjvut

import io.ktor.http.Url

internal object Endpoints {
    const val CAS = "https://qyrz.xjvut.edu.cn"
    const val LOGIN = "$CAS/cas/login"
    const val JW = "https://jw.xjvut.edu.cn:6082"
    const val SSO = "$JW/sso/zfiotlogin?url=kbcx/xskbcxMobile_cxXskbcxIndex.html?gnmkdm=Y253510&layout=default"
    const val COURSES = "$JW/jwglxt/kbcx/xskbcxMobile_cxXsKb.html"
    const val WEEKS = "$JW/jwglxt/kbcx/xskbcxMobile_cxZc.html"
    const val TIMES = "$JW/jwglxt/kbcx/xskbcx_cxRjc.html?gnmkdm=N2151"
    fun isLogin(url: Url): Boolean = url.protocol == io.ktor.http.URLProtocol.HTTPS &&
        url.host == "qyrz.xjvut.edu.cn" && url.port == 443 &&
        url.pathSegments.joinToString("/").let { it == "cas/login" || it.endsWith("/cas/login") }
    fun isIndex(url: Url): Boolean = url.protocol == io.ktor.http.URLProtocol.HTTPS &&
        url.host == "jw.xjvut.edu.cn" && url.port == 6082 &&
        url.pathSegments.lastOrNull()?.endsWith("xskbcxMobile_cxXskbcxIndex.html") == true
}
