#include "libkn_api.h"
#include "napi/native_api.h"
#include "hilog/log.h"
#include <rawfile/raw_file_manager.h>
#include <dlfcn.h>
#include <vector>

// 避免工程侧未定义 LOG_DOMAIN 时编译失败
#ifndef LOG_DOMAIN
#define LOG_DOMAIN 0x0000
#endif

static napi_value MainArkUIViewController(napi_env env, napi_callback_info info) {
    size_t argc = 3;
    napi_value arguments[3] = {};
    if (napi_get_cb_info(env, info, &argc, arguments, nullptr, nullptr) != napi_ok || argc != 3) {
        napi_throw_type_error(env, nullptr, "filesDir, captcha recognizer and photo picker are required");
        return nullptr;
    }
    size_t length = 0;
    napi_valuetype type;
    if (napi_typeof(env, arguments[1], &type) != napi_ok || type != napi_function) {
        napi_throw_type_error(env, nullptr, "captcha recognizer must be a function");
        return nullptr;
    }
    if (napi_typeof(env, arguments[2], &type) != napi_ok || type != napi_function) {
        napi_throw_type_error(env, nullptr, "photo picker must be a function");
        return nullptr;
    }
    if (napi_get_value_string_utf8(env, arguments[0], nullptr, 0, &length) != napi_ok || length == 0) {
        napi_throw_type_error(env, nullptr, "filesDir must be a nonempty string");
        return nullptr;
    }
    std::vector<char> path(length + 1);
    if (napi_get_value_string_utf8(env, arguments[0], path.data(), path.size(), &length) != napi_ok ||
        path[0] != '/') {
        napi_throw_type_error(env, nullptr, "filesDir must be an absolute path");
        return nullptr;
    }
    return reinterpret_cast<napi_value>(MainArkUIViewController(env, path.data(), arguments[1], arguments[2]));
}

static napi_value CompleteCaptcha(napi_env env, napi_callback_info info) {
    size_t argc = 2;
    napi_value arguments[2] = {};
    int32_t id;
    size_t length = 0;
    if (napi_get_cb_info(env, info, &argc, arguments, nullptr, nullptr) != napi_ok || argc != 2 ||
        napi_get_value_int32(env, arguments[0], &id) != napi_ok ||
        napi_get_value_string_utf8(env, arguments[1], nullptr, 0, &length) != napi_ok) {
        napi_throw_type_error(env, nullptr, "captcha id and text are required");
        return nullptr;
    }
    std::vector<char> text(length + 1);
    if (napi_get_value_string_utf8(env, arguments[1], text.data(), text.size(), &length) != napi_ok) {
        return nullptr;
    }
    KomaCompleteCaptcha(id, text.data());
    napi_value result;
    napi_get_undefined(env, &result);
    return result;
}

// 选图结果回传：id + ArrayBuffer（JPEG 原始字节）。取消或失败时 data 为空数组。
static napi_value CompletePhotoPick(napi_env env, napi_callback_info info) {
    size_t argc = 2;
    napi_value arguments[2] = {};
    int32_t id;
    if (napi_get_cb_info(env, info, &argc, arguments, nullptr, nullptr) != napi_ok || argc != 2 ||
        napi_get_value_int32(env, arguments[0], &id) != napi_ok) {
        napi_throw_type_error(env, nullptr, "photo pick id and data are required");
        return nullptr;
    }
    bool is_arraybuffer = false;
    void *data = nullptr;
    size_t byte_length = 0;
    if (napi_is_arraybuffer(env, arguments[1], &is_arraybuffer) == napi_ok && is_arraybuffer &&
        napi_get_arraybuffer_info(env, arguments[1], &data, &byte_length) == napi_ok) {
        KomaCompletePhotoPick(id, static_cast<char *>(data), static_cast<int32_t>(byte_length));
    } else {
        KomaCompletePhotoPick(id, nullptr, 0);
    }
    napi_value result;
    napi_get_undefined(env, &result);
    return result;
}

static napi_value ReleaseCaptcha(napi_env env, napi_callback_info info) {
    KomaReleaseCaptcha();
    napi_value result;
    napi_get_undefined(env, &result);
    return result;
}

EXTERN_C_START
static napi_value Init(napi_env env, napi_value exports) {
    androidx_compose_ui_arkui_init(env, exports);
    napi_property_descriptor desc[] = {
        {"MainArkUIViewController", nullptr, MainArkUIViewController, nullptr, nullptr, nullptr, napi_default, nullptr},
        {"completeCaptcha", nullptr, CompleteCaptcha, nullptr, nullptr, nullptr, napi_default, nullptr},
        {"releaseCaptcha", nullptr, ReleaseCaptcha, nullptr, nullptr, nullptr, napi_default, nullptr},
        {"completePhotoPick", nullptr, CompletePhotoPick, nullptr, nullptr, nullptr, napi_default, nullptr},
    };
    napi_define_properties(env, exports, sizeof(desc) / sizeof(desc[0]), desc);
    return exports;
}
EXTERN_C_END

static napi_module demoModule = {
    .nm_version = 1,
    .nm_flags = 0,
    .nm_filename = nullptr,
    .nm_register_func = Init,
    .nm_modname = "entry",
    .nm_priv = ((void*)0),
    .reserved = { 0 },
};

extern "C" __attribute__((constructor)) void RegisterEntryModule(void)
{
    napi_module_register(&demoModule);
}
