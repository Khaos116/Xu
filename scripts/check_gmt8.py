"""Run after :app:assembleNormalRelease: python scripts/check_gmt8.py.

Uses the compiled application classes and cached dependencies; no device, RPC,
or user state files are touched. System timezone changes stay in this test JVM.
"""
import os
from pathlib import Path
import re
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "app/src/main/java/com/surexu/sesame"
CLASSES = ROOT / "app/build/intermediates/javac/normalRelease/compileNormalReleaseJavaWithJavac/classes"
CACHE = Path(os.environ.get("GRADLE_USER_HOME", Path.home() / ".gradle")) / "caches/modules-2/files-2.1"


def jar(group, artifact, version):
    return str(next((CACHE / group / artifact / version).glob(f"*/{artifact}-{version}.jar")))


for name in ["Statistics", "Status", "JsonUtil"]:
    compiled = CLASSES / f"com/surexu/sesame/util/{name}.class"
    assert compiled.exists() and compiled.stat().st_mtime >= (SOURCE / f"util/{name}.java").stat().st_mtime, "Build Normal Release first"
for source in SOURCE.rglob("*"):
    if source.suffix in {".java", ".kt"}:
        text = source.read_text(encoding="utf-8")
        assert not re.search(r"Calendar\.getInstance\(\)|TimeZone\.(getDefault|setDefault)\(", text), source
        assert source.name == "TimeUtil.java" or not re.search(r"new (?:java\.text\.)?SimpleDateFormat\(", text), source

HARNESS = r'''
import java.time.Instant;
import java.util.*;
import com.surexu.sesame.util.*;

public class Gmt8Check {
    static long instant(String value) { return Instant.parse(value).toEpochMilli(); }
    static Calendar local(long ms) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ms);
        return c;
    }
    public static void main(String[] args) throws Exception {
        long before = instant("2026-09-11T15:59:59.999Z");
        long midnight = before + 1;
        TimeZone original = TimeZone.getDefault();
        try {
            for (String zone : List.of("GMT+07:00", "UTC", "GMT+08:00", "America/Los_Angeles")) {
                TimeZone.setDefault(TimeZone.getTimeZone(zone));
                Calendar now = TimeUtil.getCalendarByTimeMillis(midnight);
                assert now.getTimeInMillis() == midnight;
                assert now.get(Calendar.DAY_OF_MONTH) == 12 && now.get(Calendar.HOUR_OF_DAY) == 0;
                assert TimeUtil.getNow().getTimeZone().getRawOffset() == 8 * 3600000;
                assert TimeUtil.getToday().get(Calendar.HOUR_OF_DAY) == 0;
                assert TimeUtil.getToday().getTimeZone().getRawOffset() == 8 * 3600000;
                assert TimeUtil.newDateFormat("yyyy-MM-dd HH:mm:ss").format(midnight).equals("2026-09-12 00:00:00");
                assert TimeUtil.getCommonDate(midnight).endsWith("00:00:00");
                assert TimeUtil.getCalendarByTimeStr(midnight, "0700").getTimeInMillis() == instant("2026-09-11T23:00:00Z");
                assert TimeUtil.getCalendarByTimeStr(local(midnight), "20").getTimeInMillis() == instant("2026-09-12T12:00:00Z");
                assert TimeUtil.getCalendarByTimeStr(midnight, "060001").getTimeInMillis() == instant("2026-09-11T22:00:01Z");
                assert !TimeUtil.isSameDay(local(before), local(midnight));
                assert TimeUtil.isLessThanSecondOfDays(local(before), local(midnight));
                Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                utc.setTimeInMillis(midnight);
                assert TimeUtil.isSameDay(utc, now);
                assert utc.getTimeZone().getID().equals("UTC");
                assert TimeUtil.checkInTimeRange(instant("2026-09-11T22:00:00Z"), "0600-1000");
                assert !TimeUtil.checkInTimeRange(instant("2026-09-11T21:59:59Z"), "0600-1000");
                assert TimeUtil.isAfterOrCompareTimeStr(instant("2026-09-12T00:00:00Z"), "0800");
                Calendar alarm = TimeUtil.getCalendarByTimeMillis(before);
                alarm.add(Calendar.DATE, 1);
                assert TimeUtil.getCalendarByTimeStr(alarm, "000000").getTimeInMillis() == midnight;
                long sunday = instant("2026-09-13T15:59:59Z");
                long monday = instant("2026-09-13T16:00:00Z");
                assert TimeUtil.getWeekNumber(new Date(sunday)) != TimeUtil.getWeekNumber(new Date(monday));
                assert TimeUtil.newDateFormat("HH:mm").format(Date.from(Instant.parse("2026-09-13T16:00:00.000Z"))).equals("00:00");
                assert JsonUtil.toJsonString(new Date(midnight)).equals("\"2026-09-12 00:00:00\"");
                Statistics.updateDay(local(before));
                Statistics.addData(Statistics.DataType.COLLECTED, 7);
                assert Statistics.updateDay(local(midnight));
                assert Statistics.getData(Statistics.TimeType.DAY, Statistics.DataType.COLLECTED) == 0;
                assert Statistics.getData(Statistics.TimeType.MONTH, Statistics.DataType.COLLECTED) == 7;
                assert !Statistics.updateDay(local(midnight + 3600000));
                Statistics.updateDay(local(instant("2026-12-31T15:59:59Z")));
                Statistics.addData(Statistics.DataType.COLLECTED, 3);
                assert Statistics.updateDay(local(instant("2026-12-31T16:00:00Z")));
                assert Statistics.getData(Statistics.TimeType.YEAR, Statistics.DataType.TIME) == 2027;
                assert Statistics.getData(Statistics.TimeType.YEAR, Statistics.DataType.COLLECTED) == 0;
                Status.INSTANCE.setSaveTime(before);
                Status.INSTANCE.getFlagLogList().add("gmt8-check");
                assert !Status.updateDay(local(before));
                assert Status.hasFlagToday("gmt8-check");
                assert Status.updateDay(local(midnight));
                assert !Status.hasFlagToday("gmt8-check");
                assert TimeZone.getDefault().getID().equals(zone) : "Host timezone was changed";
                System.out.println("PASS: " + zone + " dates/windows/alarms/weeks/JSON/daily-state/year-rollover");
            }
        } finally {
            TimeZone.setDefault(original);
        }
    }
}
'''

