#include <jni.h>
#include <string>
#include <vector>
#include <algorithm>
#include <cctype>
#include <sstream>
#include <map>
#include <curl/curl.h>

extern "C" {

using namespace std;

/* ---------- Utility Functions ---------- */
static string toLower(const string& s) {
    string r = s;
    transform(r.begin(), r.end(), r.begin(),
              [](unsigned char c){ return std::tolower(c); });
    return r;
}
static string trim(const string& s) {
    size_t start = s.find_first_not_of(" \t\n\r");
    if (start == string::npos) return "";
    size_t end = s.find_last_not_of(" \t\n\r");
    return s.substr(start, end - start + 1);
}

/* Escape JSON string (quotes, backslashes, control chars) */
static string escapeJson(const string& s) {
    string out;
    out.reserve(s.size() * 2);
    for (char c : s) {
        switch (c) {
            case '\"': out += "\\\\\\\""; break;
            case '\\': out += "\\\\\\\\"; break;
            case '\n': out += "\\\\n"; break;
            case '\r': out += "\\\\r"; break;
            case '\t': out += "\\\\t"; break;
            default: out += c;
        }
    }
    return out;
}

/* ---------- Character Model ---------- */
struct Character {
    string name;
    string appearance;
    string personality;
    string speakingStyle;
    string worldview;
    string scenarioPrompt;
    bool isPublic;

    Character() : isPublic(false) {}

    Character(string n, string a, string p,
              string s, string w, string sp, bool pub)
        : name(n), appearance(a), personality(p), speakingStyle(s),
          worldview(w), scenarioPrompt(sp), isPublic(pub) {}

    string buildSystemPrompt(const string& userInput = "",
                             const string& scenario = "") const {
        ostringstream oss;
        oss << "당신은 다음 캐릭터로 완전히 immersion하여 말합니다.\n\n";
        oss << "[캐릭터 정보]\n";
        oss << "이름: " << name << "\n";
        oss << "외모: " << appearance << "\n";
        oss << "성격: " << personality << "\n";
        oss << "말투: " << speakingStyle << "\n";
        oss << "세계관: " << worldview << "\n\n";

        oss << "[중요 지침]\n";
        oss << "1. 위 캐릭터의 성격과 말투를 일관되게 유지하세요\n";
        oss << "2. 세계관을 벗어나는 내용은 생성하지 마세요 (단, 사용자가 명시적으로 새로운 상황을 설정할 경우 예외)\n";
        oss << "3. 감성적이고 섬세한 표현을 우선시하세요\n";
        oss << "4. 사용자의 입력에 캐릭터로서 자연스럽게 반응하세요\n\n";

        if (!scenario.empty())
            oss << "[현재 상황]\n" << scenario << "\n\n";

        if (!userInput.empty())
            oss << "[사용자 입력]\n" << userInput << "\n[캐릭터 응답: ]\n";

        return oss.str();
    }
};

/* JSON helpers for Character */
static string characterToJson(const Character& c) {
    ostringstream oss;
    oss << "{"
        << "\"name\":\"" << escapeJson(c.name) << "\","
        << "\"appearance\":\"" << escapeJson(c.appearance) << "\","
        << "\"personality\":\"" << escapeJson(c.personality) << "\","
        << "\"speakingStyle\":\"" << escapeJson(c.speakingStyle) << "\","
        << "\"worldview\":\"" << escapeJson(c.worldview) << "\","
        << "\"scenarioPrompt\":\"" << escapeJson(c.scenarioPrompt) << "\","
        << "\"isPublic\":" << (c.isPublic ? "true" : "false")
        << "}";
    return oss.str();
}

/* ---------- Global character storage (shared across sessions) ---------- */
static vector<Character> gCharacters;

/* ---------- Per‑session selected character index ---------- */
static map<string, int> gSessionCharIndex;

/* ---------- Simple Conversation Memory (In‑Memory) ---------- */
static map<string, vector<pair<string,string>>> gConversations;

static void addMessage(const string& sessionId,
                       const string& role,
                       const string& content) {
    gConversations[sessionId].emplace_back(role, content);
    if (gConversations[sessionId].size() > 10)
        gConversations[sessionId].erase(gConversations[sessionId].begin());
}

static string getContext(const string& sessionId) {
    if (gConversations.find(sessionId) == gConversations.end())
        return "";
    ostringstream oss;
    oss << "[이전 대화 기록]\n";
    for (auto& p : gConversations[sessionId])
        oss << p.first << ": " << p.second << "\n";
    return oss.str();
}

/* ---------- Curl Helper for HTTP Requests ---------- */
static size_t WriteCallback(void* contents, size_t size, size_t nmemb, void* userp) {
    ((std::string*)userp)->append((char*)contents, size * nmemb);
    return size * nmemb;
}

static string httpPost(const string& url, const string& jsonData,
                       const vector<pair<string,string>>& extraHeaders = {}) {
    CURL* curl;
    CURLcode res;
    string readBuffer;

    curl = curl_easy_init();
    if(curl) {
        curl_easy_setopt(curl, CURLOPT_URL, url.c_str());
        curl_easy_setopt(curl, CURLOPT_POSTFIELDS, jsonData.c_str());
        curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, WriteCallback);
        curl_easy_setopt(curl, CURLOPT_WRITEDATA, &readBuffer);

        struct curl_slist* headers = nullptr;
        headers = curl_slist_append(headers, "Content-Type: application/json");
        for (const auto& h : extraHeaders) {
            string headerLine = h.first + ": " + h.second;
            headers = curl_slist_append(headers, headerLine.c_str());
        }
        curl_easy_setopt(curl, CURLOPT_HTTPHEADER, headers);

        res = curl_easy_perform(curl);
        curl_easy_cleanup(curl);
        curl_slist_free_all(headers);

        if(res != CURLE_OK) {
            fprintf(stderr, "curl_easy_perform() failed: %s\n", curl_easy_strerror(res));
            return "";
        }
    }
    return readBuffer;
}

