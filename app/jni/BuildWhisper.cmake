
set(WHISPER_DIR "${TGX_ROOT_DIR}/thirdparty/whispercpp")
set(GGML_DIR "${WHISPER_DIR}/ggml")

set(WHISPER_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(GGML_NATIVE OFF CACHE BOOL "" FORCE)
set(GGML_USE_CPU ON CACHE BOOL "" FORCE)

add_subdirectory("${WHISPER_DIR}" "${CMAKE_CURRENT_BINARY_DIR}/whisper_build")

foreach(_target whisper ggml ggml-cpu ggml-base)
  if(TARGET ${_target})
    target_compile_options(${_target} PRIVATE -fexceptions)
  endif()
endforeach()

if (${ANDROID_ABI} STREQUAL "arm64-v8a")
  foreach(_target ggml ggml-cpu)
    if(TARGET ${_target})
      target_compile_options(${_target} PRIVATE -O3 -funroll-loops)
    endif()
  endforeach()
elseif (${ANDROID_ABI} STREQUAL "armeabi-v7a")
  foreach(_target ggml ggml-cpu)
    if(TARGET ${_target})
      target_compile_options(${_target} PRIVATE -mfpu=neon -O3)
    endif()
  endforeach()
endif()

target_include_directories(whisper PUBLIC
        "${WHISPER_DIR}/include"
        "${WHISPER_DIR}/src"
        "${GGML_DIR}/include"
        "${GGML_DIR}/src"
)

