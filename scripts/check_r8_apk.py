"""Check actual minified APKs on an emulator: python scripts/check_r8_apk.py.

Requires a running emulator, JDK, SDK build-tools and a completed Release build.
Runs an isolated app_process with the APK's DEX; no hooks, tasks or account loads.
Only temporary probe files and initialization logs are written on the emulator.
"""
import argparse
import json
import os
from pathlib import Path
import re
import subprocess
import tempfile
import zipfile

ROOT = Path(__file__).resolve().parents[1]
CACHE = Path(os.environ.get("GRADLE_USER_HOME", Path.home() / ".gradle")) / "caches/modules-2/files-2.1"

HARNESS = r'''
import java.lang.reflect.*;
import java.util.*;
import org.json.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.surexu.sesame.data.*;
import com.surexu.sesame.util.Status;
import com.surexu.sesame.util.Statistics;
import com.surexu.sesame.hook.RpcRequest;

public class R8Check {
    static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
    public static void main(String[] args) throws Exception {
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            error.printStackTrace(System.out);
            System.exit(1);
        });
        // Resolve only this private implementation field through the generated mapping.
        Field field = Class.forName(args[0]).getDeclaredField(args[1]);
        field.setAccessible(true);
        ObjectMapper mapper = (ObjectMapper) field.get(null);
        Constructor<?> typeConstructor = Class.forName(args[3]).getDeclaredConstructor();
        typeConstructor.setAccessible(true);
        TypeReference<?> reference = (TypeReference<?>) typeConstructor.newInstance();
        ParameterizedType mapType = (ParameterizedType) reference.getType();
        check(mapType.getRawType() == Map.class && mapType.getActualTypeArguments()[1].getTypeName().endsWith("UserEntity$UserDto"), "TypeReference signature");
        Model.initAllModel();
        check(Model.getModelArray().length == Integer.parseInt(args[2]), "model count");
        for (Model model : Model.getModelArray()) check(model != null, "reflective model constructor");
        ConfigV2.INSTANCE.setModelFieldsMap(null);
        ModelField<?> interval = Model.getModelConfigMap().get("FishTask").getModelField("fishCheckInterval");
        interval.setConfigValue("91");
        String config = ConfigV2.INSTANCE.toSaveStr();
        JSONObject models = new JSONObject(config).getJSONObject("modelFieldsMap");
        check(models.length() == Model.getModelArray().length, "stable model keys");
        check(models.getJSONObject("FishTask").getJSONObject("fishCheckInterval").getLong("value") == 91L * 60000, "config units");
        check(!models.getJSONObject("FishTask").getJSONObject("enable").has("defaultValue"), "JsonIgnore metadata");
        interval.setConfigValue("60");
        mapper.readerForUpdating(ConfigV2.INSTANCE).readValue(config);
        check(interval.getConfigValue().equals("91"), "config round trip");
        check(mapper.readTree(ConfigV2.INSTANCE.toSaveStr()).equals(mapper.readTree(config)), "all model fields round trip");
        Model ai = Arrays.stream(Model.getModelArray())
                .filter(model -> model.getClass().getSimpleName().equals("AnswerAI")).findFirst().orElseThrow();
        ModelFields aiFields = ai.getFields();
        check(aiFields.get("useGeminiAI").getConfigValue().equals("0"), "custom AI remains default");
        check(aiFields.get("geminiGoogleSearch").getConfigValue().equals("false"), "search remains opt-in");
        Field provider = ai.getClass().getDeclaredField(args[4]);
        provider.setAccessible(true);
        aiFields.get("customAIUrl").setConfigValue("https://example.invalid/v1");
        aiFields.get("customAIModel").setConfigValue("offline-probe");
        aiFields.get("customAIKey").setConfigValue("offline-probe");
        ai.boot(null);
        check(Class.forName(args[5]).isInstance(provider.get(null)), "custom AI routing");
        aiFields.get("useGeminiAI").setConfigValue("1");
        ai.boot(null);
        check(provider.get(null) == null, "empty Gemini key disables requests");
        aiFields.get("useGeminiAIToken").setConfigValue("offline-probe");
        ai.boot(null);
        check(Class.forName(args[6]).isInstance(provider.get(null)), "Gemini routing");
        // Generic maps, sets and nested beans must retain signatures and property names.
        Status status = new Status();
        status.setFishLastExecTime(123456789L);
        status.getExchangeReserveLogList().put(7, 2);
        status.getFlagLogList().add("probe");
        String statusJson = mapper.writeValueAsString(status);
        Status restored = new Status();
        mapper.readerForUpdating(restored).readValue(statusJson);
        check(restored.getFishLastExecTime() == 123456789L, "status field");
        check(restored.getExchangeReserveLogList().get(7) == 2, "integer map keys");
        check(restored.getFlagLogList().contains("probe"), "set values");
        Statistics stats = new Statistics();
        stats.getDay().setCollected(42);
        Statistics restoredStats = new Statistics();
        mapper.readerForUpdating(restoredStats).readValue(mapper.writeValueAsString(stats));
        check(restoredStats.getDay().getCollected() == 42, "nested statistics");
        TokenConfig tokens = new TokenConfig();
        tokens.getCustomWalkPathIdQueue().add("test-path");
        TokenConfig restoredTokens = new TokenConfig();
        mapper.readerForUpdating(restoredTokens).readValue(mapper.writeValueAsString(tokens));
        check(restoredTokens.getCustomWalkPathIdQueue().peek().equals("test-path"), "queue signature");
        RpcRequest request = new RpcRequest();
        mapper.readerForUpdating(request).readValue("{\"methodName\":\"test\",\"requestData\":[{}]}");
        check(request.getMethodName().equals("test"), "HTTP request bean");
        System.out.println("PASS: minified model constructors, config round trip, AI routing, Jackson generics, state and RPC bean");
        System.exit(0);
    }
}
'''


