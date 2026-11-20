package moe.tnxg.koma.data.education.cas

/**
 * Standalone host entry point: compile together with the actual ohosMain Cas.kt
 * and BigInt.kt, not the Android implementations. Only the Term model is external.
 *
 * Golden vectors use the 1024-bit PKCS#1 v2.1 example public modulus and exponent
 * 65537. They were independently generated with the deployed public scripts:
 * https://qyrz.xjvut.edu.cn/cas/js/security.js
 * https://qyrz.xjvut.edu.cn/cas/themes/mb3/js/cas.js
 * ASCII vectors additionally use Python's integer pow(message, 65537, modulus).
 * Unicode vectors intentionally preserve the JS implementation's overflowing
 * 16-bit digits and unsigned-32-bit carry truncation, not idealized RSA packing.
 */
private const val MODULUS =
    "a8b3b284af8eb50b387034a860f146c4919f318763cd6c5598c8ae4811a1e0abc" +
    "4c7e0b082d693a5e7fced675cf4668512772c0cbc64a742c6c630f533c8cc72f" +
    "62ae833c40bf25842e984bb78bdbf97c0107d55bdb662f5c4e0fab9845cb5148" +
    "ef7392dd3aaff93ae1e6b667bb3d4247616d4f5ba10d4cfd226de88d39f16fb"