classpath = os.pathsep.join([str(CLASSES), *[
    jar("com.fasterxml.jackson.core", artifact, "2.18.2")
    for artifact in ["jackson-core", "jackson-databind", "jackson-annotations"]
], jar("org.json", "json", "20240303")])
with tempfile.TemporaryDirectory(prefix="surexu-gmt8-") as directory:
    temp = Path(directory)
    (temp / "Gmt8Check.java").write_text(HARNESS, encoding="utf-8")
    (temp / "SuppressLint.java").write_text(
        'package android.annotation; public @interface SuppressLint { String[] value(); }', encoding="utf-8")
    (temp / "Log.java").write_text(
        'package com.surexu.sesame.util; public class Log {'
        'public static void printStackTrace(Throwable t) { throw new AssertionError(t); }'
        'public static void printStackTrace(String tag, Throwable t) { throw new AssertionError(t); }}',
        encoding="utf-8",
    )
    (temp / "MessageUtil.java").write_text(
        'package com.surexu.sesame.util; public class MessageUtil {'
        'public static void sweepExpiredBlackList() {} public static void sweepReleasedDefaults() {}}',
        encoding="utf-8",
    )
    subprocess.run(["javac", "-encoding", "UTF-8", "-cp", classpath, "-d", str(temp),
                    str(temp / "Log.java"), str(temp / "MessageUtil.java"), str(temp / "SuppressLint.java"),
                    str(SOURCE / "util/TimeUtil.java"), str(temp / "Gmt8Check.java")], check=True)
    subprocess.run(["java", "-ea", "-cp", str(temp) + os.pathsep + classpath, "Gmt8Check"], check=True)
