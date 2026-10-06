package com.surexu.sesame.model.task.fish;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

import com.surexu.sesame.data.ModelFields;
import com.surexu.sesame.data.ModelGroup;
import com.surexu.sesame.data.task.ModelTask;
import com.surexu.sesame.model.base.TaskCommon;
import com.surexu.sesame.entity.IdAndName;
import com.surexu.sesame.hook.ApplicationHook;
import com.surexu.sesame.util.*;
import com.surexu.sesame.util.idMap.AntFishpondTaskListMap;
import com.surexu.sesame.util.idMap.UserIdMap;

/**
 * 福气鱼塘任务
 */
public class FishTask extends ModelTask {
    private static final String API_BATCH_INVITE = "com.alipay.antiep.batchInviteP2P";
    private static final String API_EXCHANGE_REWARD = "com.alipay.antfishpond.fishpondExchangeReward";
    private static final String API_FINISH_TASK = "com.alipay.antiep.finishTask";
    private static final String API_FISHPOND_AD_NOTICE = "com.alipay.antfishpond.fishpondAdNotice";
    private static final String API_FISHPOND_ANGLE = "com.alipay.antfishpond.fishpondAngle";
    private static final String API_FISHPOND_SYNC_INDEX = "com.alipay.antfishpond.fishpondSyncIndex";
    private static final String API_FISHPOND_INDEX = "com.alipay.antfishpond.fishpondIndex";
    private static final String API_LIST_TASK = "com.alipay.antfishpond.listTask";
    private static final String API_QUERY_SUBPLOTS = "com.alipay.antfishpond.querySubplotsActivity";
    private static final String API_RECEIVE_AWARD = "com.alipay.antiep.receiveTaskAward";
    private static final String API_REFINED_OPERATION = "com.alipay.antfishpond.refinedOperation";
    private static final String API_ROD_POSITIONING = "com.alipay.antfishpond.fishpondAngleRodPositioning";
    private static final String API_SIGN = "com.alipay.antfishpond.sign";
    private static final String API_START_APP = "com.alipay.mobile.base.startApp";
    private static final String API_TRIGGER_SUBPLOTS = "com.alipay.antfishpond.triggerSubplotsActivity";
    // 新增广告任务完成接口
    private static final String SCENE_AD_RESULT = "ANTFISHPOND_ACTIVITY_RESULT_AD";
    private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return TimeUtil.newDateFormat("yyyy-MM-dd");
        }
    };
    private static final boolean DEBUG = false;
    private static final int DELAY_BABA_FARM_BASE = 2000;
    private static final int DELAY_BABA_FARM_FLOAT = 1000;
    private static final int DELAY_FISH = 5000;
    private static final int DELAY_FLOAT = 3000;
    private static final int DELAY_GAME_BASE = 30000;
    private static final int DELAY_LONG = 2000;
    private static final int DELAY_LOOK_BASE = 15000;
    private static final int DELAY_MEDIUM = 1000;
    private static final int DELAY_SHORT = 500;
    private static final int REEL_IN_BASE = 2800;
    private static final int REEL_IN_FLOAT = 600;
    private static final String REQUEST_TYPE_NORMAL = "NORMAL";
    private static final String REQUEST_TYPE_RPC = "RPC";
    private static final String SCENE_AD_TASK = "ANTFISHPOND_ACTIVITY_RESULT_AD";
    private static final String SCENE_FISH_TASK = "ANTFISHPOND_TASK";
    private static final String SCENE_GAME_CENTER = "GameCenter";
    private static final String SIGN_TODAY_FLAG = "fish_sign_today";
    private static final String SOURCE_AD_BASIC_LIB = "ADBASICLIB";
    private static final String SOURCE_FARM_POOL = "farmpool";
    private static final String SOURCE_RECENTLY_USED = "ch_appcollect__chsub_my-recentlyUsed";
    private static final String TASK_STATUS_FINISHED = "FINISHED";
    private static final String TASK_STATUS_RECEIVED = "RECEIVED";
    private static final String TASK_STATUS_TODO = "TODO";
    private static final String TASK_TYPE_GOFISH = "GOFISH";
    private static final String TASK_TYPE_OFFLINE_SHARE = "OFFLINE_SHARE";
    private static final String TASK_TYPE_EXCH_MANURE_4_ROD = "EXCH_MANURE_4_ROD";
    private static final String TASK_TYPE_SHARE = "SHARE";
    private static final Map<String, Integer> TASK_WAIT_TIME;
    private static final String VERSION = "20260211.01";
    private static final Set<String> autoTaskIds;
    private static final Map<String, String> displayNameCache;
    private static volatile int fishTaskCount;
    private static volatile String fishTaskData;
    private static volatile double lastFishWeight;
    private static volatile int lastRodCount;
    private static final Set<String> processedTasks;
    private static final Set<String> failedTasks;  // 记录本次运行中领取奖励失败的任务
    private boolean todayRewardEnd = false;
    private boolean firstQueryDone = false;
    private boolean tomorrowRodTriggered = false;  // 防止明日钓竿重复触发
    private boolean queryingStatus;
    private String runningUid;

    static {
        CopyOnWriteArraySet copyOnWriteArraySet = new CopyOnWriteArraySet();
        autoTaskIds = copyOnWriteArraySet;
        displayNameCache = new ConcurrentHashMap();
        ConcurrentHashMap concurrentHashMap = new ConcurrentHashMap();
        TASK_WAIT_TIME = concurrentHashMap;
        fishTaskCount = 0;
        fishTaskData = "";
        lastRodCount = -1;
        lastFishWeight = 0.0d;
        processedTasks = new HashSet();
        failedTasks = new HashSet();  // 初始化失败任务集合
        Collections.addAll(copyOnWriteArraySet, "GYG_XLIGHT_JX_BUSINEES_3", "NORMAL_TAOBAO_1_ROD", "FISH_TASK_14", "ANTFISHPOND_WECHAT_SHARE", "FISHPOND_NCLY_GAME_WPJZ_30S", "FISHPOND_NCLY_GAME_NCDDP_PLAY1", "FISHPOND_NCLY_GAME_XDDQ_30S", "FISHPOND_NCLY_GAME_MSQYJ_PLAY", "EXC_MANURE_4_ROD", "FISH_ACTIVITY_RESULT_AD");
        Integer numValueOf = Integer.valueOf(DELAY_LOOK_BASE);
        concurrentHashMap.put("GYG_XLIGHT_JX_BUSINEES_3", numValueOf);
        concurrentHashMap.put("GYG_XLIGHT_JX_BUSINEES", numValueOf);
        concurrentHashMap.put("NORMAL_TAOBAO_1_ROD", numValueOf);
        concurrentHashMap.put("FISH_ACTIVITY_RESULT_AD", numValueOf);
        AntFishpondTaskListMap.load();
        for (Map.Entry<String, String> entry : AntFishpondTaskListMap.getMap().entrySet()) {
            autoTaskIds.add(entry.getKey());
            displayNameCache.put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public String getName() {
        return "鱼塘";
    }

    @Override
    public ModelGroup getGroup() {
        return ModelGroup.OTHER;
    }

    @Override
    public ModelFields getFields() {
        ModelFields modelFields = new ModelFields();
        FishConfig.registerFields(modelFields);
        return modelFields;
    }

    @Override
    public Boolean check() {
        return !TaskCommon.IS_ENERGY_TIME && isAllowedTime()
                && (FishConfig.isEnableFishAuto() || FishConfig.isEnableFishTaskAuto());
    }

    @Override
    public void run() {
        String taskUid = UserIdMap.getCurrentUid();
        if (taskUid == null || taskUid.isEmpty() || !check()) return;
        runningUid = taskUid;
        // 检查运行时间段：早上5点后到晚上23点前
        if (!isAllowedTime()) {
            Log.other("鱼塘⏰不在运行时段（05:00-23:00）");
            return;
        }

        // 独立执行间隔检查（鱼塘使用自己的间隔，不跟随全局间隔）
        long now = System.currentTimeMillis();
        long fishInterval = FishConfig.getFishCheckInterval();
        long lastExecTime = Status.INSTANCE.getFishLastExecTime();
        if (lastExecTime > 0 && now - lastExecTime < fishInterval) {
            long remainingMinutes = (fishInterval - (now - lastExecTime)) / 60_000;
            Log.other("鱼塘⏰独立间隔未到，还需等待约" + remainingMinutes + "分钟");
            return;
        }

        String validToken = getValidToken();
        processedTasks.clear();
        failedTasks.clear();  // 清空失败任务记录
        this.todayRewardEnd = false;
        this.firstQueryDone = false;
        this.tomorrowRodTriggered = false;  // 重置明日钓竿触发标记
        lastRodCount = -1;
        lastFishWeight = 0;
        try {
            if (Status.hasFlagToday("fish_exchange_fail_" + getToday()).booleanValue()) {
                Log.other("鱼塘⚠️今日兑换失败，已暂停所有任务");
                return;
            }

            // 模拟进入鱼塘（与真实APP流程一致）
            enterFishpond();

            // 执行任务模块：做任务获取钓竿
            if (FishConfig.isEnableFishTaskAuto()) {
                if (!checkExchangeReward()) {
                    return;
                }
                Log.other("鱼塘🎣开始做任务领钓竿");
                triggerSubplotsActivity(true);
                executeTasks();
            }

            // 执行钓鱼功能
            if (FishConfig.isEnableFishAuto()) {
                // ⭐ 先检查并领取钓竿兑换奖励
                if (!checkExchangeReward()) {
                    Log.other("鱼塘⚠️钓竿兑换失败，跳过钓鱼");
                    return;
                }

                startFishing(validToken);
            }
        } catch (Throwable th) {
            Log.printStackTrace("鱼塘🪝执行异常", th);
        } finally {
            // 无论成功还是失败，都记录本次执行时间，避免短时间内重复执行
            if (taskUid.equals(UserIdMap.getCurrentUid())) {
                Status.INSTANCE.setFishLastExecTime(System.currentTimeMillis());
                Status.save();
            }
        }
    }

    private String requestString(String method, String args) {
        if (Thread.currentThread().isInterrupted()
                || (runningUid != null && !runningUid.equals(UserIdMap.getCurrentUid()))) {
            throw new CancellationException("Fish task stopped or account changed");
        }
        return ApplicationHook.requestString(method, args);
    }

    private String getValidToken() {
        return FishConfig.getFishpondToken();
    }

    public static void onTaskDiscovered(String str) {
        if (str == null || str.isEmpty()) {
            return;
        }
        if (autoTaskIds.add(str)) {
            Log.other("鱼塘✅发现新任务[" + str + "]");
        }
        AntFishpondTaskListMap.add(str, str);
        AntFishpondTaskListMap.save();
    }

    public static Set<String> getAllDiscoveredTasks() {
        return autoTaskIds;
    }

    public static class FishTaskItem extends IdAndName {
        public FishTaskItem(String str, String str2) {
            this.id = str;
            this.name = str2;
        }
    }

    public static List<FishTaskItem> getTaskList() {
        final ArrayList arrayList = new ArrayList();
        for (Map.Entry<String, String> entry : AntFishpondTaskListMap.getMap().entrySet()) {
            arrayList.add(new FishTaskItem(entry.getKey(), entry.getValue()));
        }
        return arrayList;
    }

    private static String getTaskDisplayName(String str) {
        String result = displayNameCache.get(str);
        if (result == null) {
            result = lambda$getTaskDisplayName$3(str);
            displayNameCache.put(str, result);
        }
        return result;
    }

    static /* synthetic */ String lambda$getTaskDisplayName$3(String str) {
        String str2 = AntFishpondTaskListMap.get(str);
        return str2 != null ? str2 : str;
    }

    private String getToday() {
        return DATE_FORMAT.get().format(new Date());
    }

    private boolean isSuccess(Object obj) {
        try {
            JSONObject jSONObject = obj instanceof String ? new JSONObject((String) obj) : (JSONObject) obj;
            if (jSONObject == null) {
                return false;
            }
            if (jSONObject.optInt("error", 0) != 0) return false;
            if (jSONObject.has("success")) return jSONObject.optBoolean("success", false);
            if (!jSONObject.optBoolean("success", false)) {
                if (jSONObject.optInt("resultCode", -1) != 100) {
                    return false;
                }
            }
            return true;
        } catch (Exception unused) {
            return false;
        }
    }

    private void sleep(long j) {
        try {
            if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
            Thread.sleep(j);
        } catch (InterruptedException unused) {
            Thread.currentThread().interrupt();
            throw new CancellationException("Fish task interrupted");
        }
    }

    private String buildBaseRequest(String str) {
        return String.format("[{\"appMode\":\"normal\",\"requestType\":\"%s\",\"sceneCode\":\"%s\",\"source\":\"%s\",\"version\":\"%s\"}]", REQUEST_TYPE_NORMAL, str, SOURCE_RECENTLY_USED, VERSION);
    }

    private String buildRequestWithSyncType(String str) {
        return String.format("[{\"appMode\":\"normal\",\"requestType\":\"%s\",\"sceneCode\":\"%s\",\"source\":\"%s\",\"version\":\"%s\",\"syncTypeList\":[\"FISH_ACTIVITY\",\"TASK_DISPLAY\",\"TOMORROW_ROD\",\"LOTTERY_PLUS\"]}]", REQUEST_TYPE_NORMAL, str, SOURCE_RECENTLY_USED, VERSION);
    }

    private String buildTaskRequestWithBizNo(String str, String str2) {
        return String.format("[{\"requestType\":\"%s\",\"sceneCode\":\"%s\",\"source\":\"%s\",\"version\":\"%s\",\"taskType\":\"%s\",\"outBizNo\":\"%s%d\"}]", REQUEST_TYPE_NORMAL, str, SOURCE_RECENTLY_USED, VERSION, str2, UserIdMap.getCurrentUid(), Long.valueOf(System.currentTimeMillis()));
    }

    /**
     * 构建 finishTask 请求
     * 基于真实客户端抓包数据（手动操作时捕获）：
     * {
     *   "finishBusinessInfo": {"pwPreBizId": "adBizNo"},
     *   "outBizNo": "taskType_timestamp_uid8位尾",
     *   "requestType": "RPC",
     *   "sceneCode": "ANTFISHPOND_TASK" 或 "ANTFISHPOND_ACTIVITY_RESULT_AD",
     *   "source": "ADBASICLIB",
     *   "taskType": "GYG_XLIGHT_JX_BUSINEES_3"
     * }
     * 注意：真实请求没有 version 字段！
     */
    private String buildFinishTaskRequest(String sceneCode, String taskId, String adBizNo) throws org.json.JSONException {
        String uid = UserIdMap.getCurrentUid();
        if (uid == null) throw new CancellationException("No current account");
        String outBizNo = taskId + "_" + System.currentTimeMillis() + "_" + uid.substring(Math.max(0, uid.length() - 8));
        JSONObject request = new JSONObject().put("outBizNo", outBizNo)
                .put("requestType", REQUEST_TYPE_RPC).put("sceneCode", sceneCode)
                .put("source", SOURCE_AD_BASIC_LIB).put("taskType", taskId);
        if (adBizNo != null && !adBizNo.isEmpty()) {
            request.put("finishBusinessInfo", new JSONObject().put("pwPreBizId", adBizNo));
        }
        return new JSONArray().put(request).toString();
    }

    private String buildReceiveAwardRequest(String str, String str2) {
        return String.format("[{\"requestType\":\"%s\",\"sceneCode\":\"%s\",\"source\":\"%s\",\"version\":\"%s\",\"taskType\":\"%s\",\"ignoreLimit\":false}]", REQUEST_TYPE_NORMAL, str, SOURCE_RECENTLY_USED, VERSION, str2);
    }

    private void executeTasks() {
        doSign();
        sleep(ThreadLocalRandom.current().nextInt(1000) + 1000);

        // 只执行一次，不循环
        Log.record("鱼塘🎣开始执行任务");

        // 1. 先检查并领取 GIFT_BOX 奖励
        checkAndReceiveGiftBox();
        sleep(DELAY_BABA_FARM_BASE + ThreadLocalRandom.current().nextInt(DELAY_BABA_FARM_FLOAT));

        // 2. 执行常规任务列表
        listTask();

        Log.record("鱼塘✅任务执行完成");
        sleep(ThreadLocalRandom.current().nextInt(1000) + 2000);
    }

    /**
     * 检查当前时间是否在允许的运行时段内
     * @return true=允许运行（05:00-23:00），false=不允许运行
     */
    private boolean isAllowedTime() {
        Calendar calendar = TimeUtil.getNow();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        // 早上5点后且晚上23点前
        return hour >= 5 && hour < 23;
    }

    private void doSign() {
        String str = "fish_sign_today_" + getToday();
        if (Status.hasFlagToday(str).booleanValue()) {
            return;
        }
        try {
            String strRequestString = requestString(API_SIGN, String.format("[{\"appMode\":\"normal\",\"requestType\":\"%s\",\"sceneCode\":\"%s\",\"signKey\":\"%s\",\"source\":\"%s\",\"version\":\"%s\"}]", REQUEST_TYPE_NORMAL, SCENE_GAME_CENTER, getToday(), SOURCE_RECENTLY_USED, VERSION));
            if (strRequestString == null) {
                return;
            }
            JSONObject jSONObject = new JSONObject(strRequestString);
            if (isSuccess(jSONObject)) {
                JSONObject jSONObjectOptJSONObject = jSONObject.optJSONObject("signTaskInfo");
                if (jSONObjectOptJSONObject != null && jSONObjectOptJSONObject.optBoolean("signed", false)) {
                    Log.other("鱼塘✅签到成功");
                    Status.flagToday(str);
                }
            } else {
                String strOptString = jSONObject.optString("resultCode");
                if ("SIGNED".equals(strOptString) || "ALREADY_SIGN".equals(strOptString)) {
                    Status.flagToday(str);
                } else if (!"C20".equals(strOptString)) {
                    Log.other("鱼塘❌签到失败[" + strOptString + "]");
                }
            }
        } catch (Throwable unused) {
        }
    }

    private boolean hasPendingTask() {
        JSONArray jSONArrayOptJSONArray;
        try {
            String strRequestString = requestString(API_LIST_TASK, buildBaseRequest(SCENE_GAME_CENTER));
            if (strRequestString == null || (jSONArrayOptJSONArray = new JSONObject(strRequestString).optJSONArray("taskList")) == null) {
                return false;
            }
            Set<String> fishTaskBlacklist = FishConfig.getFishTaskBlacklist();
            String today = getToday();
            for (int i = 0; i < jSONArrayOptJSONArray.length(); i++) {
                JSONObject jSONObject = jSONArrayOptJSONArray.getJSONObject(i);
                String strOptString = jSONObject.optString("taskId", "");
                String strOptString2 = jSONObject.optString("taskStatus", "");
                String strOptString3 = jSONObject.optString("actionType", "");
                if ((fishTaskBlacklist == null || !fishTaskBlacklist.contains(strOptString)) && !TASK_STATUS_RECEIVED.equals(strOptString2) && !TASK_TYPE_GOFISH.equals(strOptString3)) {
                    if (TASK_STATUS_FINISHED.equals(strOptString2)) {
                        return true;
                    }
                    if (TASK_STATUS_TODO.equals(strOptString2)) {
                        if (!TASK_TYPE_OFFLINE_SHARE.equals(strOptString3)) {
                            return true;
                        }
                        return !Status.hasFlagToday("offline_share_" + strOptString + "_" + today).booleanValue();
                    }
                }
            }
        } catch (Throwable unused) {
        }
        return false;
    }

    private void listTask() {
        JSONArray jSONArrayOptJSONArray;
        try {
            String strRequestString = requestString(API_LIST_TASK, buildBaseRequest(SCENE_GAME_CENTER));
            if (strRequestString == null || (jSONArrayOptJSONArray = new JSONObject(strRequestString).optJSONArray("taskList")) == null) {
                return;
            }
            int iSaveTaskIfNeeded = 0;
            for (int i = 0; i < jSONArrayOptJSONArray.length(); i++) {
                JSONObject jSONObject = jSONArrayOptJSONArray.getJSONObject(i);
                iSaveTaskIfNeeded += saveTaskIfNeeded(jSONObject);

                // ⭐ 借鉴蚂蚁庄园的设计模式
                String taskStatus = jSONObject.optString("taskStatus", "");
                String taskId = jSONObject.optString("taskId", "");
                String actionType = jSONObject.optString("actionType", "");
                String displayName = getTaskDisplayName(taskId);

                // 1. 黑名单任务跳过
                Set<String> fishTaskBlacklist = FishConfig.getFishTaskBlacklist();
                if (fishTaskBlacklist != null && fishTaskBlacklist.contains(taskId)) {
                    Log.record("鱼塘⚠️跳过黑名单任务[" + displayName + "]");
                    // 如果已完成但未领取，尝试领取
                    if (TASK_STATUS_FINISHED.equals(taskStatus)) {
                        receiveTaskAward(taskId, taskId);
                    }
                    continue;
                }

                // 2. 状态检查
                if (TASK_STATUS_RECEIVED.equals(taskStatus)) {
                    continue;  // 已领取，跳过
                }

                // GOFISH类型：TODO时跳过（累计型，不需要主动完成），但FINISHED时领取奖励
                if (TASK_TYPE_GOFISH.equals(actionType)) {
                    if (TASK_STATUS_TODO.equals(taskStatus)) {
                        continue;  // GOFISH TODO，累计型任务，跳过
                    }
                    // FINISHED状态继续往下走，领取奖励
                }

                // 3. TODO 状态 → 执行任务
                if (TASK_STATUS_TODO.equals(taskStatus)) {
                    if (!doTaskActionAndFinish(jSONObject)) {
                        continue;  // 执行失败，跳过
                    }
                }

                // 4. FINISHED 状态 → 领取奖励
                if (TASK_STATUS_FINISHED.equals(taskStatus)) {
                    Log.other("鱼塘🎁领取奖励[" + getTaskDisplayName(taskId) + "]");
                    if (!receiveTaskAward(taskId, taskId)) {
                        // ⭐ 领取失败，加入黑名单
                        Log.other("鱼塘❌领取失败[" + getTaskDisplayName(taskId) + "]");
                        addToBlacklist(taskId);
                    } else {
                        Log.other("鱼塘✅领取成功[" + getTaskDisplayName(taskId) + "]");
                    }
                }

                // ⭐ 增加任务间间隔，避免被限流（3-5秒随机）
                int taskInterval = DELAY_BABA_FARM_BASE + ThreadLocalRandom.current().nextInt(DELAY_BABA_FARM_FLOAT);
                Log.record("鱼塘⏳等待" + taskInterval + "ms");
                sleep(taskInterval);
            }
            if (iSaveTaskIfNeeded > 0) {
                AntFishpondTaskListMap.save();
            }
        } catch (Throwable th) {
            Log.other("鱼塘❌获取任务列表失败[" + th.getMessage() + "]");
        }
    }

    /**
     * 执行任务并标记完成（借鉴蚂蚁庄园 doFarmTask）
     * @return true=成功，false=失败
     */
    private boolean doTaskActionAndFinish(JSONObject taskJson) {
        try {
            String taskId = taskJson.optString("taskId", "");
            String actionType = taskJson.optString("actionType", "");
            String displayName = getTaskDisplayName(taskId);

            Log.other("鱼塘🚀开始任务[" + displayName + "]");

            // 执行任务操作（浏览广告类/游戏类任务内部已调用finishTaskById+等待）
            boolean actionSuccess = doTaskAction(taskId, actionType);

            // ⭐ doTaskAction返回false表示任务无法处理，加入黑名单
            if (!actionSuccess) {
                failedTasks.add(taskId);
                addToBlacklist(taskId);
                return false;
            }

            // 只有非浏览广告类/游戏类任务才需要额外等待和finishTask
            // handleBrowseAdTask/handleGameTask/handleExchangeTask 内部已处理等待和finishTask
            // 这些特殊任务在finishTaskByIdWithResult中已经一并发放了奖励，无需再调用receiveTaskAward
            if (isSpecialHandledTask(taskId)) {
                Log.other("鱼塘✅完成任务[" + displayName + "]");
                return true;
            }

            if (!isBrowseAdTask(taskId)) {
                // 根据任务类型等待足够时间
                Integer waitTime = TASK_WAIT_TIME.get(taskId);
                if (waitTime != null) {
                    Log.other("鱼塘⏳等待" + waitTime + "ms");
                    sleep(waitTime.longValue());
                    sleep(ThreadLocalRandom.current().nextInt(DELAY_FLOAT));
                } else {
                    sleep(DELAY_MEDIUM);
                }

                // ⭐ 标记任务完成
                finishTaskById(taskId);
                sleep(DELAY_SHORT);
            }

            // ⭐ 领取任务奖励（TODO完成后必须领取）
            if (!receiveTaskAward(taskId, taskId)) {
                Log.other("鱼塘❌领取失败[" + displayName + "]");
                addToBlacklist(taskId);
                return false;
            }

            Log.other("鱼塘✅完成任务[" + displayName + "]");
            return true;

        } catch (Exception e) {
            Log.record("鱼塘❌执行任务异常[" + e.getMessage() + "]");
            return false;
        }
    }

    /**
     * 检查是否为内部已处理finishTask的特殊任务类型
     * （handleBrowseAdTask、handleGameTask 内部已调用 finishTaskById）
     */
    private boolean isSpecialHandledTask(String taskId) {
        return "NORMAL_TAOBAO_1_ROD".equals(taskId) ||
               "GYG_XLIGHT_JX_BUSINEES_3".equals(taskId) ||
               "GYG_XLIGHT_JX_BUSINEES".equals(taskId) ||
               "FISH_ACTIVITY_RESULT_AD".equals(taskId) ||
               containsWaitTime(taskId) ||
               isBrowseAdTask(taskId) ||
               isExchangeTask(taskId);
    }
    private int saveTaskIfNeeded(JSONObject jSONObject) {
        String strOptString = jSONObject.optString("taskId", "");
        if (strOptString.isEmpty()) {
            return 0;
        }
        JSONObject jSONObjectOptJSONObject = jSONObject.optJSONObject("taskDisplayConfig");
        AntFishpondTaskListMap.add(strOptString, jSONObjectOptJSONObject != null ? jSONObjectOptJSONObject.optString("title", strOptString) : strOptString);
        return 1;
    }

    private void finishTask(JSONObject r36) {
        try {
            String taskId = r36.optString("taskId", "");
            String taskStatus = r36.optString("taskStatus", "");
            String actionType = r36.optString("actionType", "");
            String today = getToday();

            if (taskId.isEmpty()) {
                return;
            }

            if (TASK_STATUS_RECEIVED.equals(taskStatus)) {
                return;
            }

            if (TASK_TYPE_GOFISH.equals(actionType)) {
                return;
            }

            Set<String> fishTaskBlacklist = FishConfig.getFishTaskBlacklist();
            if (fishTaskBlacklist != null && fishTaskBlacklist.contains(taskId)) {
                return;
            }

            // 如果该任务在本次运行中已经失败过，跳过
            if (failedTasks.contains(taskId)) {
                Log.record("鱼塘⚠️跳过失败任务[" + getTaskDisplayName(taskId) + "]");
                return;
            }

            if (TASK_STATUS_FINISHED.equals(taskStatus)) {
                Log.other("鱼塘🎁领取奖励[" + getTaskDisplayName(taskId) + "]");
                if (!receiveTaskAward(taskId, taskId)) {
                    failedTasks.add(taskId);  // 记录失败任务
                    addToBlacklist(taskId);  // ⭐ 加入黑名单
                }
                return;
            }

            if (TASK_STATUS_TODO.equals(taskStatus)) {
                if (TASK_TYPE_OFFLINE_SHARE.equals(actionType)) {
                    if (Status.hasFlagToday("offline_share_" + taskId + "_" + today).booleanValue()) {
                        return;
                    }
                }

                Log.other("鱼塘🚀开始任务[" + getTaskDisplayName(taskId) + "]");
                doTaskAction(taskId, actionType);

                Integer waitTime = TASK_WAIT_TIME.get(taskId);
                if (waitTime != null) {
                    Log.other("鱼塘⏳等待" + waitTime + "ms");
                    sleep(waitTime.longValue());
                    sleep(ThreadLocalRandom.current().nextInt(DELAY_FLOAT));
                } else {
                    sleep(DELAY_MEDIUM);
                }

                // ⭐ 标记任务完成
                finishTaskById(taskId);
                sleep(DELAY_SHORT);

                if (!receiveTaskAward(taskId, taskId)) {
                    failedTasks.add(taskId);  // 记录失败任务
                    addToBlacklist(taskId);  // ⭐ 加入黑名单
                }
            }

        } catch (Exception e) {
            Log.record("鱼塘❌执行任务异常[" + e.getMessage() + "]");
        }
    }

    private boolean doTaskAction(String taskId, String actionType) {
        try {
            if ("FISH_ACTIVITY_RESULT_AD".equals(taskId)) {
                Log.other("鱼塘📺浏览[钓鱼活动广告]");
                handleFishActivityResultAd(taskId);
                return true;
            } else if ("NORMAL_TAOBAO_1_ROD".equals(taskId) ||
                "GYG_XLIGHT_JX_BUSINEES_3".equals(taskId) ||
                "GYG_XLIGHT_JX_BUSINEES".equals(taskId)) {
                Log.other("鱼塘📺浏览[" + getTaskDisplayName(taskId) + "]");
                return handleBrowseAdTask(taskId);
            } else if (taskId.startsWith("FISHPOND_NCLY_GAME")) {
                // FISHPOND_NCLY_GAME 开头的任务分两种：
                // 1. 名称带\d+s（如"玩英雄没有闪30s"）→ 挂机等待即可完成 → 走 handleGameTask
                // 2. 名称不带\d+s（如"对对碰乐园消除5组"）→ 需要实际玩游戏 → 直接加黑名单
                if (containsWaitTime(taskId)) {
                    Log.other("鱼塘🎮游戏[" + getTaskDisplayName(taskId) + "]");
                    return handleGameTask(taskId);
                } else {
                    Log.other("鱼塘🎮游戏[" + getTaskDisplayName(taskId) + "]#需实际游玩，加入黑名单");
                    addToBlacklist(taskId);
                    return false;
                }
            } else if (containsWaitTime(taskId)) {
                // 处理需要等待的任务（如"玩寻道大千30s"等所有带30s的任务）
                // 统一按 handleGameTask 的完整模式处理：等待 → finishTaskByIdWithResult → 等待同步
                Log.other("鱼塘🎮游戏[" + getTaskDisplayName(taskId) + "]");
                return handleGameTask(taskId);
            } else if (isExchangeTask(taskId)) {
                // 处理兑换类任务（如"消耗肥料兑换钓竿"）
                Log.other("鱼塘💰兑换[" + getTaskDisplayName(taskId) + "]");
                return handleExchangeTask(taskId);
            } else if (isBrowseAdTask(taskId)) {
                // 处理浏览广告任务（如"看精选商品得钓竿"）
                Log.other("鱼塘📺浏览[" + getTaskDisplayName(taskId) + "]");
                return handleBrowseAdTask(taskId);
            }
            // 未知任务类型，直接加入黑名单避免浪费时间
            Log.other("鱼塘❌未知任务[" + getTaskDisplayName(taskId) + "]#无法处理，加入黑名单");
            addToBlacklist(taskId);
            return false;
        } catch (Exception e) {
            Log.record("鱼塘⚠️执行异常[" + e.getMessage() + "]");
            return false;
        }
    }

    /**
     * 检查任务名称是否包含等待时间（如30s、60s等）
     */
    private boolean containsWaitTime(String taskId) {
        String displayName = getTaskDisplayName(taskId);
        return displayName != null && displayName.matches(".*\\d+s.*");
    }

    /**
     * 从任务名称中提取等待时间（秒）
     * 例如："玩寻道大千30s" -> 30
     */
    private int extractWaitTime(String taskId) {
        String displayName = getTaskDisplayName(taskId);
        if (displayName == null) {
            return 30; // 默认30秒
        }
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)s");
        java.util.regex.Matcher matcher = pattern.matcher(displayName);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                return 30;
            }
        }
        return 30; // 默认30秒
    }

    /**
     * 处理钓鱼活动结果页广告
     * 根据抓包数据，该任务需要：
     * 1. 浏览广告
     * 2. 调用fishpondAdNotice通知
     * 3. 等待15秒
     * 4. 调用finishTask完成（含 adBizNo）
     * 5. 自动获得钓竿奖励
     */
    private void handleFishActivityResultAd(String taskId) {
        try {
            Log.other("鱼塘📺浏览[钓鱼活动广告]");

            // 获取 adBizNo
            String adBizNo = getAdBizNoForTask(taskId);
            if (adBizNo != null && !adBizNo.isEmpty()) {
                Log.other("鱼塘📡发送广告通知");
                notifyAdStart(adBizNo);
                sleep(DELAY_SHORT);
            }

            // 等待浏览时间
            sleep(DELAY_LOOK_BASE);
            sleep(ThreadLocalRandom.current().nextInt(DELAY_FLOAT));

            // ⭐ 调用 finishTask（含 adBizNo）
            finishTaskById(taskId, adBizNo);
            sleep(DELAY_SHORT);

            Log.other("鱼塘✅浏览完成[钓鱼活动广告]");
        } catch (Exception e) {
            Log.record("鱼塘❌处理广告失败[" + e.getMessage() + "]");
        }
    }

    /**
     * 检查是否为浏览广告类任务
     * 例如："看精选商品得钓竿"、"看视频得奖励"等
     */
    private boolean isBrowseAdTask(String taskId) {
        String displayName = getTaskDisplayName(taskId);
        if (displayName == null) {
            return false;
        }
        // 包含"看"、"浏览"、"广告"、"商品"等关键词
        return displayName.contains("看") ||
               displayName.contains("浏览") ||
               displayName.contains("广告") ||
               displayName.contains("商品") ||
               displayName.contains("视频");
    }

    /**
     * 检查是否为兑换类任务（如消耗肥料兑换钓竿）
     */
    private boolean isExchangeTask(String taskId) {
        String displayName = getTaskDisplayName(taskId);
        if (displayName == null) {
            return false;
        }
        // 包含"消耗"、"兑换"、"肥料"等关键词
        return displayName.contains("消耗") ||
               displayName.contains("兑换") ||
               displayName.contains("肥料");
    }

    /**
     * 查询单个任务的状态
     * 用于多次任务（如浏览广告 0/3）判断是否需要继续循环
     */
    private String getTaskStatus(String taskId) {
        try {
            String result = requestString(API_LIST_TASK, buildBaseRequest(SCENE_GAME_CENTER));
            if (result == null) {
                return "";
            }
            JSONObject json = new JSONObject(result);
            JSONArray taskList = json.optJSONArray("taskList");
            if (taskList == null) {
                return "";
            }
            for (int i = 0; i < taskList.length(); i++) {
                JSONObject task = taskList.getJSONObject(i);
                if (taskId.equals(task.optString("taskId", ""))) {
                    return task.optString("taskStatus", "");
                }
            }
        } catch (Exception e) {
            Log.record("鱼塘⚠️查询任务状态失败[" + e.getMessage() + "]");
        }
        return "";
    }

    /**
     * 处理兑换类任务（如消耗肥料兑换钓竿）
     * 这类任务需要先执行兑换操作，再调用 finishTask
     */
    private boolean handleExchangeTask(String taskId) {
        try {
            String displayName = getTaskDisplayName(taskId);
            Log.other("鱼塘💰兑换[" + displayName + "]");

            // ⭐ 先调用 finishTask 标记完成
            boolean finishSuccess = finishTaskByIdWithResult(taskId);
            if (!finishSuccess) {
                Log.other("鱼塘❌兑换失败[" + displayName + "]#finishTask未成功");
                return false;
            }
            sleep(DELAY_SHORT);

            // ⭐ 等待服务器状态同步
            Log.other("鱼塘⏳等待同步...");
            sleep(DELAY_MEDIUM);
            sleep(ThreadLocalRandom.current().nextInt(500));

            Log.other("鱼塘✅兑换完成[" + displayName + "]");
            return true;
        } catch (Exception e) {
            Log.record("鱼塘❌兑换失败[" + e.getMessage() + "]");
            return false;
        }
    }

    /**
     * 处理游戏类任务（如农场解螺丝、玩寻道大千）
     * 游戏机制：点击进入游戏等待指定时间（不需要玩、不需要注册），然后 finishTask
     * @return true=finishTask成功，false=失败
     */
    private boolean handleGameTask(String taskId) {
        try {
            String displayName = getTaskDisplayName(taskId);
            Log.other("鱼塘🎮游戏[" + displayName + "]");

            // 从任务名称提取等待时间（如"玩寻道大千30s" → 30秒）
            int waitSeconds = extractWaitTime(taskId);
            if (waitSeconds > 0) {
                Log.other("鱼塘⏳进入游戏等待" + waitSeconds + "s[" + displayName + "]");
                sleep(waitSeconds * 1000L);
                sleep(ThreadLocalRandom.current().nextInt(DELAY_FLOAT));
            }

            // ⭐ 调用 finishTask 标记完成
            boolean finishSuccess = finishTaskByIdWithResult(taskId);
            sleep(DELAY_SHORT);

            if (!finishSuccess) {
                Log.other("鱼塘❌游戏失败[" + displayName + "]#finishTask未成功");
                return false;
            }

            // ⭐ 等待服务器状态同步
            Log.other("鱼塘⏳等待同步...");
            sleep(DELAY_MEDIUM);
            sleep(ThreadLocalRandom.current().nextInt(500));

            Log.other("鱼塘✅游戏完成[" + displayName + "]");
            return true;
        } catch (Exception e) {
            Log.record("鱼塘❌游戏失败[" + e.getMessage() + "]");
            return false;
        }
    }

    /**
     * 处理浏览广告任务（看精选商品得钓竿）
     * 根据抓包数据，正确流程是：
     * 1. 从 listTask 获取 adBizNo
     * 2. 调用 fishpondAdNotice 通知开始浏览
     * 3. 等待15秒
     * 4. 调用 antiep.finishTask（含 pwPreBizId=adBizNo, source=ADBASICLIB, requestType=RPC）
     * 5. finishTask 返回 deltaAwardCount 直接给奖励，无需 receiveTaskAward
     * 6. 支持多次任务（如 0/3），循环直到任务完成或失败
     * @return true=成功，false=失败
     */
    private boolean handleBrowseAdTask(String taskId) {
        try {
            String displayName = getTaskDisplayName(taskId);
            Log.other("鱼塘📺浏览[" + displayName + "]");

            // ⭐ 从任务名称中解析最大次数，如"看精选商品得钓竿（0/3）" → 3
            // 如果解析不到，默认1次
            int maxCount = extractMaxCount(displayName);
            Log.other("鱼塘📺浏览[" + displayName + "]#最多" + maxCount + "次");

            int maxLoops = Math.min(Math.max(maxCount, 1), 10);
            int loopCount = 0;

            while (loopCount < maxLoops) {
                loopCount++;

                // ⭐ 步骤1: 获取任务的 adBizNo（每次重新获取，因为广告ID会变化）
                String adBizNo = getAdBizNoForTask(taskId);
                if (adBizNo == null || adBizNo.isEmpty()) {
                    Log.other("鱼塘❌浏览失败[" + displayName + "]#无adBizNo");
                    addToBlacklist(taskId);
                    return false;
                }

                // ⭐ 步骤2: 调用 fishpondAdNotice 通知开始浏览
                Log.other("鱼塘📡发送广告通知");
                notifyAdStart(adBizNo);
                sleep(DELAY_SHORT);

                // ⭐ 步骤3: 等待15秒让服务器记录浏览时长
                Log.other("鱼塘⏳浏览15s...");
                sleep(DELAY_LOOK_BASE);  // 15秒
                sleep(ThreadLocalRandom.current().nextInt(DELAY_FLOAT));

                // ⭐ 步骤4: 调用 finishTask（必须传入 adBizNo 作为 pwPreBizId）
                Log.other("鱼塘✅提交完成状态[" + displayName + "]#第" + loopCount + "次");
                boolean finishSuccess = finishTaskByIdWithResult(taskId, adBizNo);
                sleep(DELAY_SHORT);

                if (!finishSuccess) {
                    Log.other("鱼塘❌浏览失败[" + displayName + "]#第" + loopCount + "次finishTask未成功");
                    return false;
                }

                // ⭐ 步骤5: 重新查询任务状态，判断是否还需要继续
                sleep(DELAY_MEDIUM);
                String taskStatus = getTaskStatus(taskId);

                if (TASK_STATUS_TODO.equals(taskStatus)) {
                    // 任务还有剩余次数（如 1/3 → 2/3），继续循环
                    Log.other("鱼塘🔄浏览继续[" + displayName + "]#剩余次数");
                    sleep(DELAY_BABA_FARM_BASE + ThreadLocalRandom.current().nextInt(DELAY_BABA_FARM_FLOAT));
                    continue;
                } else if (TASK_STATUS_FINISHED.equals(taskStatus)) {
                    // 全部次数完成，不再需要 receiveTaskAward（finishTask已给奖励）
                    Log.other("鱼塘✅浏览完成[" + displayName + "]#全部" + loopCount + "次");
                    return true;
                } else {
                    // 其他状态，视为完成
                    Log.other("鱼塘✅浏览完成[" + displayName + "]#状态[" + taskStatus + "]");
                    return true;
                }
            }

            Log.other("鱼塘⚠️浏览任务[" + displayName + "]#达到最大循环次数" + maxLoops);
            return true;  // 虽然没全部完成，但不视为失败

        } catch (Exception e) {
            Log.record("鱼塘❌浏览失败[" + e.getMessage() + "]");
            return false;
        }
    }

    /**
     * 从任务名称中解析最大完成次数
     * 例如："看精选商品得钓竿（0/3）" → 3
     *       "看精选商品得钓竿（1/3）" → 3
     *       "看精选商品得钓竿" → 1（默认1次）
     */
    private int extractMaxCount(String displayName) {
        if (displayName == null) {
            return 1;
        }
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\(\\d+/(\\d+)\\)");
        java.util.regex.Matcher matcher = pattern.matcher(displayName);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                return 1;
            }
        }
        return 1;  // 默认1次
    }

    /**
     * 获取任务对应的 adBizNo（广告业务编号）
     * 从 listTask 返回的数据中提取
     */
    private String getAdBizNoForTask(String taskId) {
        try {
            // 重新查询任务列表获取 adBizNo
            String result = requestString(API_LIST_TASK, buildBaseRequest(SCENE_GAME_CENTER));
            if (result == null) {
                return null;
            }

            JSONObject json = new JSONObject(result);
            JSONArray taskList = json.optJSONArray("taskList");
            if (taskList == null) {
                return null;
            }

            for (int i = 0; i < taskList.length(); i++) {
                JSONObject task = taskList.getJSONObject(i);
                String currentTaskId = task.optString("taskId", "");
                if (taskId.equals(currentTaskId)) {
                    return task.optString("adBizNo", "");
                }
            }
        } catch (Exception e) {
            Log.record("鱼塘⚠️获取adBizNo失败[" + e.getMessage() + "]");
        }
        return null;
    }

    /**
     * 调用 fishpondAdNotice 接口通知广告开始浏览
     */
    private void notifyAdStart(String adBizNo) {
        try {
            String request = String.format(
                "[{\"adBizNo\":\"%s\",\"appMode\":\"normal\",\"requestType\":\"%s\",\"sceneCode\":\"%s\",\"source\":\"%s\",\"version\":\"%s\"}]",
                adBizNo,
                REQUEST_TYPE_NORMAL,
                SCENE_GAME_CENTER,
                SOURCE_RECENTLY_USED,
                VERSION
            );

            String result = requestString(API_FISHPOND_AD_NOTICE, request);
            if (result != null && isSuccess(result)) {
                Log.record("鱼塘✅广告通知成功");
            } else {
                Log.record("鱼塘⚠️广告通知失败");
            }
        } catch (Exception e) {
            Log.record("鱼塘⚠️广告通知异常[" + e.getMessage() + "]");
        }
    }

    private void finishTaskById(String taskId) {
        finishTaskById(taskId, null);
    }

    /**
     * 调用 finishTask 接口，可选传入 adBizNo（浏览广告类任务需要）
     */
    private void finishTaskById(String taskId, String adBizNo) {
        try {
            // 根据抓包，不同任务的finishTask使用不同的sceneCode
            // GYG_XLIGHT_JX_BUSINEES_3 → ANTFISHPOND_TASK
            // FISHPOND_NCLY_GAME_* → ANTFISHPOND_TASK
            // FISH_ACTIVITY_RESULT_AD → ANTFISHPOND_ACTIVITY_RESULT_AD
            String sceneCode = getTaskSceneCode(taskId);

            String request = buildFinishTaskRequest(sceneCode, taskId, adBizNo);
            String result = requestString(API_FINISH_TASK, request);

            if (result != null && isSuccess(result)) {
                JSONObject jSONObject = new JSONObject(result);
                JSONObject finishAwardResultVO = jSONObject.optJSONObject("finishAwardResultVO");
                if (finishAwardResultVO != null) {
                    int deltaAwardCount = finishAwardResultVO.optInt("deltaAwardCount", 0);
                    Log.other("鱼塘✅完成[" + getTaskDisplayName(taskId) + "]" + (deltaAwardCount > 0 ? "#deltaAwardCount[" + deltaAwardCount + "]" : ""));
                } else {
                    Log.other("鱼塘✅完成[" + getTaskDisplayName(taskId) + "]");
                }
            } else {
                // finishTask 失败：记录详细错误信息并加入黑名单
                if (result != null) {
                    try {
                        JSONObject errorJson = new JSONObject(result);
                        String code = errorJson.optString("code", "");
                        String desc = errorJson.optString("desc", errorJson.optString("memo", ""));
                        Log.record("鱼塘❌finishTask失败[" + getTaskDisplayName(taskId) + "]#code[" + code + "]#desc[" + desc + "]");
                        // code=400000001 表示"任务全局配置不存在"，直接拉黑
                        if ("400000001".equals(code)) {
                            addToBlacklist(taskId);
                            Log.other("鱼塘⚠️任务配置不存在，已加入黑名单[" + getTaskDisplayName(taskId) + "]");
                        }
                    } catch (Exception parseEx) {
                        Log.record("鱼塘❌finishTask失败[" + getTaskDisplayName(taskId) + "]#结果[" + result + "]");
                    }
                } else {
                    Log.record("鱼塘❌finishTask失败[" + getTaskDisplayName(taskId) + "]#接口无返回");
                }
            }
        } catch (Exception e) {
            Log.record("鱼塘❌finishTask异常[" + e.getMessage() + "]");
        }
    }

    /**
     * 调用 finishTask 并返回结果（用于游戏类等需要知道是否成功的场景）
     * @return true=成功，false=失败
     */
    private boolean finishTaskByIdWithResult(String taskId) {
        return finishTaskByIdWithResult(taskId, null);
    }

    private boolean finishTaskByIdWithResult(String taskId, String adBizNo) {
        try {
            String sceneCode = getTaskSceneCode(taskId);
            String request = buildFinishTaskRequest(sceneCode, taskId, adBizNo);
            String result = requestString(API_FINISH_TASK, request);

            if (result != null && isSuccess(result)) {
                JSONObject jSONObject = new JSONObject(result);
                JSONObject finishAwardResultVO = jSONObject.optJSONObject("finishAwardResultVO");
                if (finishAwardResultVO != null) {
                    int deltaAwardCount = finishAwardResultVO.optInt("deltaAwardCount", 0);
                    Log.other("鱼塘✅完成[" + getTaskDisplayName(taskId) + "]" + (deltaAwardCount > 0 ? "#deltaAwardCount[" + deltaAwardCount + "]" : ""));
                } else {
                    Log.other("鱼塘✅完成[" + getTaskDisplayName(taskId) + "]");
                }
                return true;
            } else {
                if (result != null) {
                    try {
                        JSONObject errorJson = new JSONObject(result);
                        String code = errorJson.optString("code", "");
                        String desc = errorJson.optString("desc", errorJson.optString("memo", ""));
                        Log.record("鱼塘❌finishTask失败[" + getTaskDisplayName(taskId) + "]#code[" + code + "]#desc[" + desc + "]");
                        if ("400000001".equals(code)) {
                            addToBlacklist(taskId);
                            Log.other("鱼塘⚠️任务配置不存在，已加入黑名单[" + getTaskDisplayName(taskId) + "]");
                        }
                    } catch (Exception parseEx) {
                        Log.record("鱼塘❌finishTask失败[" + getTaskDisplayName(taskId) + "]#结果[" + result + "]");
                    }
                } else {
                    Log.record("鱼塘❌finishTask失败[" + getTaskDisplayName(taskId) + "]#接口无返回");
                }
                return false;
            }
        } catch (Exception e) {
            Log.record("鱼塘❌finishTask异常[" + e.getMessage() + "]");
            return false;
        }
    }

    /**
     * 根据taskId获取正确的sceneCode（用于finishTask和receiveTaskAward）
     * 抓包分析：
     * - FISH_ACTIVITY_RESULT_AD → ANTFISHPOND_ACTIVITY_RESULT_AD
     * - 其他任务（GYG_*, FISHPOND_NCLY_GAME_*, NORMAL_*等）→ ANTFISHPOND_TASK
     */
    private String getTaskSceneCode(String taskId) {
        if ("FISH_ACTIVITY_RESULT_AD".equals(taskId)) {
            return SCENE_AD_RESULT;
        }
        return SCENE_FISH_TASK;
    }

    private boolean receiveTaskAward(String taskId, String taskType) {
        try {
            String sceneCode = getTaskSceneCode(taskType);

            String strRequestString = requestString(API_RECEIVE_AWARD, buildReceiveAwardRequest(sceneCode, taskType));
            if (strRequestString == null) {
                Log.other("鱼塘❌领取失败[" + getTaskDisplayName(taskType) + "]#接口无返回");
                addToBlacklist(taskId);
                return false;
            }

            JSONObject jSONObject = new JSONObject(strRequestString);
            String resultCode = jSONObject.optString("resultCode");

            if ("TASK_NOT_FINISHED".equals(resultCode)) {
                Log.other("鱼塘❌领取失败[" + getTaskDisplayName(taskType) + "]#任务未完成");
                addToBlacklist(taskId);
                return false;
            }

            if (isSuccess(jSONObject)) {
                JSONObject awardInfo = jSONObject.optJSONObject("awardInfo");
                if (awardInfo != null) {
                    int rodCount = awardInfo.optInt("rodCount", 0);
                    Log.other("鱼塘🎁领取[" + getTaskDisplayName(taskType) + "]#获得[钓竿*" + rodCount + "]");
                } else {
                    Log.other("鱼塘🎁领取成功[" + getTaskDisplayName(taskType) + "]");
                }
                return true;
            } else {
                Log.other("鱼塘❌领取失败[" + getTaskDisplayName(taskType) + "][" + resultCode + "]");
                addToBlacklist(taskId);
                return false;
            }
        } catch (Throwable th) {
            Log.other("鱼塘❌领取异常[" + getTaskDisplayName(taskType) + "][" + th.getMessage() + "]");
            addToBlacklist(taskId);
            return false;
        }
    }

    private void addToBlacklist(String taskId) {
        if (!FishConfig.isAutoFishTaskBlacklist() || taskId == null || taskId.isEmpty()) return;
        Set<String> fishTaskBlacklist = FishConfig.getFishTaskBlacklist();
        if (fishTaskBlacklist != null && !fishTaskBlacklist.contains(taskId)) {
            FishConfig.addToFishTaskBlacklist(taskId);
            Log.other("鱼塘⚠️加入黑名单[" + getTaskDisplayName(taskId) + "]");
        }
    }

    private void triggerSubplotsActivity(boolean r23) {
        try {
            if (r23) {
                Log.other("鱼塘🎁处理活动任务");
            }

            String queryRequest = buildBaseRequest(SCENE_GAME_CENTER);
            String queryResult = requestString(API_QUERY_SUBPLOTS, queryRequest);

            if (queryResult == null || !isSuccess(queryResult)) {
                Log.record("鱼塘⚠️查询活动失败");
                return;
            }

            JSONObject queryJson = new JSONObject(queryResult);
            JSONArray activitiesArray = queryJson.optJSONArray("subplotsActivityList");

            if (activitiesArray == null || activitiesArray.length() == 0) {
                Log.record("鱼塘📊无待处理活动");
                return;
            }

            Log.other("鱼塘📊检测到活动任务");

            for (int i = 0; i < activitiesArray.length(); i++) {
                JSONObject activity = activitiesArray.getJSONObject(i);
                String activityId = activity.optString("activityId", "");
                String status = activity.optString("status", "");
                String activityType = activity.optString("activityType", "");

                // 跳过空类型和社交动态
                if (activityType.isEmpty() || "DYNAMIC_LIST".equals(activityType)) {
                    continue;
                }

                // 处理GIFT_BOX类型活动（每日宝箱）
                // 根据抓包数据：使用 activityType + actionType:"receiveAward" 触发，触发即领取
                if ("GIFT_BOX".equals(activityType)) {
                    if ("FINISHED".equals(status)) {
                        Log.other("鱼塘🎁每日宝箱已领取");
                    } else if ("TODO".equals(status)) {
                        Log.other("鱼塘🎁触发每日宝箱");
                        String triggerRequest = String.format(
                            "[{\"actionType\":\"receiveAward\",\"activityType\":\"GIFT_BOX\",\"requestType\":\"NORMAL\",\"sceneCode\":\"%s\",\"source\":\"%s\",\"version\":\"%s\"}]",
                            SCENE_GAME_CENTER,
                            SOURCE_RECENTLY_USED,
                            VERSION
                        );
                        String triggerResult = requestString(API_TRIGGER_SUBPLOTS, triggerRequest);
                        if (triggerResult != null && isSuccess(triggerResult)) {
                            JSONObject triggerJson = new JSONObject(triggerResult);
                            JSONObject triggerActivity = triggerJson.optJSONObject("triggerSubplotsActivity");
                            if (triggerActivity != null) {
                                String extend = triggerActivity.optString("extend", "");
                                if (!extend.isEmpty()) {
                                    try {
                                        JSONObject extendJson = new JSONObject(extend);
                                        String awardCount = extendJson.optString("awardCount", "0");
                                        String awardType = extendJson.optString("awardType", "");
                                        Log.other("鱼塘🎁领取[每日宝箱]#获得[" + toAwardChineseName(awardType) + "*" + awardCount + "]");
                                    } catch (Exception e) {
                                        Log.other("鱼塘🎁领取[每日宝箱]#成功");
                                    }
                                } else {
                                    Log.other("鱼塘🎁领取[每日宝箱]#成功");
                                }
                            } else {
                                Log.other("鱼塘🎁触发[每日宝箱]#成功");
                            }
                        }
                    }
                    sleep(ThreadLocalRandom.current().nextInt(DELAY_FLOAT) + DELAY_BABA_FARM_BASE);
                    continue;
                }

                // 处理FISH_ACTIVITY类型（根据抓包数据，无activityId，需从extend解析taskType）
                if ("FISH_ACTIVITY".equals(activityType)) {
                    String extendStr = activity.optString("extend", "");
                    if ("FINISHED".equals(status)) {
                        // 钓鱼活动已完成，领取奖励（抓包显示用 actionType=receiveAward）
                        try {
                            JSONObject extendJson = new JSONObject(extendStr);
                            String taskType = extendJson.optString("taskType", "");
                            String awardCount = extendJson.optString("awardCount", "0");
                            String awardType = extendJson.optString("awardType", "");
                            Log.other(String.format("鱼塘🎣钓鱼活动已完成[%s]#获得[%s*%s]", taskType, toAwardChineseName(awardType), awardCount));
                            // 用triggerSubplotsActivity领取奖励（actionType=receiveAward，与抓包一致）
                            String triggerRequest = String.format(
                                "[{\"actionType\":\"receiveAward\",\"activityType\":\"FISH_ACTIVITY\",\"requestType\":\"NORMAL\",\"sceneCode\":\"%s\",\"source\":\"%s\",\"version\":\"%s\"}]",
                                SCENE_GAME_CENTER, SOURCE_RECENTLY_USED, VERSION);
                            String triggerResult = requestString(API_TRIGGER_SUBPLOTS, triggerRequest);
                            if (triggerResult != null && isSuccess(triggerResult)) {
                                Log.other("鱼塘🎣领取[钓鱼活动奖励]#成功");
                                // 抓包显示领取后返回 adInfo，包含后续广告浏览任务
                                // 需要解析 adInfo 并触发 FISH_ACTIVITY_RESULT_AD 流程
                                JSONObject triggerJson = new JSONObject(triggerResult);
                                JSONObject triggerActivity = triggerJson.optJSONObject("triggerSubplotsActivity");
                                if (triggerActivity != null) {
                                    String adExtend = triggerActivity.optString("extend", "");
                                    if (!adExtend.isEmpty()) {
                                        try {
                                            JSONObject adExtendJson = new JSONObject(adExtend);
                                            JSONObject adInfo = adExtendJson.optJSONObject("adInfo");
                                            if (adInfo != null) {
                                                String adBizNo = adInfo.optString("adBizNo", "");
                                                String adTaskType = adInfo.optString("taskType", "");
                                                String targetUrl = adInfo.optString("targetUrl", "");
                                                Log.other("鱼塘📺钓鱼活动广告[" + adTaskType + "]#adBizNo[" + adBizNo + "]");
                                                // 通知广告开始
                                                if (!adBizNo.isEmpty()) {
                                                    notifyAdStart(adBizNo);
                                                    sleep(DELAY_SHORT);
                                                }
                                                // 等待浏览时间
                                                sleep(DELAY_LOOK_BASE);
                                                sleep(ThreadLocalRandom.current().nextInt(DELAY_FLOAT));
                                                // 完成广告任务
                                                finishTaskById(adTaskType, adBizNo);
                                                sleep(DELAY_SHORT);
                                                Log.other("鱼塘✅钓鱼活动广告完成[" + adTaskType + "]");
                                            }
                                        } catch (Exception adEx) {
                                            Log.record("鱼塘⚠️解析广告信息失败[" + adEx.getMessage() + "]");
                                        }
                                    }
                                }
                            } else {
                                Log.other("鱼塘❌领取钓鱼活动奖励失败");
                            }
                        } catch (Exception e) {
                            Log.record("鱼塘⚠️解析钓鱼活动失败[" + e.getMessage() + "]");
                        }
                    } else if ("TODO".equals(status)) {
                        if (!extendStr.isEmpty()) {
                            try {
                                JSONObject extendJson = new JSONObject(extendStr);
                                String taskType = extendJson.optString("taskType", "");
                                int leftFishTimes = extendJson.optInt("leftFishTimes", -1);
                                Log.other(String.format("鱼塘🎣钓鱼活动[%s]#剩余%d次", taskType, leftFishTimes));
                                // FISH_ACTIVITY TODO 不需要单独触发，通过钓鱼完成
                            } catch (Exception e) {
                                Log.record("鱼塘⚠️解析活动失败[" + e.getMessage() + "]");
                            }
                        }
                    }
                    continue;
                }

                // 处理TOMORROW_ROD类型（明日钓竿）
                if ("TOMORROW_ROD".equals(activityType)) {
                    if ("TODAY_FINISH".equals(status) || "FINISHED".equals(status)) {
                        Log.other("鱼塘🌅明日钓竿已完成");
                    } else if ("TODO".equals(status) || "TODAY_TODO".equals(status)) {
                        Log.other("鱼塘🌅触发明日钓竿");
                        triggerTomorrowRodAward();
                        sleep(ThreadLocalRandom.current().nextInt(DELAY_FLOAT) + DELAY_BABA_FARM_BASE);
                    }
                    continue;
                }

                // 处理需要 activityId 的活动
                if (activityId.isEmpty()) {
                    Log.record("鱼塘⚠️缺少activityId[" + activityType + "]");
                    continue;
                }

                if ("TODO".equals(status)) {
                    Log.other("鱼塘🚀触发活动[" + activityType + "]");

                    String triggerResult;

                    // 其他活动使用 activityId 格式
                    if (!activityId.isEmpty()) {
                        // 其他活动使用 activityId 格式
                        String triggerRequest = String.format(
                            "[{\"activityId\":\"%s\",\"requestType\":\"NORMAL\",\"sceneCode\":\"%s\",\"source\":\"%s\",\"version\":\"%s\"}]",
                            activityId,
                            SCENE_GAME_CENTER,
                            SOURCE_RECENTLY_USED,
                            VERSION
                        );
                        triggerResult = requestString(API_TRIGGER_SUBPLOTS, triggerRequest);
                    } else {
                        Log.record("鱼塘⚠️缺少参数[" + activityType + "]");
                        continue;
                    }

                    if (triggerResult != null && isSuccess(triggerResult)) {
                        Log.other("鱼塘✅触发成功[" + activityType + "]");
                        sleep(DELAY_MEDIUM);

                        // TOMORROW_ROD 不需要领取奖励，其他活动需要
                        if (!"TOMORROW_ROD".equals(activityType)) {
                            receiveActivityAward(activityId);
                        }
                    }

                    sleep(ThreadLocalRandom.current().nextInt(DELAY_FLOAT) + DELAY_BABA_FARM_BASE);
                }
            }

            Log.other("鱼塘✅活动处理完成");
        } catch (Exception e) {
            Log.record("鱼塘❌处理活动异常[" + e.getMessage() + "]");
        }
    }

    private void receiveActivityAward(String activityId) {
        try {
            String request = String.format(
                "[{\"activityId\":\"%s\",\"requestType\":\"NORMAL\",\"sceneCode\":\"%s\",\"source\":\"%s\",\"version\":\"%s\"}]",
                activityId,
                SCENE_GAME_CENTER,
                SOURCE_RECENTLY_USED,
                VERSION
            );

            String result = requestString(API_RECEIVE_AWARD, request);

            if (result != null && isSuccess(result)) {
                JSONObject json = new JSONObject(result);
                JSONObject awardInfo = json.optJSONObject("awardInfo");
                if (awardInfo != null) {
                    int rodCount = awardInfo.optInt("rodCount", 0);
                    Log.other("鱼塘🎁领取[活动奖励]#获得[钓竿*" + rodCount + "]");
                } else {
                    Log.other("鱼塘🎁领取[活动奖励]#成功");
                }
            } else {
                Log.record("鱼塘⚠️领取活动奖励失败");
            }
        } catch (Exception e) {
            Log.record("鱼塘❌领取活动奖励异常[" + e.getMessage() + "]");
        }
    }

    /**
     * 检查并领取 GIFT_BOX 每日宝箱奖励（独立于 triggerSubplotsActivity）
     * 根据抓包数据，GIFT_BOX 真实流程：
     * 1. querySubplotsActivity 查询状态
     * 2. 如果 TODO → triggerSubplotsActivity（参数：activityType="GIFT_BOX" + actionType="receiveAward"）
     * 3. 触发即领取，无需单独调用 receiveAward
     */
    private void checkAndReceiveGiftBox() {
        try {
            Log.other("鱼塘🔍检查每日宝箱");

            String queryResult = requestString(
                API_QUERY_SUBPLOTS,
                buildBaseRequest(SCENE_GAME_CENTER)
            );

            if (queryResult == null) {
                Log.record("鱼塘⚠️查询宝箱失败");
                return;
            }

            JSONObject queryJson = new JSONObject(queryResult);
            JSONArray activitiesArray = queryJson.optJSONArray("subplotsActivityList");

            if (activitiesArray == null || activitiesArray.length() == 0) {
                Log.record("鱼塘📊无活动任务");
                return;
            }

            boolean giftBoxFound = false;
            for (int i = 0; i < activitiesArray.length(); i++) {
                JSONObject activity = activitiesArray.getJSONObject(i);
                String activityType = activity.optString("activityType", "");
                String status = activity.optString("status", "");

                if ("GIFT_BOX".equals(activityType)) {
                    giftBoxFound = true;
                    Log.other("鱼塘🎁检测到每日宝箱[" + toStatusChineseName(status) + "]");

                    if ("FINISHED".equals(status)) {
                        Log.other("鱼塘✅每日宝箱已领取");
                        return;
                    } else if ("TODO".equals(status)) {
                        Log.other("鱼塘🎁触发每日宝箱");
                        // 根据抓包数据：triggerSubplotsActivity 参数使用 activityType + actionType
                        String triggerRequest = String.format(
                            "[{\"actionType\":\"receiveAward\",\"activityType\":\"GIFT_BOX\",\"requestType\":\"NORMAL\",\"sceneCode\":\"%s\",\"source\":\"%s\",\"version\":\"%s\"}]",
                            SCENE_GAME_CENTER,
                            SOURCE_RECENTLY_USED,
                            VERSION
                        );
                        String triggerResult = requestString(API_TRIGGER_SUBPLOTS, triggerRequest);
                        if (triggerResult != null && isSuccess(triggerResult)) {
                            // 检查返回的奖励信息
                            JSONObject triggerJson = new JSONObject(triggerResult);
                            JSONObject triggerActivity = triggerJson.optJSONObject("triggerSubplotsActivity");
                            if (triggerActivity != null) {
                                String extend = triggerActivity.optString("extend", "");
                                if (!extend.isEmpty()) {
                                    try {
                                        JSONObject extendJson = new JSONObject(extend);
                                        String awardCount = extendJson.optString("awardCount", "0");
                                        String awardType = extendJson.optString("awardType", "");
                                        Log.other("鱼塘🎁领取[每日宝箱]#获得[" + toAwardChineseName(awardType) + "*" + awardCount + "]");
                                    } catch (Exception e) {
                                        Log.other("鱼塘🎁领取[每日宝箱]#成功");
                                    }
                                } else {
                                    Log.other("鱼塘🎁领取[每日宝箱]#成功");
                                }
                            } else {
                                Log.other("鱼塘🎁触发[每日宝箱]#成功");
                            }
                        } else {
                            Log.other("鱼塘❌每日宝箱触发失败");
                        }
                        return;
                    }
                    break;
                }
            }

            if (!giftBoxFound) {
                Log.record("鱼塘📊未检测到每日宝箱");
            }

        } catch (Exception e) {
            Log.record("鱼塘❌检查宝箱异常[" + e.getMessage() + "]");
        }
    }

    /**
     * 触发明日钓竿奖励领取
     * 通过 triggerSubplotsActivity 接口，使用 activityType="TOMORROW_ROD" + actionType="FINISH"
     */
    private void triggerTomorrowRodAward() {
        try {
            // 本次运行已触发过，跳过重复请求
            if (tomorrowRodTriggered) {
                Log.other("鱼塘🌅明日钓竿本次已触发，跳过");
                return;
            }
            tomorrowRodTriggered = true;

            String triggerRequest = String.format(
                "[{\"actionType\":\"FINISH\",\"activityType\":\"TOMORROW_ROD\",\"requestType\":\"NORMAL\",\"sceneCode\":\"%s\",\"source\":\"%s\",\"version\":\"%s\"}]",
                SCENE_GAME_CENTER,
                SOURCE_RECENTLY_USED,
                VERSION
            );
            String triggerResult = requestString(API_TRIGGER_SUBPLOTS, triggerRequest);
            if (triggerResult != null && isSuccess(triggerResult)) {
                Log.other("鱼塘🌅领取[明日钓竿]#成功");
                // 重新查询状态确认
                sleep(DELAY_MEDIUM);
                String syncResult = requestString(API_FISHPOND_SYNC_INDEX, buildRequestWithSyncType(SCENE_GAME_CENTER));
                if (syncResult != null && isSuccess(syncResult)) {
                    JSONObject syncJson = new JSONObject(syncResult);
                    JSONObject tomorrowRod = syncJson.optJSONObject("tomorrowRod");
                    if (tomorrowRod != null) {
                        int tomorrowRodCount = tomorrowRod.optInt("tomorrowRodCount", 0);
                        String status = tomorrowRod.optString("status", "");
                        Log.other(String.format("鱼塘🌅明日钓竿[%s]#明日可领%d个钓竿", toStatusChineseName(status), tomorrowRodCount));
                    }
                }
            } else {
                Log.other("鱼塘❌明日钓竿触发失败");
            }
        } catch (Exception e) {
            Log.record("鱼塘❌触发明日钓竿异常[" + e.getMessage() + "]");
        }
    }

    /**
     * 领取 GIFT_BOX 礼盒奖励
     * @param activityId 从接口返回的真实 activityId
     */
    private void receiveGiftBoxAward(String activityId) {
        try {
            // 使用真实的 activityId，如果为空则回退到 "GIFT_BOX"
            String realActivityId = (activityId != null && !activityId.isEmpty()) ? activityId : "GIFT_BOX";
            String request = String.format(
                "[{\"activityId\":\"%s\",\"requestType\":\"%s\",\"sceneCode\":\"%s\",\"source\":\"%s\",\"version\":\"%s\"}]",
                realActivityId,
                REQUEST_TYPE_NORMAL,
                SCENE_GAME_CENTER,
                SOURCE_RECENTLY_USED,
                VERSION
            );

            String result = requestString(API_RECEIVE_AWARD, request);

            if (result == null) {
                Log.other("鱼塘❌礼盒领取失败#接口无返回");
                return;
            }

            JSONObject json = new JSONObject(result);
            String resultCode = json.optString("resultCode", "");

            if (isSuccess(json)) {
                JSONObject awardInfo = json.optJSONObject("awardInfo");
                if (awardInfo != null) {
                    int rodCount = awardInfo.optInt("rodCount", 0);
                    Log.other("鱼塘🎁领取[礼盒]#获得[钓竿*" + rodCount + "]");
                } else {
                    Log.other("鱼塘🎁领取[礼盒]#成功");
                }
            } else if ("TASK_NOT_FINISHED".equals(resultCode)) {
                Log.other("鱼塘❌礼盒领取失败#任务未完成");
            } else {
                Log.other("鱼塘❌礼盒领取失败[" + resultCode + "]");
            }

        } catch (Exception e) {
            Log.record("鱼塘❌礼盒领取异常[" + e.getMessage() + "]");
        }
    }

    // 保留无参方法用于向后兼容（triggerSubplotsActivity 中的调用）
    private void receiveGiftBoxAward() {
        receiveGiftBoxAward("GIFT_BOX");
    }

    private void triggerSubplotsActivity() {
        triggerSubplotsActivity(false);
    }

    /**
     * 模拟进入鱼塘弹窗（与真实APP流程一致）
     * 抓包流程: refinedOperation(ENTER_FISHPOND_POP) → querySubplotsActivity → ...
     */
    private void enterFishpond() {
        try {
            String request = String.format(
                "[{\"actionId\":\"ENTER_FISHPOND_POP\",\"requestType\":\"%s\",\"sceneCode\":\"%s\",\"source\":\"%s\",\"version\":\"%s\"}]",
                REQUEST_TYPE_NORMAL, SCENE_GAME_CENTER, SOURCE_RECENTLY_USED, VERSION
            );
            requestString(API_REFINED_OPERATION, request);
        } catch (Exception e) {
            Log.record("鱼塘⚠️enterFishpond异常[" + e.getMessage() + "]");
        }
    }

    private boolean checkExchangeReward() {
        JSONObject jSONObjectOptJSONObject;
        try {
            String strBuildBaseRequest = buildBaseRequest(SCENE_GAME_CENTER);
            String strRequestString = requestString(API_FISHPOND_INDEX, strBuildBaseRequest);
            if (strRequestString != null && (jSONObjectOptJSONObject = new JSONObject(strRequestString).optJSONObject("roundInfo")) != null && jSONObjectOptJSONObject.optBoolean("canExchange", false)) {
                Log.other("鱼塘💰兑换奖励");
                String strRequestString2 = requestString(API_EXCHANGE_REWARD, strBuildBaseRequest);
                if (strRequestString2 == null) {
                    Status.flagToday("fish_exchange_fail_" + getToday());
                    Log.other("鱼塘❌兑换失败#接口无返回");
                    return false;
                }
                JSONObject jSONObject = new JSONObject(strRequestString2);
                boolean zIsSuccess = isSuccess(jSONObject);
                if (zIsSuccess) {
                    Log.other("鱼塘💰兑换成功");
                    queryFishStatus(true);
                    return true;
                }
                Status.flagToday("fish_exchange_fail_" + getToday());
                Log.other("鱼塘❌兑换失败");
                return false;
            }
            return true;
        } catch (Throwable th) {
            Status.flagToday("fish_exchange_fail_" + getToday());
            Log.other("鱼塘❌兑换异常");
            return false;
        }
    }

    private void queryFishStatus(boolean z) {
        if (queryingStatus) return;
        queryingStatus = true;
        lastRodCount = -1;
        JSONObject jSONObjectOptJSONObject;
        try {
            String strRequestString = requestString(API_FISHPOND_SYNC_INDEX, buildRequestWithSyncType(SCENE_GAME_CENTER));
            if (isSuccess(strRequestString)) {
                JSONObject jSONObject = new JSONObject(strRequestString);
                lastRodCount = jSONObject.optInt("rodSumCount", 0);
                JSONObject jSONObjectOptJSONObject2 = jSONObject.optJSONObject("roundInfo");
                if (jSONObjectOptJSONObject2 != null && (jSONObjectOptJSONObject = jSONObjectOptJSONObject2.optJSONObject("fishAssetInfo")) != null) {
                    lastFishWeight = jSONObjectOptJSONObject.optDouble("currentFishWeight", 0.0d);
                    if (!z) {
                        Log.other(String.format("鱼塘📊钓竿%d#鱼获%.2fg/%sg#还需%sg", Integer.valueOf(lastRodCount), Double.valueOf(lastFishWeight), jSONObjectOptJSONObject.optString("targetFishWeight", "10000"), jSONObjectOptJSONObject.optString("diffFishWeight", "0")));
                    }
                }

                // 处理lastAdInfo（根据抓包数据）
                JSONObject lastAdInfo = jSONObject.optJSONObject("lastAdInfo");
                if (lastAdInfo != null) {
                    boolean complete = lastAdInfo.optBoolean("complete", false);
                    String adType = lastAdInfo.optString("adType", "");
                    String sceneCode = lastAdInfo.optString("sceneCode", "");

                    if (!complete && "FISH_ACTIVITY_AD".equals(adType)) {
                        Log.other("鱼塘📺处理钓鱼活动广告");
                        handleLastAdTask(lastAdInfo);
                    }
                }

                JSONObject jSONObjectOptJSONObject3 = jSONObject.optJSONObject("fishActivity");
                if (jSONObjectOptJSONObject3 != null) {
                    String status = jSONObjectOptJSONObject3.optString("status", "");
                    int leftFishTimes = jSONObjectOptJSONObject3.optInt("leftFishTimes", -1);

                    // 如果状态是 TODO 且有剩余次数，说明可以参与钓鱼活动
                    if ("TODO".equals(status) && leftFishTimes > 0) {
                        Log.other(String.format("鱼塘🎣钓鱼活动#剩余%d次", leftFishTimes));
                        // 不返回，继续执行后续逻辑
                    } else if (leftFishTimes == 0) {
                        // 次数用完，领取奖励
                        Log.other("鱼塘🎁领取钓鱼活动奖励");
                        triggerSubplotsActivity();
                        return;
                    }
                    // 其他情况继续执行
                }

                // 处理 tomorrowRod（明日钓竿）信息
                JSONObject tomorrowRod = jSONObject.optJSONObject("tomorrowRod");
                if (tomorrowRod != null && !z) {
                    String rodStatus = tomorrowRod.optString("status", "");
                    int todayRodCount = tomorrowRod.optInt("todayRodCount", 0);
                    int tomorrowRodCount = tomorrowRod.optInt("tomorrowRodCount", 0);
                    int targetCount = tomorrowRod.optInt("targetCount", 0);
                    int remainCount = Math.max(0, targetCount - todayRodCount);

                    if ("TODAY_FINISH".equals(rodStatus)) {
                        Log.other(String.format("鱼塘🌅明日钓竿已完成#今日钓%d次#明日可领%d个钓竿",
                            todayRodCount, tomorrowRodCount));
                        // 如果已完成但还没领取（tomorrowRodCount > 0），尝试触发领取
                        if (tomorrowRodCount > 0) {
                            Log.other("鱼塘🌅触发明日钓竿");
                            triggerTomorrowRodAward();
                        }
                    } else if ("TODAY_TODO".equals(rodStatus)) {
                        Log.other(String.format("鱼塘⏳明日钓竿待完成#还需%d次#明日可领%d个钓竿",
                            remainCount, tomorrowRodCount));
                        // 如果已完成目标（todayRodCount >= targetCount），触发领取
                        if (todayRodCount >= targetCount && targetCount > 0) {
                            Log.other("鱼塘🌅触发领取明日钓竿");
                            triggerTomorrowRodAward();
                        }
                    }
                }
                if (this.todayRewardEnd) {
                    return;
                }
                this.todayRewardEnd = true;
                Log.other("鱼塘📊今日活动奖励已领完");
            }
        } catch (Throwable th) {
            if (z) {
                return;
            }
            Log.other("鱼塘🪝查询状态失败[" + th.getMessage() + "]");
        } finally {
            queryingStatus = false;
        }
    }

    /**
     * 处理lastAdInfo中的广告任务
     * 根据抓包数据，该任务需要调用finishTask完成
     */
    private void handleLastAdTask(JSONObject lastAdInfo) {
        try {
            String taskId = lastAdInfo.optString("taskId", "");
            String sceneCode = lastAdInfo.optString("sceneCode", "");
            String adBizNo = lastAdInfo.optString("adBizNo", "");

            if (taskId.isEmpty() || sceneCode.isEmpty()) {
                Log.record("鱼塘⚠️lastAdInfo信息不完整");
                return;
            }

            Log.other("鱼塘📺处理钓鱼活动广告");
            sleep(DELAY_LOOK_BASE);
            sleep(ThreadLocalRandom.current().nextInt(DELAY_FLOAT));

            // 构造finishTask请求
            String outBizNo = "FISH_ACTIVITY_RESULT_AD_" + System.currentTimeMillis() + "_" + ThreadLocalRandom.current().nextInt(10000);
            String requestData = String.format(
                "[{\"finishBusinessInfo\":{\"pwPreBizId\":\"%s\"}," +
                "\"outBizNo\":\"%s\",\"requestType\":\"RPC\"," +
                "\"sceneCode\":\"%s\",\"source\":\"ADBASICLIB\"," +
                "\"taskType\":\"%s\"}]",
                adBizNo,
                outBizNo,
                sceneCode,
                taskId
            );

            String result = requestString(API_FINISH_TASK, requestData);

            if (isSuccess(result)) {
                JSONObject jSONObject = new JSONObject(result);
                JSONObject finishAwardResultVO = jSONObject.optJSONObject("finishAwardResultVO");
                if (finishAwardResultVO != null) {
                    int deltaAwardCount = finishAwardResultVO.optInt("deltaAwardCount", 0);
                    Log.other("鱼塘📺钓鱼活动广告完成" + (deltaAwardCount > 0 ? "#deltaAwardCount[" + deltaAwardCount + "]" : ""));
                } else {
                    Log.other("鱼塘📺钓鱼活动广告完成");
                }
                sleep(DELAY_MEDIUM);
                queryFishStatus(true);
            } else {
                Log.other("鱼塘❌钓鱼活动广告失败");
            }

        } catch (Throwable th) {
            Log.record("鱼塘❌lastAdInfo异常[" + th.getMessage() + "]");
        }
    }

    private void queryFishStatus() {
        queryFishStatus(false);
    }

    private FishResult performRodPositioning(String str, String str2) {
        if (str != null && !str.isEmpty()) {
            try {
                int iNextInt = ThreadLocalRandom.current().nextInt(REEL_IN_FLOAT) + REEL_IN_BASE;
                Log.other("鱼塘🎣等待" + iNextInt + "ms收杆");
                sleep((long) iNextInt);
                String strRequestString = requestString(API_ROD_POSITIONING, String.format("[{\"areaType\":\"%s\",\"bizNo\":\"%s\",\"requestType\":\"%s\",\"sceneCode\":\"%s\",\"source\":\"%s\",\"version\":\"%s\"}]", (str2 == null || str2.isEmpty()) ? "SPECIAL_BIG_ZONE" : str2, str, REQUEST_TYPE_NORMAL, SCENE_GAME_CENTER, SOURCE_RECENTLY_USED, VERSION));
                if (!isSuccess(strRequestString)) {
                    Log.other("鱼塘❌收杆定位失败");
                    return null;
                }
                JSONObject jSONObject = new JSONObject(strRequestString);
                JSONObject jSONObjectOptJSONObject = jSONObject.optJSONObject("angleResultInfo");
                if (jSONObjectOptJSONObject == null) {
                    Log.other("鱼塘❌收杆数据异常");
                    return null;
                }
                String strOptString = jSONObjectOptJSONObject.optString("fishName", "未知");
                String strOptString2 = jSONObjectOptJSONObject.optString("fishWeight", "0");
                jSONObjectOptJSONObject.optString("fishType", "");
                jSONObjectOptJSONObject.optString("fishAreaType", "");
                Log.other(String.format("鱼塘🐟收杆[%s]#%sg", strOptString, strOptString2));
                lastRodCount = jSONObject.optInt("rodSumCount", lastRodCount);
                return FishResult.success(lastRodCount, strOptString, strOptString2, str, jSONObjectOptJSONObject.optJSONObject("angleAdInfo"), true);
            } catch (Throwable th) {
                Log.other("鱼塘❌收杆异常[" + th.getMessage() + "]");
            }
        }
        return null;
    }

    private void startFishing(String str) {
        if (str == null || str.isEmpty() || "1".equals(str)) {
            Log.other("鱼塘🪝Token无效，跳过钓鱼");
            return;
        }
        Log.other("鱼塘🎣开始自动钓鱼");
        this.firstQueryDone = false;
        int i = 0;
        int i2 = 0;
        while (!Thread.currentThread().isInterrupted() && isAllowedTime()) {
            if (i >= 5 || i2 >= 50) {
                break;
            }
            if (!this.firstQueryDone) {
                queryFishStatus(false);
                this.firstQueryDone = true;
            } else {
                queryFishStatus(true);
            }
            // ⭐ 每次查询状态后检测并领取钓竿（TOMORROW_ROD等）
            tryClaimRodBeforeFish();
            sleep(ThreadLocalRandom.current().nextInt(1000) + 1000);
            if (lastRodCount <= 0) {
                // ⭐ 钓竿用完，最后再检测一次有没有可领取的钓竿
                Log.other("鱼塘🪝钓竿不足，检测可领取钓竿");
                tryClaimRodBeforeFish();
                if (lastRodCount <= 0) {
                    Log.other("鱼塘🪝钓竿不足，停止钓鱼");
                    break;
                }
                Log.other("鱼塘🪝领取到钓竿，继续钓鱼");
            }
            FishResult fishResultPerformSingleFish = performSingleFish(str);
            if (fishResultPerformSingleFish.isSuccess()) {
                i = 0;
                i2++;
                handleFishSuccess(fishResultPerformSingleFish);
            } else if (fishResultPerformSingleFish.isTooSmall()) {
                i++;
                if (i >= 5) {
                    Log.other("鱼塘🪝连续5次无效，Token已清空");
                    FishConfig.setFishpondToken("");
                    break;
                } else {
                    sleep(ThreadLocalRandom.current().nextInt(1000) + 4000);
                    sleep(ThreadLocalRandom.current().nextInt(5000) + 5000);
                }
            } else if (fishResultPerformSingleFish.isTokenInvalid()) {
                Log.other("鱼塘🪝Token失效，已清空");
                FishConfig.setFishpondToken("");
                break;
            } else if (fishResultPerformSingleFish.isNeedPositioning()) {
                i = 0;
                i2++;
                handleFishSuccess(fishResultPerformSingleFish);
            } else {
                i++;
                if (i >= 5) {
                    Log.other("鱼塘🪝连续5次失败，Token已清空");
                    FishConfig.setFishpondToken("");
                    break;
                } else {
                    sleep(ThreadLocalRandom.current().nextInt(1000) + 4000);
                    sleep(ThreadLocalRandom.current().nextInt(5000) + 5000);
                }
            }
            sleep(ThreadLocalRandom.current().nextInt(5000) + 5000);
        }
        Log.other("鱼塘✅钓鱼完成#共" + i2 + "次");
    }

    /**
     * 钓鱼前检测并领取可用的钓竿
     * 包括 TOMORROW_ROD（明日钓竿）等
     */
    private void tryClaimRodBeforeFish() {
        try {
            // 查询活动列表，检测 TOMORROW_ROD 是否可领取
            String result = requestString(API_QUERY_SUBPLOTS, buildBaseRequest(SCENE_GAME_CENTER));
            if (result != null && isSuccess(result)) {
                JSONObject json = new JSONObject(result);
                JSONArray activityList = json.optJSONArray("subplotsActivityList");
                if (activityList != null) {
                    for (int idx = 0; idx < activityList.length(); idx++) {
                        JSONObject activity = activityList.optJSONObject(idx);
                        if (activity == null) continue;
                        String activityType = activity.optString("activityType", "");
                        String status = activity.optString("status", "");
                        if ("TOMORROW_ROD".equals(activityType) && ("TODO".equals(status) || "TODAY_TODO".equals(status))) {
                            Log.other("鱼塘🌅钓鱼前领取明日钓竿");
                            triggerTomorrowRodAward();
                            // 领取后同步更新 rodCount
                            String syncResult = requestString(API_FISHPOND_SYNC_INDEX, buildRequestWithSyncType(SCENE_GAME_CENTER));
                            if (syncResult != null && isSuccess(syncResult)) {
                                JSONObject syncJson = new JSONObject(syncResult);
                                lastRodCount = syncJson.optInt("rodSumCount", lastRodCount);
                            }
                            return;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 静默忽略，不影响主流程
        }
    }

    private FishResult performSingleFish(String str) {
        FishResult fishResultPerformRodPositioning;
        if (str == null || str.isEmpty() || "1".equals(str)) {
            return FishResult.tokenInvalid();
        }
        try {
            JSONObject riskToken;
            try {
                riskToken = new JSONObject(str);
            } catch (Exception invalidToken) {
                return FishResult.tokenInvalid();
            }
            if (riskToken.length() == 0) return FishResult.tokenInvalid();
            JSONObject args = new JSONObject().put("bizNo", "").put("requestType", "NORMAL")
                    .put("riskToken", riskToken).put("sceneCode", SCENE_GAME_CENTER)
                    .put("source", SOURCE_FARM_POOL).put("version", "20260211.01");
            String strRequestString = requestString(API_FISHPOND_ANGLE, new JSONArray().put(args).toString());
            if (!isSuccess(strRequestString)) {
                if ("C21".equals(new JSONObject(strRequestString).optString("resultCode"))) {
                    return FishResult.tokenInvalid();
                }
                return FishResult.fail();
            }
            JSONObject jSONObject = new JSONObject(strRequestString);
            lastRodCount = jSONObject.optInt("rodSumCount", 0);
            boolean zOptBoolean = jSONObject.optBoolean("needRodPositioning", false);
            JSONObject jSONObjectOptJSONObject = jSONObject.optJSONObject("angleResultInfo");
            if (!zOptBoolean || jSONObjectOptJSONObject == null) {
                if (jSONObjectOptJSONObject == null) {
                    return FishResult.fail();
                }
                if (((float) jSONObjectOptJSONObject.optDouble("fishWeight", 0.0d)) > 0.01f) {
                    return FishResult.success(lastRodCount, jSONObjectOptJSONObject.optString("fishName", "未知"), jSONObjectOptJSONObject.optString("fishWeight", "0"), jSONObjectOptJSONObject.optString("bizNo", ""), jSONObjectOptJSONObject.optJSONObject("angleAdInfo"), false);
                }
                return FishResult.tooSmall();
            }
            float fOptDouble = (float) jSONObjectOptJSONObject.optDouble("fishWeight", 0.0d);
            String strOptString = jSONObjectOptJSONObject.optString("fishType", "");
            if (fOptDouble > 0.0f) {
                return FishResult.success(lastRodCount, jSONObjectOptJSONObject.optString("fishName", "未知"), jSONObjectOptJSONObject.optString("fishWeight", "0"), jSONObjectOptJSONObject.optString("bizNo", ""), jSONObjectOptJSONObject.optJSONObject("angleAdInfo"), false);
            }
            if (fOptDouble == 0.0f && "WELFARE_FISH".equals(strOptString)) {
                String strOptString2 = jSONObjectOptJSONObject.optString("bizNo", "");
                return (strOptString2.isEmpty() || (fishResultPerformRodPositioning = performRodPositioning(strOptString2, jSONObjectOptJSONObject.optString("areaType", "SPECIAL_BIG_ZONE"))) == null || !fishResultPerformRodPositioning.isSuccess()) ? FishResult.needPositioning(strOptString2) : fishResultPerformRodPositioning;
            }
            return FishResult.tooSmall();
        } catch (Throwable th) {
            Log.other("鱼塘🪝钓鱼异常[" + th.getClass().getSimpleName() + "]");
            return FishResult.fail();
        }
    }

    private void handleFishSuccess(FishResult fishResult) {
        JSONObject jSONObjectOptJSONObject;
        if (fishResult.isNeedPositioning() && fishResult.getBizNo() != null && !fishResult.getBizNo().isEmpty() && ((fishResult = performRodPositioning(fishResult.getBizNo(), "SPECIAL_BIG_ZONE")) == null || !fishResult.isSuccess())) {
            Log.other("鱼塘⚠️收杆失败，放弃");
            return;
        }
        if (fishResult.isSuccess()) {
            if (!fishResult.isConverted()) {
                Log.other(String.format("鱼塘🐟钓鱼成功[%s]#%sg", fishResult.getFishName(), fishResult.getFishWeight()));
            }
            String strRequestString = requestString(API_FISHPOND_SYNC_INDEX, buildRequestWithSyncType(SCENE_GAME_CENTER));
            if (isSuccess(strRequestString)) {
                try {
                    JSONObject jSONObject = new JSONObject(strRequestString);
                    int iOptInt = jSONObject.optInt("rodSumCount", 0);
                    JSONObject jSONObjectOptJSONObject2 = jSONObject.optJSONObject("roundInfo");
                    if (jSONObjectOptJSONObject2 != null && (jSONObjectOptJSONObject = jSONObjectOptJSONObject2.optJSONObject("fishAssetInfo")) != null) {
                        Log.other(String.format("鱼塘📊钓竿%d#鱼获%.2fg/%sg#还需%sg", Integer.valueOf(iOptInt), Double.valueOf(jSONObjectOptJSONObject.optDouble("currentFishWeight", 0.0d)), jSONObjectOptJSONObject.optString("targetFishWeight", "10000"), jSONObjectOptJSONObject.optString("diffFishWeight", "0")));
                    }
                } catch (Exception unused) {
                }
            }
            if (!this.todayRewardEnd) {
                String strRequestString2 = requestString(API_FISHPOND_SYNC_INDEX, buildRequestWithSyncType(SCENE_GAME_CENTER));
                try {
                    if (isSuccess(strRequestString2)) {
                        JSONObject jSONObjectOptJSONObject3 = new JSONObject(strRequestString2).optJSONObject("fishActivity");
                        if (jSONObjectOptJSONObject3 != null) {
                            int iOptInt2 = jSONObjectOptJSONObject3.optInt("leftFishTimes", -1);
                            if (iOptInt2 == 0 || iOptInt2 == -1) {
                                Log.other("鱼塘🎁领取钓鱼活动奖励");
                                triggerSubplotsActivity();
                            }
                        } else {
                            this.todayRewardEnd = true;
                            Log.other("鱼塘📊今日活动奖励已领完");
                        }
                    }
                } catch (Exception unused2) {
                }
            }
            if (fishResult.getAngleAdInfo() != null && !fishResult.getAngleAdInfo().optBoolean("complete", false)) {
                handleDoubleAdTask(fishResult.getAngleAdInfo());
            }
            if (fishTaskCount > 0) {
                fishTaskCount--;
                if (fishTaskCount == 0) {
                    sleep(ThreadLocalRandom.current().nextInt(1000) + 4000);
                    isSuccess(requestString(API_FINISH_TASK, fishTaskData));
                    sleep(1000L);
                    listTask();
                }
            }
        }
    }

    private void handleDoubleAdTask(JSONObject angleAdInfo) {
        try {
            if (angleAdInfo == null || angleAdInfo.optBoolean("complete", false)) {
                return;
            }

            String taskId = angleAdInfo.optString("taskId");
            String sceneCode = angleAdInfo.optString("sceneCode");
            String adBizNo = angleAdInfo.optString("adBizNo");

            if (taskId.isEmpty() || sceneCode.isEmpty()) {
                Log.record("鱼塘⚠️双倍广告信息不完整");
                return;
            }

            String taskKey = "double_ad_" + adBizNo;
            if (processedTasks.contains(taskKey)) {
                Log.record("鱼塘⚠️双倍广告已处理");
                return;
            }

            Log.other("鱼塘📺处理双倍奖励广告");

            // 根据抓包数据，优先使用schemaJson中的url
            String clickUrl = extractClickUrl(angleAdInfo);
            if (!clickUrl.isEmpty()) {
                Log.record("鱼塘🚀打开广告小程序");
                openMiniProgram(clickUrl);
                sleep(DELAY_LOOK_BASE);
                sleep(ThreadLocalRandom.current().nextInt(DELAY_FLOAT));
                Log.other("鱼塘✅广告浏览完成");
            } else {
                Log.record("鱼塘⚠️未找到广告链接");
                sleep(DELAY_MEDIUM);
            }

            // 根据抓包数据构造finishTask请求
            String outBizNo = taskId + "_" + System.currentTimeMillis() + "_" + ThreadLocalRandom.current().nextInt(10000);
            String requestData = String.format(
                "[{\"finishBusinessInfo\":{\"pwPreBizId\":\"%s\"}," +
                "\"outBizNo\":\"%s\",\"requestType\":\"RPC\"," +
                "\"sceneCode\":\"%s\",\"source\":\"ADBASICLIB\"," +
                "\"taskType\":\"%s\"}]",
                adBizNo.isEmpty() ? "" : adBizNo,
                outBizNo,
                sceneCode,
                taskId
            );

            String result = requestString(API_FINISH_TASK, requestData);

            if (isSuccess(result)) {
                JSONObject jSONObject = new JSONObject(result);
                JSONObject finishAwardResultVO = jSONObject.optJSONObject("finishAwardResultVO");
                if (finishAwardResultVO != null) {
                    int deltaAwardCount = finishAwardResultVO.optInt("deltaAwardCount", 0);
                    boolean hasNextStage = finishAwardResultVO.optBoolean("hasNextStage", false);
                    Log.other("鱼塘📺双倍广告完成" + (deltaAwardCount > 0 ? "#deltaAwardCount[" + deltaAwardCount + "]" : "") + (hasNextStage ? "#还有下一阶段" : ""));
                } else {
                    Log.other("鱼塘📺双倍广告完成");
                }

                processedTasks.add(taskKey);
                sleep(DELAY_MEDIUM);
                queryFishStatus(true);
            } else {
                Log.other("鱼塘❌双倍广告失败");
            }

        } catch (Throwable th) {
            Log.record("鱼塘❌双倍广告异常[" + th.getMessage() + "]");
        }
    }

    private String extractClickUrl(JSONObject angleAdInfo) {
        try {
            // 优先从schemaJson中提取URL（根据抓包数据）
            String schemaJson = angleAdInfo.optString("schemaJson");
            if (!schemaJson.isEmpty()) {
                JSONObject schema = new JSONObject(schemaJson);
                String url = schema.optString("url");
                if (!url.isEmpty()) {
                    return url;
                }
            }

            // 其次从clickThroughUrl获取
            String url = angleAdInfo.optString("clickThroughUrl");
            if (!url.isEmpty()) {
                return url;
            }

            // 最后尝试xlightDeepLinkUrl
            url = angleAdInfo.optString("xlightDeepLinkUrl");
            if (!url.isEmpty()) {
                return url;
            }

        } catch (Throwable unused) {
        }
        return "";
    }

    private void openMiniProgram(String url) {
        try {
            if (url == null || url.isEmpty()) {
                return;
            }

            Log.record("鱼塘📱打开小程序: " + (url.length() > 50 ? url.substring(0, 50) + "..." : url));

        } catch (Throwable th) {
            Log.record("鱼塘❌打开小程序失败[" + th.getMessage() + "]");
        }
    }

    private String extractPwPreBizIdFromUrl(String str) {
        try {
            int iIndexOf = str.indexOf("pwPreBizId=");
            if (iIndexOf == -1) {
                return "";
            }
            int i = iIndexOf + 11;
            int iIndexOf2 = str.indexOf("&", i);
            if (iIndexOf2 == -1) {
                iIndexOf2 = str.length();
            }
            return str.substring(i, iIndexOf2);
        } catch (Exception unused) {
            return "";
        }
    }

    private String extractBizIdFromUrl(String str) {
        try {
            int iIndexOf = str.indexOf("bizId=");
            if (iIndexOf == -1) {
                return "";
            }
            int i = iIndexOf + 6;
            int iIndexOf2 = str.indexOf("&", i);
            if (iIndexOf2 == -1) {
                iIndexOf2 = str.length();
            }
            return str.substring(i, iIndexOf2);
        } catch (Exception unused) {
            return "";
        }
    }

    private boolean isBeforeFourAM() {
        return TimeUtil.getNow().get(11) < 4;
    }

    private static class FishResult {
        private static final int TYPE_FAIL = 2;
        private static final int TYPE_NEED_POSITIONING = 6;
        private static final int TYPE_SUCCESS = 1;
        private static final int TYPE_TOKEN_INVALID = 4;
        private static final int TYPE_TOO_SMALL = 3;
        private static final int TYPE_WELFARE = 5;
        private final JSONObject angleAdInfo;
        private final String bizNo;
        private final boolean converted;
        private final String fishName;
        private final String fishWeight;
        private final boolean needPositioning;
        private final int rodCount;
        private final int type;

        private FishResult(int i, boolean z, int i2, String str, String str2, String str3, JSONObject jSONObject, boolean z2) {
            this.type = i;
            this.needPositioning = z;
            this.rodCount = i2;
            this.fishName = str;
            this.fishWeight = str2;
            this.bizNo = str3;
            this.angleAdInfo = jSONObject;
            this.converted = z2;
        }

        static FishResult success(int i, String str, String str2, String str3, JSONObject jSONObject, boolean z) {
            return new FishResult(1, false, i, str, str2, str3, jSONObject, z);
        }

        static FishResult fail() {
            return new FishResult(2, false, 0, "", "", "", null, false);
        }

        static FishResult tokenInvalid() {
            return new FishResult(4, false, 0, "", "", "", null, false);
        }

        static FishResult tooSmall() {
            return new FishResult(3, false, 0, "", "", "", null, false);
        }

        static FishResult welfareFish(String str) {
            return new FishResult(5, true, 0, "福利鱼", "0", str, null, false);
        }

        static FishResult needPositioning(String str) {
            return new FishResult(6, true, 0, "", "0", str, null, false);
        }

        boolean isSuccess() {
            return this.type == 1;
        }

        boolean isTokenInvalid() {
            return this.type == 4;
        }

        boolean isTooSmall() {
            return this.type == 3;
        }

        boolean isWelfareFish() {
            return this.type == 5;
        }

        boolean isNeedPositioning() {
            int i;
            return this.needPositioning || (i = this.type) == 5 || i == 6;
        }

        boolean isConverted() {
            return this.converted;
        }

        int getRodCount() {
            return this.rodCount;
        }

        String getFishName() {
            return this.fishName;
        }

        String getFishWeight() {
            return this.fishWeight;
        }

        String getBizNo() {
            return this.bizNo;
        }

        JSONObject getAngleAdInfo() {
            return this.angleAdInfo;
        }
    }

    /**
     * 将奖励类型英文转中文
     */
    private static String toAwardChineseName(String awardType) {
        switch (awardType) {
            case "FISHROD": return "钓竿";
            case "FISHINGBAIT": return "鱼饵";
            case "GOLD": return "金币";
            case "DIAMOND": return "钻石";
            case "FISH": return "鱼苗";
            default: return awardType;
        }
    }

    /**
     * 将状态英文转中文
     */
    private static String toStatusChineseName(String status) {
        switch (status) {
            case "FINISHED": return "已完成";
            case "TODO": return "待完成";
            case "TODAY_FINISH": return "今日已完成";
            case "TODAY_TODO": return "今日待完成";
            default: return status;
        }
    }
}