fun main() {
    var assertions = 0
    fun equal(label: String, expected: Any?, actual: Any?) {
        check(expected == actual) { "Harmony CAS regression failed: $label" }
        assertions++
    }
    fun rsa(label: String, password: String, expected: String, modulus: String = MODULUS) {
        equal(label, expected, encryptCasPassword(password, modulus, "10001"))
    }

    equal("odd-nibble exponent", "010001", BigInt.fromHex("10001").toHexString())
    equal("odd-nibble modulus", MODULUS.dropLast(1).padStart(256, '0'),
        BigInt.fromHex(MODULUS.dropLast(1)).toHexString())
    equal("modulus bit length", 1024, BigInt.fromHex(MODULUS).bitLength())
    equal("bounded division", "02", BigInt.fromHex("2a").mod(BigInt.fromHex("5")).toHexString())
    equal("limb carry shift", "0100000000", BigInt.fromHex("1").shl(32).toHexString())
    equal("limb carry right shift", "01", BigInt.fromHex("100000000").shr(32).toHexString())
    equal("legacy packing without exponentiation", "7465737431323334",
        encryptCasPassword("test1234", MODULUS, "1"))

    rsa("ASCII real exponent", "test1234",
        "1d9d86f3e17d26e5b5f23377970aa01953ee19bc5be2cefc7549722cd7c1c877b4f1036b6286764655ee07fffcc98ddfd2ad67de265a5c8e48adba9f6d5342e45139c37e13fd700746aa1d9c0bda1fc7857ed8e14a21d61e039237f0391b142e22eb7d18e3e18762efae7a95431ae9bc80b7a82b43460eb1c32fd4be3c2b4569")
    rsa("form punctuation", "P@ss+word&=123",
        "1659951aad4cbe1b76a8817c531ac52e86dad5877f22f436792d3e2d3e2c06baab1902a813bd6e4c5ba3e62a2a39b87bcc7e265e45ba84f1c26dc0a6b5ce6e8ca9a02f2686d8ba8ee84e6edf81a1de5ed05ba357de14ddd0547a4cd8dce8b27eb1f35e199195adf603afbae522ce488d20b08dcccc27eff34eb8c72d8ae4e706")
    rsa("Latin-1", "café123",
        "501357d294693f04ccae55214e8d39117735478091070abebbb3756a356e7dde7ef1baedeea30dedf2a287f68bc04fc4fd43f800a3b870b62cf69a028ff8d7ade8cad5c38086138a95ea5d7ff22ab72f47766ed2769948d509e63dcda6d9f531fcd5c4bb2fa392f5d0555d1188af12f5e805f3b6fc8462e0f78ade830d8b7105")
    rsa("BMP UTF-16 legacy multiplication", "密码123",
        "7f73afeace19880548e91463a4cc3656038923ff60bba68ce7b7ec0ba44d5368ed853ca81ec62b3e27c954c02b738db1827510ba923dd742c436f065bdaf33f644012b3a4cc38aa5513ba8e26dd27512c608cad38492d3fd06ecf2aadf3ca129af116c9100b1140b5b858f2e1a018f7390aac0e29dc45983dcf2e0d141fd06b2")
    rsa("surrogate units reversed separately", "A\uD83D\uDE00Z",
        "2f8b2b00311dfbe938f097553a810584bdf83ed170eb44bc2c6ce82e073879af5e2f2419cfeaaf7cabfc701d47025218affd7a277e9c3158454cdd0c0bad89518690fa9a31f834d216704f7f7c6936c404386ed9f2a90f9367155d83702f75a79d82275a7c20b1bba2073ae62bfb592477db0af2bd89ed767bb00788da209e0f")
    rsa("overlapping UTF-16 digit bits", "ĀĀ",
        "5bd7350f2eed11e0435ed9bcc64284a80b6b0f8a2ac20d6071fa8b9e05911103b89dd56e06efd3ed1b53d4447d4ad2142b3eeb70c025a5b6832d5a66d41f89511c37c022e75207fc83368e693e91d3191e6cb48fa30abc27a73db33b7569dbe2df5706b215cfafe3bfd9adb941c80652a637c704022b2a527afefe4085c08a11")
    rsa("126-unit chunk boundary", "x".repeat(127),
        "a62568e156b070c56b62a939e5577c598fc9918e0b899688a6fb835947fb917ab9e5328fd6d6199e775b13f9e2ed9ff6101e03f12e7efc84ddf51944ab9f3cf56b0de6133f373389af14ead965a4998038557858b8dbd423d68636b5f861889b9f56dbc76000a3555a0f127feb6b39dbfd7a3686237a07db97182222a41ef58e 3a4be52e2a0e63a9bec7912340e61d0b8c0ff19864847906fa6f108cf0ee5dfafa8441d4c945c69e934c85daf557b6d96ee14bef559098768c6912d22b719d88a26393d6c28a356dc1b6524b5ccfd1f78387b0db827810ca453923099a18d274015e271f9c13602e666845eabd2db5a9d0e097bf4dc482cac3d6c07c119bfad7")
    rsa("non-word-aligned modulus keeps 126-unit chunk", "x".repeat(125),
        "044413f820a2455f5e91e5ad34aa19693759e993de40ce69c0d0129d25eda3ab834d1b64bbab76854b2748ca489b2edab0cab962cf83b2e4c00f4dbfbae9fbd792b85ddf9af14c5b8255d8ba29f7fc0de8790630772171441bd8214a5dd3d48acbe1f99443da097371aa62d4469ba504bc4c849ea787536a352ca104a6842dea",
        MODULUS.dropLast(1))

    for (quote in listOf("'", "\"")) {
        val captcha = casErrorText("<p class=${quote}bottom-error-text$quote></p><span class=${quote}errors auth_error$quote><b>FailedAuthcodeException:</b> 验证码错误</span>")
        equal("nested captcha text with $quote", "FailedAuthcodeException: 验证码错误", captcha)
        equal("captcha classification with $quote", true, isCaptchaError(captcha))
        equal("captcha description with $quote", "验证码不对", describeCasError(captcha))
        val account = casErrorText("<div class=${quote}top-error-text$quote>password not recognized</div>")
        equal("account description with $quote", "账号或密码不对", describeCasError(account))
        val session = casErrorText("<div class=${quote}top-error-text$quote><b>Invalid</b> execution flow</div>")
        equal("session description with $quote", "登录过期了，请重试", describeCasError(session))
        equal("session is not captcha with $quote", false, isCaptchaError(session))
    }
    println("Harmony CAS regression: $assertions assertions passed")
}
