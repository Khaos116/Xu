"""Offline checks for the GR migration. Run after compiling Normal Release.

Compiles the actual task sources; RPC, persistence and the scheduler are replaced
with in-memory boundaries. No Alipay requests, account files or device changes.
"""
import os
from pathlib import Path
import subprocess
import tempfile
import zipfile

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "app/src/main/java/com/surexu/sesame"
CLASSES = ROOT / "app/build/intermediates/javac/normalRelease/compileNormalReleaseJavaWithJavac/classes"
CACHE = Path(os.environ.get("GRADLE_USER_HOME", Path.home() / ".gradle")) / "caches/modules-2/files-2.1"


def jar(group, artifact, version):
    return str(next((CACHE / group / artifact / version).glob(f"*/{artifact}-{version}.jar")))


STUBS = {
    "ModelTask": '''package com.surexu.sesame.data.task;
        import com.surexu.sesame.data.*;
        public abstract class ModelTask {
          public abstract String getName(); public abstract ModelGroup getGroup();
          public abstract ModelFields getFields(); public abstract Boolean check(); public abstract void run();
        }''',
    "ApplicationHook": '''package com.surexu.sesame.hook;
        import java.util.function.BiFunction;
        public class ApplicationHook {
          public static BiFunction<String,String,String> rpc = (m,a) -> "{}";
          public static String requestString(String m,String a) { return rpc.apply(m,a); }
        }''',
    "ConfigV2": '''package com.surexu.sesame.data;
        public class ConfigV2 { public static boolean save(String uid, boolean force) { return true; } }''',
    "UserIdMap": '''package com.surexu.sesame.util.idMap;
        public class UserIdMap { public static String uid = "test-account";
          public static String getCurrentUid() { return uid; } }''',
    "Status": '''package com.surexu.sesame.util;
        import java.util.*;
        public class Status {
          public static final Status INSTANCE = new Status();
          public static final Set<String> flags = new HashSet<>();
          private long last;
          public long getFishLastExecTime() { return last; }
          public void setFishLastExecTime(long t) { last=t; }
          public static Boolean hasFlagToday(String k) { return flags.contains(k); }
          public static void flagToday(String k) { flags.add(k); }
          public static void save() {}
        }''',
    "FileUtil": '''package com.surexu.sesame.util;
        import java.io.File;
        public class FileUtil {
          public static File getAntFishpondTaskListMapFile() { return new File("unused"); }
          public static String readFromFile(File f) { return ""; }
          public static boolean write2File(String s,File f) { return true; }
        }''',
    "Log": '''package com.surexu.sesame.util;
        public class Log {
          public static void other(String s) {} public static void record(String s) {}
          public static void farm(String s) {} public static void i(String s) {}
          public static void i(String a,String s) {}
          public static void err(String a,String s,Throwable t) {}
          public static void printStackTrace(Throwable t) {}
          public static void printStackTrace(String s,Throwable t) {}
        }''',
}

