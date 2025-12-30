//=============================================================================
//
//  Copyright (c)
//  Qualcomm Technologies, Inc. and/or its subsidiaries.
//  All Rights Reserved.
//  Confidential and Proprietary - Qualcomm Technologies, Inc.
//
//=============================================================================

#include <chrono>
#include <exception>
#include <fstream>
#include <iomanip>
#include <iostream>
#include <string>
#include <unordered_map>
#include <unordered_set>
#include <vector>

#include "Logging.hpp"
#include "main.hpp"

std::string g_strAnswer;

/* -----------------------------------------------------------------------
 *  Callback for GenieDialog_query
 * ----------------------------------------------------------------------- */
void queryCallback(const char *responseStr,
                   const GenieDialog_SentenceCode_t sentenceCode,
                   const void *)
{
    if (sentenceCode == GENIE_DIALOG_SENTENCE_ABORT ||
        sentenceCode == GENIE_DIALOG_SENTENCE_END)
        return;

    if (responseStr)
    {
        if (sentenceCode == GENIE_DIALOG_SENTENCE_BEGIN)
            g_strAnswer.clear();

        g_strAnswer += responseStr;
    }
}

/* -----------------------------------------------------------------------
 *  DsConfig
 * ----------------------------------------------------------------------- */
DsConfig::DsConfig()
{
    m_handle = NULL;
}

DsConfig::~DsConfig()
{
    UnInit();
}

int DsConfig::Init(char *szConfigPath)
{
    if (m_handle != NULL)
    {
        LoggingWarning("Already Init !!!\n");
        return 0;
    }

    std::string strConfig;
    std::getline(std::ifstream(szConfigPath), strConfig, '\0');

    LoggingDebug("<<<<<<<<<< GenieDialogConfig_createFromJson(\"%s\");\n", szConfigPath);
    int32_t status = GenieDialogConfig_createFromJson(strConfig.c_str(), &m_handle);
    LoggingDebug(">>>>>>>>>> status=%d (path=%s)\n", status, szConfigPath);

    if ((GENIE_STATUS_SUCCESS != status) || m_handle == NULL)
    {
        LoggingCritic("Failed to create dialog config!!! status=%d\n", status);
        m_handle = NULL;
        return -1;
    }

    return 0;
}

void DsConfig::UnInit()
{
    if (m_handle == NULL)
    {
        LoggingWarning("Not Init !!!\n");
        return;
    }

    LoggingMax("<<<<<<<<<< GenieDialogConfig_free()\n");
    int32_t status = GenieDialogConfig_free(m_handle);
    LoggingMax(">>>>>>>>>> status=%d\n", status);

    if (status != GENIE_STATUS_SUCCESS)
    {
        LoggingCritic("Failed to free dialog config! status=%d\n", status);
    }

    m_handle = NULL;
}

/* -----------------------------------------------------------------------
 *  DsDialog
 * ----------------------------------------------------------------------- */
DsDialog::DsDialog()
{
    m_blReset = false;
    m_handle = NULL;
}

DsDialog::~DsDialog()
{
    UnInit();
}

int DsDialog::Init(DsConfig &config)
{
    if (m_handle != NULL)
    {
        LoggingWarning("Already Init !!!\n");
        return 0;
    }

    LoggingDebug("<<<<<<<<<< GenieDialog_create()\n");
    int32_t status = GenieDialog_create(config.GetHandle(), &m_handle);
    LoggingDebug(">>>>>>>>>> status=%d\n", status);

    if ((GENIE_STATUS_SUCCESS != status) || m_handle == NULL)
    {
        LoggingCritic("Failed to create GenieDialog! status=%d\n", status);
        m_handle = NULL;
        return -1;
    }

    return 0;
}

void DsDialog::UnInit()
{
    if (m_handle == NULL)
    {
        LoggingWarning("Not Init !!!\n");
        return;
    }

    LoggingMax("<<<<<<<<<< GenieDialog_free()\n");
    int32_t status = GenieDialog_free(m_handle);
    LoggingMax(">>>>>>>>>> status=%d\n", status);

    if (status != GENIE_STATUS_SUCCESS)
    {
        LoggingCritic("Failed to free dialog! status=%d\n", status);
    }

    m_handle = NULL;
}

std::string DsDialog::Query(const std::string strPrompt)
{
    if (m_handle == NULL)
    {
        LoggingWarning("Dialog Not Init !!!\n");
        return "";
    }

    if (m_blReset)
    {
        GenieDialog_reset(m_handle);
    }

    int32_t status = GenieDialog_query(
            m_handle,
            strPrompt.c_str(),
            GENIE_DIALOG_SENTENCE_COMPLETE,
            queryCallback,
            nullptr);

    if (status != GENIE_STATUS_SUCCESS)
    {
        LoggingCritic("Query failed! status=%d\n", status);
    }

    m_blReset = true;
    return g_strAnswer;
}

/* -----------------------------------------------------------------------
 *  ★ JNI에서 호출되는 핵심 함수: run_llm_once
 * ----------------------------------------------------------------------- */
std::string run_llm_once(const std::string &prompt)
{
    LoggingDebug("run_llm_once() called. prompt = %s\n", prompt.c_str());

    // Genie 모델 설정 JSON 파일 경로
    std::string configPath = "/data/local/tmp/llama-maum-skm-htp.json";

    // 1) Config 생성
    DsConfig config;
    if (config.Init((char *)configPath.c_str()) != 0)
    {
        LoggingCritic("Config init failed\n");
        return "[ERROR] Config Init failed";
    }

    // 2) Dialog 생성
    DsDialog dialog;
    if (dialog.Init(config) != 0)
    {
        LoggingCritic("Dialog init failed\n");
        return "[ERROR] Dialog Init failed";
    }

    // 3) Query 실행
    LoggingDebug("Executing Query...");
    std::string result = dialog.Query(prompt);

    if (result.empty())
    {
        LoggingDebug("Empty result received\n");
        return "[EMPTY RESPONSE]";
    }

    LoggingDebug("Query Result: %s\n", result.c_str());

    return result;
}
