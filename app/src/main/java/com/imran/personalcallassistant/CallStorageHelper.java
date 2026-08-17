package com.imran.personalcallassistant;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class CallStorageHelper {
    private static final String PREF_NAME = "call_assistant_records";
    private static final String KEY_RECORDS = "records_json";

    public static synchronized void saveRecord(Context context, CallRecord record) {
        List<CallRecord> records = getRecords(context);
        records.add(0, record);

        JSONArray array = new JSONArray();
        for (CallRecord r : records) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("phoneNumber", r.getPhoneNumber());
                obj.put("callerName", r.getCallerName());
                obj.put("reason", r.getReason());
                obj.put("language", r.getLanguage());
                obj.put("timestamp", r.getTimestamp());
                array.put(obj);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_RECORDS, array.toString()).apply();
    }

    public static List<CallRecord> getRecords(Context context) {
        List<CallRecord> list = new ArrayList<>();
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_RECORDS, "[]");

        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                list.add(new CallRecord(
                        obj.optString("phoneNumber"),
                        obj.optString("callerName"),
                        obj.optString("reason"),
                        obj.optString("language"),
                        obj.optLong("timestamp")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
}