/* ---------- OpenRouter API Integration (DeepSeek) ---------- */
static string getOpenRouterResponse(const string& prompt) {
    string url = "https://openrouter.ai/api/v1/chat/completions";

    // Build JSON payload
    string jsonPayload = R"({"model":"deepseek-chat","messages":[{"role":"user","content":"") +
                         prompt + R"("}],"temperature":0.7,"max_tokens":512,"top_p":0.9,"stream":false})";

    // Authorization header with the provided key
    vector<pair<string,string>> headers;
    headers.emplace_back("Authorization", "Bearer YOUR_API_KEY_HERE");

    string response = httpPost(url, jsonPayload, headers);

    // Parse JSON response to extract "choices[0].message.content"
    // Simple parsing: find '"content":"' then capture until next '"'
    size_t contentPos = response.find(R"("content":"")");
    if (contentPos == string::npos) {
        return "오류: OpenRouter 응답을 파싱할 수 없습니다.";
    }
    contentPos += strlen(R"("content":"")");
    size_t endPos = response.find('"', contentPos);
    if (endPos == string::npos) {
        return "오류: OpenRouter 응답 형식이 잘못됨.";
    }
    return response.substr(contentPos, endPos - contentPos);
}

/* ---------- JNI Interface ---------- */

/**
 * 초기화: 세션 ID와 (선택적) 캐릭터 인덱스를 설정.
 * 캐릭터 인덱스가 -1이면 기존에 저장된 인덱스가 없으면 첫 번째 캐릭터(0)를 선택.
 */
JNIEXPORT void JNICALL
Java_com_example_zetachat_NativeChat_initSession(
        JNIEnv* env, jobject /* this */,
        jstring jSessionId, jint characterIdx) {

    const char* nativeSessionId = env->GetStringUTFChars(jSessionId, nullptr);
    string sessionId(nativeSessionId);
    env->ReleaseStringUTFChars(jSessionId, nativeSessionId);

    // Reset conversation for this session (optional)
    gConversations[sessionId].clear();

    // Determine which character to use for this session
    int idx = characterIdx;
    if (idx < 0) {
        // Try to load previously selected index for this session
        auto it = gSessionCharIndex.find(sessionId);
        if (it != gSessionCharIndex.end()) {
            idx = it->second;
        } else {
            // No previous selection → default to first character if any, else -1
            idx = (gCharacters.empty() ? -1 : 0);
        }
    }
    // Clamp index to valid range
    if (idx < 0) idx = -1;
    if (idx >= static_cast<int>(gCharacters.size())) idx = -1;

    gSessionCharIndex[sessionId] = idx;

    // Also store the selected index as a system message (helps debugging)
    ostringstream charInfo;
    charInfo << "[SELECTED_CHAR_INDEX:" << idx << "]";
    addMessage(sessionId, "system", charInfo.str());
}

/**
 * 새 캐릭터를 전역 목록에 추가하고 해당 인덱스를 반환.
 * 실패 시 -1 반환.
 */