def run(args, **kwargs):
    result = subprocess.run([str(a) for a in args], capture_output=True, **kwargs)
    if result.returncode:
        raise RuntimeError((result.stdout + result.stderr).decode("utf-8", errors="replace"))
    return result.stdout.decode("utf-8", errors="replace")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--serial", default="emulator-5556")
    parser.add_argument("--flavors", nargs="+", choices=["normal"], default=["normal"])
    args = parser.parse_args()
    assert args.serial.startswith("emulator-"), "Use an emulator, not an account device"
    sdk_line = next(x for x in (ROOT / "local.properties").read_text(encoding="utf-8").splitlines() if x.startswith("sdk.dir="))
    sdk = Path(sdk_line.split("=", 1)[1].replace("\\:", ":").replace("\\\\", "/"))
    adb = sdk / "platform-tools/adb.exe"
    build_tools = max((sdk / "build-tools").iterdir(), key=lambda p: tuple(int(n) for n in p.name.split(".") if n.isdigit()))
    android_jar = next((sdk / "platforms").glob("android-37*/android.jar"))
    model_count = len(re.findall(r"clazzList.add\(\w+\.class\)", (ROOT / "app/src/main/java/com/surexu/sesame/model/base/ModelOrder.java").read_text(encoding="utf-8")))
    with tempfile.TemporaryDirectory(prefix="surexu-r8-") as temp:
        temp = Path(temp)
        source = temp / "R8Check.java"
        source.write_text(HARNESS, encoding="utf-8")
        jars = [next((CACHE / "com.fasterxml.jackson.core" / a / "2.18.2").glob("*/*.jar")) for a in ("jackson-core", "jackson-databind", "jackson-annotations")]
        classes = ROOT / "app/build/intermediates/javac/normalRelease/compileNormalReleaseJavaWithJavac/classes"
        run(["javac", "-encoding", "UTF-8", "-cp", os.pathsep.join(map(str, [classes, android_jar, *jars])), "-d", temp, source])
        probe = temp / "probe.jar"
        run(["java", "-cp", build_tools / "lib/d8.jar", "com.android.tools.r8.D8", "--min-api", "26", "--output", probe, temp / "R8Check.class"])
        remote = "/data/local/tmp/surexu-r8-check"
        run([adb, "-s", args.serial, "shell", "mkdir", "-p", remote])
        try:
            run([adb, "-s", args.serial, "push", probe, remote + "/probe.jar"])
            for flavor in args.flavors:
                output = ROOT / f"app/build/outputs/apk/{flavor}/release"
                meta = json.loads((output / "output-metadata.json").read_text(encoding="utf-8"))
                apk = output / meta["elements"][0]["outputFile"]
                mapping = (ROOT / f"app/build/outputs/mapping/{flavor}Release/mapping.txt").read_text(encoding="utf-8")
                assert apk.stat().st_mtime >= (ROOT / "app/proguard-rules.pro").stat().st_mtime, "Rebuild Release first"
                with zipfile.ZipFile(apk) as z:
                    for entry, entry_file in [("LegacyEntry", "assets/xposed_init"),
                                              ("LibXposedEntry", "META-INF/xposed/java_init.list")]:
                        entry_name = f"com.surexu.sesame.hook.{entry}"
                        assert f"{entry_name} -> {entry_name}:" in mapping, "Xposed entry renamed/removed"
                        assert z.read(entry_file).decode().strip() == entry_name
                block = re.search(r"^com\.surexu\.sesame\.util\.JsonUtil -> ([^:]+):\n(.*?)(?=^[^\s#]|\Z)", mapping, re.M | re.S)
                assert block, "JsonUtil mapping missing"
                mapped_field = re.search(r"ObjectMapper MAPPER -> (\S+)", block[2])[1]
                type_reference = re.search(r"^com\.surexu\.sesame\.util\.idMap\.UserIdMap\$2 -> ([^:]+):", mapping, re.M)[1]
                ai_block = re.search(r"^com\.surexu\.sesame\.model\.normal\.answerAI\.AnswerAI -> [^:]+:\n(.*?)(?=^[^\s#]|\Z)", mapping, re.M | re.S)[1]
                provider_field = re.search(r"AnswerAIInterface customAI -> (\S+)", ai_block)[1]
                providers = [re.search(rf"^com\.surexu\.sesame\.model\.normal\.answerAI\.{name} -> ([^:]+):", mapping, re.M)[1]
                             for name in ("CustomAI", "GeminiAI")]
                run([adb, "-s", args.serial, "push", apk, remote + "/app.apk"])
                result = run([adb, "-s", args.serial, "shell", f"CLASSPATH={remote}/app.apk:{remote}/probe.jar app_process / R8Check {block[1]} {mapped_field} {model_count} {type_reference} {provider_field} {' '.join(providers)}"])
                assert "PASS:" in result, result
                print(f"{flavor}: {apk.stat().st_size / 1048576:.2f} MiB; {result.strip()}")
        finally:
            run([adb, "-s", args.serial, "shell", "rm", "-f", remote + "/probe.jar", remote + "/app.apk"])


if __name__ == "__main__":
    main()
