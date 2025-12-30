cmake_minimum_required(VERSION 3.10.2)

project("Llama2Genie")

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++14")

include_directories(${CMAKE_CURRENT_SOURCE_DIR})
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/Genie_inc)

set(APP_SOURCES
		native-lib.cpp
		main.cpp
		Logging.cpp
		LlmClient.cpp     # ← JNI 함수 있는 파일!!!
)

add_library(
		Llama2Genie
		SHARED
		${APP_SOURCES}
)

find_library(log-lib log)

target_link_libraries(
		Llama2Genie
		${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/arm64-v8a/libGenie.so
		android
		${log-lib}
)