JNIEXPORT jlong JNICALL
Java_com_example_zetachat_NativeChat_addCharacter(
        JNIEnv* env, jobject /* this */,
        jstring jName, jstring jAppearance, jstring jPersonality,
        jstring jSpeakingStyle, jstring jWorldview, jstring jScenarioPrompt,
        jboolean jIsPublic) {

    const char* cName = env->GetStringUTFChars(jName, nullptr);
    const char* cAppearance = env->GetStringUTFChars(jAppearance, nullptr);
    const char* cPersonality = env->GetStringUTFChars(jPersonality, nullptr);
    const char* cSpeakingStyle = env->GetStringUTFChars(jSpeakingStyle, nullptr);
    const char* cWorldview = env->GetStringUTFChars(jWorldview, nullptr);
    const char* cScenarioPrompt = env->GetStringUTFChars(jScenarioPrompt, nullptr);
    jboolean isPublic = jIsPublic;

    string name(cName ? cName : "");
    string appearance(cAppearance ? cAppearance : "");
    string personality(cPersonality ? cPersonality : "");
    string speakingStyle(cSpeakingStyle ? cSpeakingStyle : "");
    string worldview(cWorldview ? cWorldview : "");
    string scenarioPrompt(cScenarioPrompt ? cScenarioPrompt : "");

    env->ReleaseStringUTFChars(jName, cName);
    env->ReleaseStringUTFChars(jAppearance, cAppearance);
    env->ReleaseStringUTFChars(jPersonality, cPersonality);
    env->ReleaseStringUTFChars(jSpeakingStyle, cSpeakingStyle);
    env->ReleaseStringUTFChars(jWorldview, cWorldview);
    env->ReleaseStringUTFChars(jScenarioPrompt, cScenarioPrompt);

    Character ch(name, appearance, personality, speakingStyle,
                 worldview, scenarioPrompt, isPublic != JNI_FALSE);
    gCharacters.push_back(ch);
    return static_cast<jlong>(gCharacters.size() - 1); // zero‑based index
}

/**
 * 전역 캐릭터 목록의 개수 반환.
 */
JNIEXPORT jint JNICALL
Java_com_example_zetachat_NativeChat_getCharacterCount(
        JNIEnv* env, jobject /* this */) {
    return static_cast<jint>(gCharacters.size());
}

/**
 * 지정된 인덱스의 캐릭터 정보를 JSON 문자열로 반환.
 * 인덱스가 범위를 벗어나면 빈 문자열 반환.
 */
JNIEXPORT jstring JNICALL
Java_com_example_zetachat_NativeChat_getCharacterInfo(
        JNIEnv* env, jobject /* this */,
        jlong index) {
    if (index < 0 || index >= static_cast<jlong>(gCharacters.size())) {
        return env->NewStringUTF("");
    }
    const Character& ch = gCharacters[static_cast<size_t>(index)];
    string json = characterToJson(ch);
    return env->NewStringUTF(json.c_str());
}

/**
 * 전역 캐릭터 목록 전체를 JSON 배열 형태로 반환.
 */
JNIEXPORT jstring JNICALL
Java_com_example_zetachat_NativeChat_getCharactersJson(
        JNIEnv* env, jobject /* this */) {
    ostringstream oss;
    oss << "[";
    for (size_t i = 0; i < gCharacters.size(); ++i) {
        if (i > 0) oss << ",";
        oss << characterToJson(gCharacters[i]);
    }
    oss << "]";
    return env->NewStringUTF(oss.str().c_str());
}

/**
 * 현재 세션에 선택된 캐릭터 인덱스를 설정.
 * 인덱스가 유효하지 않으면 무시.
 */
JNIEXPORT void JNICALL
Java_com_example_zetachat_NativeChat_selectCharacter(
        JNIEnv* env, jobject /* this */,
        jstring jSessionId, jlong index) {

    const char* nativeSessionId = env->GetStringUTFChars(jSessionId, nullptr);
    string sessionId(nativeSessionId);
    env->ReleaseStringUTFChars(jSessionId, nativeSessionId);

    if (index < 0 || index >= static_cast<jlong>(gCharacters.size())) {
        // ignore invalid index
        return;
    }
    gSessionCharIndex[sessionId] = static_cast<int>(index);
}

/**
 * 현재 세션에 선택된 캐릭터 인덱스를 반환.
 * 선택된 것이 없으면 -1 반환.
 */
