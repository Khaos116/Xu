"""Run: python scripts/check_gemini.py (JDK + existing Gradle dependency cache).

Optional --live reads a Gemini key from stdin and tests the real application class.
No credentials are saved. Offline checks never contact a server.
"""
import os
from pathlib import Path
import subprocess
import sys
import tempfile

ROOT = Path(__file__).resolve().parents[1]
CACHE = Path(os.environ.get("GRADLE_USER_HOME", Path.home() / ".gradle")) / "caches/modules-2/files-2.1"
PACKAGE = "com.surexu.sesame.model.normal.answerAI"
SOURCE = ROOT / "app/src/main/java" / PACKAGE.replace(".", "/")


def jar(group, artifact, version):
    matches = list((CACHE / group / artifact / version).glob(f"*/{artifact}-{version}.jar"))
    if not matches:
        raise SystemExit(f"Missing cached dependency: {artifact}:{version}")
    return str(matches[0])


HARNESS = r'''
package com.surexu.sesame.model.normal.answerAI;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.*;
import okio.Buffer;
import org.json.JSONObject;
import com.surexu.sesame.util.Log;

public class GeminiCheck {
    public static void main(String[] args) throws Exception {
        AtomicInteger mode = new AtomicInteger();
        AtomicInteger calls = new AtomicInteger();
        AtomicReference<String> prompt = new AtomicReference<>();
        AtomicReference<JSONObject> requestJson = new AtomicReference<>();
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(chain -> {
            calls.incrementAndGet();
            Request request = chain.request();
            assert request.url().toString().equals("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent");
            assert "test-secret".equals(request.header("x-goog-api-key"));
            assert request.method().equals("POST");
            Buffer buffer = new Buffer();
            request.body().writeTo(buffer);
            requestJson.set(new JSONObject(buffer.readUtf8()));
            prompt.set(requestJson.get().getJSONArray("contents")
                    .getJSONObject(0).getJSONArray("parts").getJSONObject(0).getString("text"));
            if (mode.get() == 4) throw new java.io.IOException("test-secret");
            String body = switch (mode.get()) {
                case 1 -> "test-secret";
                case 2 -> "invalid JSON test-secret";
                case 3 -> "{}";
                default -> """
                    {"candidates":[{"content":{"parts":[{"text":" 2 "}]}}]}
                    """;
            };
            return new Response.Builder().request(request).protocol(Protocol.HTTP_1_1)
                    .code(mode.get() == 1 ? 403 : 200).message("check")
                    .body(ResponseBody.create(body, MediaType.parse("application/json"))).build();
        }).build();
        GeminiAI ai = new GeminiAI(" test-secret ", client);
        String question = "题目\n\"C:\\tmp\"\t中文";
        assert ai.getAnswerStr(question).equals("2");
        assert !requestJson.get().has("tools");
        assert new GeminiAI("test-secret", client, true).getAnswerStr(question).equals("2");
        assert requestJson.get().getJSONArray("tools").getJSONObject(0).has("google_search");
        assert prompt.get().endsWith(question) : "JSON must preserve special characters";
        assert ai.getAnswer("1+1?", List.of("1", "2")) == 1;
        assert prompt.get().endsWith("1+1?\n[1][2]");
        for (int i = 1; i <= 4; i++) {
            mode.set(i);
            assert ai.getAnswerStr(question).isEmpty();
        }
        int before = calls.get();
        assert new GeminiAI(null, client).getAnswerStr(question).isEmpty();
        assert new GeminiAI("  ", client).getAnswerStr(question).isEmpty();
        assert calls.get() == before;
        assert !Log.messages.contains("test-secret") : "Credentials leaked to logs";
        System.out.println("PASS: request JSON, model/auth, answer parsing, failure paths, empty key, log redaction");
        if (args.length > 0 && args[0].equals("--live")) {
            String key = new Scanner(System.in).nextLine();
            String answer = new GeminiAI(key).getAnswerStr("1+1=?\n[1][2]");
            if (!answer.equals("2")) throw new AssertionError("Live answer check failed; " + Log.messages);
            System.out.println("PASS: live Gemini 2.5 Flash answer = 2");
        }
    }
}
'''

classpath = os.pathsep.join([
    jar("com.squareup.okhttp3", "okhttp", "4.12.0"),
    jar("com.squareup.okio", "okio-jvm", "3.6.0"),
    jar("org.jetbrains.kotlin", "kotlin-stdlib", "2.4.0"),
    jar("org.json", "json", "20240303"),
])
with tempfile.TemporaryDirectory(prefix="surexu-gemini-check-") as directory:
    temp = Path(directory)
    (temp / "GeminiCheck.java").write_text(HARNESS, encoding="utf-8")
    (temp / "Log.java").write_text(
        'package com.surexu.sesame.util; public class Log {'
        'public static String messages = ""; public static void other(String s) { messages += s; }}',
        encoding="utf-8",
    )
    subprocess.run([
        "javac", "-encoding", "UTF-8", "-cp", classpath, "-d", str(temp),
        str(SOURCE / "GeminiAI.java"), str(SOURCE / "AnswerAIInterface.java"),
        str(temp / "Log.java"), str(temp / "GeminiCheck.java"),
    ], check=True)
    subprocess.run([
        "java", "-ea", "-cp", str(temp) + os.pathsep + classpath,
        PACKAGE + ".GeminiCheck", *sys.argv[1:],
    ], check=True)
