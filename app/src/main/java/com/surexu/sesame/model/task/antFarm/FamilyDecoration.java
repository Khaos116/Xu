package com.surexu.sesame.model.task.antFarm;

import com.surexu.sesame.util.Log;
import com.surexu.sesame.util.idMap.UserIdMap;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashSet;
import java.util.Set;

/** 家庭装修金兑换流程，移植自 GR 的 AntFarmFamily。 */
public class FamilyDecoration {
    public static void run(JSONObject family) {
        if (family == null) return;
        String activityId = family.optString("decorationCoinActivityId");
        String uid = UserIdMap.getCurrentUid();
        if (activityId.isEmpty() || uid == null || uid.isEmpty()) return;
        String[] labels = {"", "recentlyAdded", "sofa", "seat2", "seat4", "seat5", "seat3",
                "curtain", "table", "carpet", "mattress", "bed3", "bed4", "bed5", "ceiling",
                "windowView", "firstFloor", "firstWall", "secondFloor", "secondWall",
                "leftWallDecoration", "rightWallDecoration", "treadmill", "slide"};
        Set<String> attempted = new HashSet<>();
        try {
            for (String label : labels) {
                int start = 0;
                // ponytail: 每类最多 100 页；商城超过 1000 件时再调整上限。
                for (int page = 0; page < 100; page++) {
                    if (Thread.currentThread().isInterrupted() || !uid.equals(UserIdMap.getCurrentUid())) return;
                    String response = AntFarmRpcCall.getFitmentItemList(activityId, label, start);
                    if (response == null || response.isEmpty()) return;
                    JSONObject list = new JSONObject(response);
                    if (!isSuccess(list)) return;
                    JSONObject account = list.optJSONObject("mallAccountInfoVO");
                    JSONObject holding = account == null ? null : account.optJSONObject("holdingCount");
                    long balance = holding == null ? 0 : holding.optLong("cent", 0);
                    JSONArray items = list.optJSONArray("itemInfoVOList");
                    if (items == null || items.length() == 0) break;
                    for (int i = 0; i < items.length(); i++) {
                        JSONObject item = items.optJSONObject(i);
                        if (item == null) continue;
                        JSONObject priceInfo = item.optJSONObject("minPrice");
                        long price = priceInfo == null ? -1 : priceInfo.optLong("cent", -1);
                        JSONArray statuses = item.optJSONArray("itemStatusList");
                        JSONArray skus = item.optJSONArray("skuModelList");
                        String spuId = item.optString("spuId");
                        if (price < 0 || price > balance || spuId.isEmpty() || attempted.contains(spuId)
                                || (statuses != null && statuses.length() > 0) || skus == null || skus.length() == 0) continue;
                        JSONObject sku = skus.optJSONObject(0);
                        String skuId = sku == null ? "" : sku.optString("skuId");
                        if (skuId.isEmpty()) continue;
                        if (Thread.currentThread().isInterrupted() || !uid.equals(UserIdMap.getCurrentUid())) return;
                        attempted.add(spuId);
                        String result = AntFarmRpcCall.exchangeFamilyDecoration(spuId, skuId, activityId);
                        if (result == null || result.isEmpty() || !isSuccess(new JSONObject(result))) return;
                        balance -= price;
                        Log.farm("家庭装饰🏠兑换[" + item.optString("spuName", spuId) + "]#消耗装修金 " + price / 100.0);
                        Thread.sleep(2000);
                    }
                    int next = list.optInt("nextStartIndex", 0);
                    if (!list.optBoolean("hasMore") || next <= start) break;
                    start = next;
                    Thread.sleep(1000);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            Log.printStackTrace("FamilyDecoration", e);
        }
    }

    private static boolean isSuccess(JSONObject result) {
        if (result.has("success")) return result.optBoolean("success");
        return "100".equals(result.optString("resultCode"))
                || "SUCCESS".equals(result.optString("resultCode"))
                || "SUCCESS".equals(result.optString("memo"));
    }
}