JNIEXPORT jlong JNICALL
Java_com_example_zetachat_NativeChat_getSelectedCharacter(
        JNIEnv* env, jobject /* this */,
        jstring jSessionId) {

    const char* nativeSessionId = env->GetStringUTFChars(jSessionId, nullptr);
    string sessionId(nativeSessionId);
    env->ReleaseStringUTFChars(jSessionId, nativeSessionId);

    auto it = gSessionCharIndex.find(sessionId);
    if (it == gSessionCharIndex.end())
        return -1;
    return static_cast<jlong>(it->second);
}

/**
 * 사용자 메시지를 입력받아 OpenRouter 모델에 요청하고 응답을 반환.
 * 선택된 캐릭터가 있으면 해당 캐릭터의 시스템 프롬프트를 프롬프트 앞쪽에 붙인다.
 */
JNIEXPORT jstring JNICALL
Java_com_example_zetachat_NativeChat_sendMessage(
        JNIEnv* env, jobject /* this */,
        jstring jSessionId, jstring jUserMessage) {

    const char* nativeSessionId = env->GetStringUTFChars(jSessionId, nullptr);
    const char* nativeUserMessage = env->GetStringUTFChars(jUserMessage, nullptr);
    string sessionId(nativeSessionId);
    string userMessage(nativeUserMessage);
    env->ReleaseStringUTFChars(jSessionId, nativeSessionId);
    env->ReleaseStringUTFChars(jUserMessage, nativeUserMessage);

    // Determine selected character for this session
    int characterIdx = -1;
    auto it = gSessionCharIndex.find(sessionId);
    if (it != gSessionCharIndex.end())
        characterIdx = it->second;

    // Build system prompt if a character is selected
    string systemPrompt;
    if (characterIdx >= 0 && characterIdx < static_cast<int>(gCharacters.size())) {
        const Character& ch = gCharacters[static_cast<size_t>(characterIdx)];
        // Use empty userInput/scenario here; we will append userMessage later
        systemPrompt = ch.buildSystemPrompt("", "");
    }

    // Add user message to conversation memory
    addMessage(sessionId, "사용자", userMessage);

    // Get conversation context (for debugging / possible future use)
    string context = getContext(sessionId);

    // Construct final prompt for OpenRouter
    // We'll prepend the system prompt (if any) and conversation context,
    // then the current user turn.
    ostringstream finalPrompt;
    if (!systemPrompt.empty()) {
        finalPrompt << systemPrompt << "\n";
    }
    if (!context.empty()) {
        finalPrompt << context << "\n";
    }
    finalPrompt << "사용자: " << userMessage << "\n";
    // Assistant placeholder – the model will generate after this.
    finalPrompt << "어시스턴트: ";

    // Get response from OpenRouter (DeepSeek-3)
    string aiResponse = getOpenRouterResponse(finalPrompt.str());

    // Fallback if OpenRouter fails or returns empty
    if (aiResponse.empty() || aiResponse.find("오류:") != string::npos) {
        // Simple keyword‑based response as fallback
        string lowerMsg = toLower(userMessage);
        if (lowerMsg.find("안녕") != string::npos ||
            lowerMsg.find("hello") != string::npos) {
            aiResponse = "안녕하세요! 저는 당신의 이야기를 들어주는 친구예요. 오늘 하루는 어떻게 보냈나요?";
        } else if (lowerMsg.find("슬퍼") != string::npos ||
                   lowerMsg.find("우울") != string::npos ||
                   lowerMsg.find(" sad") != string::npos) {
            aiResponse = "마음이 무거우신 것 같아요. 여기서 이야기를 편하게 들어드릴게요. 무엇이 그렇게 힘들었나요?";
        } else if (lowerMsg.find("기뻐") != string::npos ||
                   lowerMsg.find("행복") != string::npos ||
                   lowerMsg.find(" happy") != string::npos) {
            aiResponse = "정말 다행이에요! 당신이 기뻐하는 기분이 여기까지 전해져요. 무슨 좋은 일이 있었나요?";
        } else if (lowerMsg.find("이야기") != string::npos ||
                   lowerMsg.find("story") != string::npos) {
            aiResponse = "이야기 나눌 친구가 생겨서 기뻐요. 어떤 이야기를 듣고 싶으신가요? 아니면 제가 먼저 시작해볼까요?";
        } else {
            aiResponse = "흥미로운 지점이네요. 조금 더 자세히 말씀해 주실 수 있나요?";
        }
    }

    // Add AI response to conversation
    addMessage(sessionId, "어시스턴트", aiResponse);

    // Return response to Java
    return env->NewStringUTF(aiResponse.c_str());
}

} // extern "C"