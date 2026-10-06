package com.surexu.sesame.model.task.antFarm;

import java.util.Locale;
import org.json.JSONObject;

/** GR 中替代 SO 的庄园任务筛选规则。 */
public class FarmTaskPolicy {
    public static boolean canRun(JSONObject task) {
        if (task == null) return false;
        String key = task.optString("bizKey");
        String id = task.optString("taskId");
        String mode = task.optString("taskMode");
        String title = task.optString("title");
        String desc = task.optString("desc");
        if (key.isEmpty() || !("VIEW".equals(mode) || "COUNT_DOWN".equals(mode)
                || ("TRIGGER".equals(mode) && !id.isEmpty() && id.equals(key)))) return false;
        if ("ONLINE_PAY".equals(key) || "OFFLINE_PAY".equals(key)
                || key.startsWith("HEART_DONAT") || "BABAFARM_TB".equals(key)
                || "_chouchoulechoukuan".equals(key) || "TAO_GOLDEN_V2".equals(key)) return false;
        if (title.contains("付款") || title.contains("买") || desc.contains("付款")
                || (desc.contains("付") && desc.contains("元"))
                || (desc.contains("捐") && (desc.contains("元") || desc.contains("金额")))) return false;
        return !key.toLowerCase(Locale.ROOT).contains("xiadan") || "LSHS_xiadan_202509".equals(key);
    }
}
