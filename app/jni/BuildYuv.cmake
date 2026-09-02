# yuv

set(YUV_DIR "${THIRDPARTY_DIR}/libyuv")

# libyuv tip enables AArch64 SME kernels via a compile probe, but the SME
# code uses __arm_new("za") — Clang 19+ syntax. NDK r27 ships Clang 18, the
# probe passes (the attribute-on-function form compiles) while the real
# sources fail. Force-disable SME until the toolchain catches up.
add_compile_definitions(LIBYUV_DISABLE_SME)

add_subdirectory("${YUV_DIR}"
  EXCLUDE_FROM_ALL
)

if(TARGET yuv_shared)
  set_target_properties(yuv_shared PROPERTIES
    EXCLUDE_FROM_ALL TRUE
    EXCLUDE_FROM_DEFAULT_BUILD TRUE
  )
endif()

target_include_directories(yuv PUBLIC
  "${YUV_DIR}/include"
)
