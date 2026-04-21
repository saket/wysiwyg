# Android-safe CMake config for the tree-sitter-markdown grammar.
#
# The ktreesitter-plugin's generated CMakeLists.txt uses find_package(JNI REQUIRED), which
# fails under AGP's externalNativeBuild — the NDK toolchain doesn't satisfy FindJNI's
# desktop-JDK expectations. This file bypasses that by relying on the NDK's sysroot for
# jni.h. Tracking: https://github.com/tree-sitter/kotlin-tree-sitter/issues/<TODO: filed>
#
# GRAMMAR_DIR and GENERATED_DIR are passed in from the module's build.gradle via
# externalNativeBuild.cmake.arguments.

cmake_minimum_required(VERSION 3.22.1)
project(ktreesitter-markdown LANGUAGES C)

set(CMAKE_C_STANDARD 11)
set(CMAKE_C_VISIBILITY_PRESET hidden)
add_compile_options(-Wall -Wextra -Wno-unused-parameter -Werror=implicit-function-declaration)
add_compile_definitions(TREE_SITTER_HIDE_SYMBOLS)

if(NOT DEFINED GRAMMAR_DIR OR NOT DEFINED GENERATED_DIR)
  message(FATAL_ERROR "GRAMMAR_DIR and GENERATED_DIR must be passed via -D")
endif()

include_directories(${GRAMMAR_DIR}/bindings/c/tree-sitter)

add_library(ktreesitter-markdown SHARED
  ${GENERATED_DIR}/src/jni/binding.c
  ${GRAMMAR_DIR}/src/parser.c
  ${GRAMMAR_DIR}/src/scanner.c
)

# TODO: remove once https://github.com/tree-sitter/kotlin-tree-sitter/issues/59 is resolved.
# Forces 16 KB page alignment in the ELF LOAD segments, required for Android devices using
# 16 KB memory pages (Pixel 8+ and other newer ARM hardware running Android 15+).
target_link_options(ktreesitter-markdown PRIVATE "-Wl,-z,max-page-size=16384")

set_target_properties(ktreesitter-markdown PROPERTIES DEFINE_SYMBOL "")