HARNESS = r'''
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.*;
import com.surexu.sesame.data.*;
import com.surexu.sesame.data.modelFieldExt.*;
import com.surexu.sesame.hook.ApplicationHook;
import com.surexu.sesame.model.task.fish.*;
import com.surexu.sesame.model.task.antFarm.*;
import com.surexu.sesame.model.base.TaskCommon;
import com.surexu.sesame.util.Status;
import com.surexu.sesame.util.idMap.UserIdMap;

public class GrSyncCheck {
    static Object call(Object target, String name, Class<?>[] types, Object... args) throws Exception {
        Method method = target.getClass().getDeclaredMethod(name,types);
        method.setAccessible(true); return method.invoke(target,args);
    }
    static JSONObject task(String key, String mode) {
        return new JSONObject().put("bizKey",key).put("taskId",key).put("taskMode",mode);
    }
    public static void main(String[] args) throws Exception {
        assert FarmTaskPolicy.canRun(task("browse", "VIEW"));
        assert FarmTaskPolicy.canRun(task("browse", "COUNT_DOWN"));
        assert FarmTaskPolicy.canRun(task("browse", "TRIGGER"));
        assert !FarmTaskPolicy.canRun(task("browse", "TRIGGER").put("taskId","different"));
        for (String key : List.of("ONLINE_PAY","OFFLINE_PAY","HEART_DONATION","TAO_GOLDEN_V2","xiadan_buy"))
            assert !FarmTaskPolicy.canRun(task(key,"VIEW")) : key;
        assert !FarmTaskPolicy.canRun(task("browse","VIEW").put("desc","付款1元"));
        assert !FarmTaskPolicy.canRun(task("browse","VIEW").put("title","买东西"));
        assert !FarmTaskPolicy.canRun(null);

        FishTask fish = new FishTask(); ModelFields fields = fish.getFields();
        assert !FishConfig.isEnableFishAuto() && !FishConfig.isEnableFishTaskAuto();
        assert FishConfig.getFishCheckInterval() == 3600000;
        assert fields.get("fishCheckInterval").getConfigValue().equals("60");
        fields.get("fishCheckInterval").setConfigValue("90");
        assert FishConfig.getFishCheckInterval() == 5400000;
        assert fields.get("fishCheckInterval").getConfigValue().equals("90");
        fields.get("fishCheckInterval").setConfigValue("60");
        TaskCommon.IS_ENERGY_TIME = true;
        ((BooleanModelField)fields.get("enableFishAuto")).setValue(true);
        assert !fish.check();
        TaskCommon.IS_ENERGY_TIME = false;
        for (String zone : List.of("UTC","GMT+07:00","America/Los_Angeles")) {
            TimeZone.setDefault(TimeZone.getTimeZone(zone));
            String day = (String)call(fish,"getToday",new Class<?>[0]);
            String expected = java.time.Instant.now().atOffset(java.time.ZoneOffset.ofHours(8)).toLocalDate().toString();
            assert day.equals(expected) : zone;
        }
        assert !(Boolean)call(fish,"isSuccess",new Class<?>[]{Object.class},"{\"success\":false,\"resultCode\":100}");
        assert !(Boolean)call(fish,"isSuccess",new Class<?>[]{Object.class},"{\"success\":true,\"error\":1009}");
        assert !(Boolean)call(fish,"isSuccess",new Class<?>[]{Object.class},"bad JSON");
        assert (Boolean)call(fish,"isSuccess",new Class<?>[]{Object.class},"{\"resultCode\":100}");
        String encoded=(String)call(fish,"buildFinishTaskRequest",new Class<?>[]{String.class,String.class,String.class},
            "scene", "task\"quoted", "ad\\id");
        JSONObject encodedRequest=new JSONArray(encoded).getJSONObject(0);
        assert encodedRequest.getString("taskType").equals("task\"quoted");
        assert encodedRequest.getJSONObject("finishBusinessInfo").getString("pwPreBizId").equals("ad\\id");
        ((BooleanModelField)fields.get("autoFishTaskBlacklist")).setValue(false);
        call(fish,"addToBlacklist",new Class<?>[]{String.class},"broken-task");
        assert !FishConfig.getFishTaskBlacklist().contains("broken-task");
        ((BooleanModelField)fields.get("autoFishTaskBlacklist")).setValue(true);
        call(fish,"addToBlacklist",new Class<?>[]{String.class},"broken-task");
        assert FishConfig.getFishTaskBlacklist().contains("broken-task");

        AtomicInteger rpcCount = new AtomicInteger();
        ApplicationHook.rpc = (m,a) -> { rpcCount.incrementAndGet(); return "{\"success\":true}"; };
        Object invalid = call(fish,"performSingleFish",new Class<?>[]{String.class},"not-json");
        assert (Boolean)call(invalid,"isTokenInvalid",new Class<?>[0]);
        assert rpcCount.get()==0;
        ApplicationHook.rpc = (m,a) -> {
            JSONObject req = new JSONArray(a).getJSONObject(0);
            assert req.getJSONObject("riskToken").getString("token").equals("quote\"slash\\");
            return "{\"success\":false,\"resultCode\":\"C21\"}";
        };
        call(fish,"performSingleFish",new Class<?>[]{String.class},new JSONObject().put("token","quote\"slash\\").toString());

        ApplicationHook.rpc = (m,a) -> m.endsWith("fishpondIndex")
            ? "{\"success\":true,\"roundInfo\":{\"canExchange\":true}}" : "{\"success\":true}";
        Status.flags.clear();
        assert (Boolean)call(fish,"checkExchangeReward",new Class<?>[0]);
        assert Status.flags.isEmpty() : "Successful exchange must not block the day";
        ApplicationHook.rpc = (m,a) -> m.endsWith("fishpondIndex")
            ? "{\"success\":true,\"roundInfo\":{\"canExchange\":true}}" : "{\"success\":false}";
        assert !(Boolean)call(fish,"checkExchangeReward",new Class<?>[0]);
        assert !Status.flags.isEmpty();
        rpcCount.set(0);
        ApplicationHook.rpc = (m,a) -> {rpcCount.incrementAndGet();return "{}";};
        Field uidField=FishTask.class.getDeclaredField("runningUid"); uidField.setAccessible(true);
        uidField.set(fish,"previous-account");
        try { call(fish,"requestString",new Class<?>[]{String.class,String.class},"unused","[]"); assert false; }
        catch (InvocationTargetException e) { assert e.getCause() instanceof java.util.concurrent.CancellationException; }
        assert rpcCount.get()==0;
        Thread.currentThread().interrupt();
        try { call(fish,"sleep",new Class<?>[]{long.class},1L); assert false; }
        catch (InvocationTargetException e) { assert e.getCause() instanceof java.util.concurrent.CancellationException; }
        assert Thread.interrupted();

        AtomicInteger purchases=new AtomicInteger();
        ApplicationHook.rpc=(m,a) -> {
            JSONObject req=new JSONArray(a).getJSONObject(0);
            if (m.endsWith("itemList")) {
                assert req.getString("activityId").equals("act\"id");
                return new JSONObject().put("success",true).put("mallAccountInfoVO",new JSONObject()
                    .put("holdingCount",new JSONObject().put("cent",100)))
                    .put("itemInfoVOList",new JSONArray()
                        .put(new JSONObject().put("spuId","missing-price").put("skuModelList",new JSONArray().put(new JSONObject().put("skuId","sku"))))
                        .put(new JSONObject().put("spuId","chair").put("minPrice",new JSONObject().put("cent",80))
                            .put("skuModelList",new JSONArray().put(new JSONObject().put("skuId","sku\"1")))))
                    .put("hasMore",true).put("nextStartIndex",0).toString();
            }
            assert m.endsWith("exchangeBenefit");
            assert req.getString("spuId").equals("chair");
            assert req.getString("skuId").equals("sku\"1");
            assert req.getJSONObject("context").getString("activityId").equals("act\"id");
            purchases.incrementAndGet(); return "{\"success\":true}";
        };
        FamilyDecoration.run(new JSONObject()); assert purchases.get()==0;
        FamilyDecoration.run(new JSONObject().put("decorationCoinActivityId","act\"id"));
        assert purchases.get()==1 : "Do not buy the same item across categories";
        rpcCount.set(0);
        ApplicationHook.rpc=(m,a) -> { rpcCount.incrementAndGet(); return "{\"success\":false,\"resultCode\":\"100\"}"; };
        FamilyDecoration.run(new JSONObject().put("decorationCoinActivityId","act"));
        assert rpcCount.get()==1 : "An explicit failure must stop before exchanging";
        System.out.println("PASS: GR task policy, fish config/timezone/result/token/blacklist/account/cancellation, family exchange");
    }
}
'''

