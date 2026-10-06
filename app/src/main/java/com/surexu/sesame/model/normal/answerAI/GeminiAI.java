package com.surexu.sesame.model.normal.answerAI;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import com.surexu.sesame.util.Log;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * GenAI帮助类
 *
 * @author Xiong
 */
public class GeminiAI implements AnswerAIInterface {
    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .callTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .followRedirects(false)
            .build();
    private final OkHttpClient client;
    private final String token;
    private final boolean googleSearch;

    public GeminiAI(String token) {
        this(token, false);
    }

    public GeminiAI(String token, boolean googleSearch) {
        this(token, CLIENT, googleSearch);
    }

    GeminiAI(String token, OkHttpClient client) {
        this(token, client, false);
    }

    GeminiAI(String token, OkHttpClient client, boolean googleSearch) {
        this.token = token == null ? "" : token.trim();
        this.client = client;
        this.googleSearch = googleSearch;
    }

    /**
     * 获取AI回答结果
     *
     * @param text 问题内容
     * @return AI回答结果
     */
    @Override
    public String getAnswerStr(String text) {
        if (token.isEmpty()) {
            return "";
        }
        try {
            JSONObject content = new JSONObject().put("contents", new JSONArray().put(
                    new JSONObject().put("parts", new JSONArray().put(new JSONObject().put(
                            "text", "直接给出答案文字，严禁解释，不要额外标点。题目：" + text)))));
            if (googleSearch) {
                content.put("tools", new JSONArray().put(new JSONObject().put("google_search", new JSONObject())));
            }
            RequestBody body = RequestBody.create(content.toString(), MediaType.parse("application/json"));
            Request request = new Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent")
                    .post(body)
                    .header("x-goog-api-key", token)
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    Log.other("Gemini请求失败，HTTP " + response.code());
                    return "";
                }
                if (response.body() == null) {
                    return "";
                }
                JSONObject json = new JSONObject(response.body().string());
                return json.getJSONArray("candidates").getJSONObject(0)
                        .getJSONObject("content").getJSONArray("parts")
                        .getJSONObject(0).getString("text").trim();
            }
        } catch (Exception e) {
            // 异常消息或响应正文可能回显令牌，只记录异常类型。
            Log.other("Gemini答题出错：" + e.getClass().getSimpleName());
        }
        return "";
    }

    /**
     * 获取答案
     *
     * @param title     问题
     * @param answerList 答案集合
     * @return 空没有获取到
     */
    @Override
    public Integer getAnswer(String title, List<String> answerList) {
        StringBuilder answerStr = new StringBuilder();
        for (String answer : answerList) {
            answerStr.append("[").append(answer).append("]");
        }
        String answerResult = getAnswerStr(title + "\n" + answerStr);
        if (answerResult != null && !answerResult.isEmpty()) {
            for (int i = 0, size = answerList.size(); i < size; i++) {
                if (answerResult.contains(answerList.get(i))) {
                    return i;
                }
            }
        }
        return -1;
    }
}