classpath = os.pathsep.join([
    jar("org.json", "json", "20240303"), str(CLASSES),
    *[jar("com.fasterxml.jackson.core", name, "2.18.2") for name in ("jackson-databind", "jackson-core", "jackson-annotations")],
    str(next(Path(os.environ.get("ANDROID_HOME", "E:/SDK")).glob("platforms/android-37*/android.jar"))),
])
with tempfile.TemporaryDirectory(prefix="surexu-gr-check-") as directory:
    temp = Path(directory)
    for name, source in {**STUBS, "GrSyncCheck": HARNESS}.items():
        (temp / f"{name}.java").write_text(source, encoding="utf-8")
    sources = [SOURCE / p for p in (
        "model/task/fish/FishTask.java", "model/task/fish/FishConfig.java",
        "util/idMap/AntFishpondTaskListMap.java", "model/task/antFarm/FarmTaskPolicy.java",
        "model/task/antFarm/FamilyDecoration.java", "model/task/antFarm/AntFarmRpcCall.java",
    )]
    subprocess.run(["javac", "-encoding", "UTF-8", "-cp", classpath, "-d", str(temp),
                    *map(str, sources), *map(str, temp.glob("*.java"))], check=True)
    subprocess.run(["java", "-ea", "-cp", str(temp) + os.pathsep + classpath, "GrSyncCheck"], check=True)

assert not list((ROOT / "app/src").rglob("libsesame.so"))
for p in (ROOT / "app/src").rglob("*"):
    if p.suffix in (".java", ".kt"):
        content = p.read_text(encoding="utf-8")
        assert "System.load(" not in content and "System.loadLibrary(" not in content, p
apk_dir = ROOT / "app/build/outputs/apk/normal/release"
for apk in apk_dir.glob("*.apk"):
    if apk.stat().st_mtime >= (SOURCE / "model/task/fish/FishTask.java").stat().st_mtime:
        with zipfile.ZipFile(apk) as archive:
            assert not any(n.endswith(("/libsesame.so", "/libwatermark.so")) for n in archive.namelist())
        print("PASS: APK excludes sesame and watermark native libraries")
