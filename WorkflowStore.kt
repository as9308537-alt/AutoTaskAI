package com.example.autotaskai

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object WorkflowStore {
    private const val PREFS = "autotask"
    private const val KEY_WORKFLOWS = "workflows"

    data class Step(
        val type: String,
        val packageName: String = "",
        val viewId: String = "",
        val text: String = "",
        val contentDescription: String = "",
        val className: String = "",
        val value: String = "",
        val direction: Int = 0
    )

    fun save(context: Context, name: String, steps: List<Step>) {
        val all = loadRaw(context)
        val arr = JSONArray()
        for (i in 0 until all.length()) {
            val o = all.getJSONObject(i)
            if (!o.optString("name").equals(name, true)) arr.put(o)
        }
        val wrapper = JSONObject()
        wrapper.put("name", name)
        val sa = JSONArray()
        steps.forEach { s ->
            sa.put(JSONObject().apply {
                put("type", s.type); put("packageName", s.packageName); put("viewId", s.viewId)
                put("text", s.text); put("contentDescription", s.contentDescription)
                put("className", s.className); put("value", s.value); put("direction", s.direction)
            })
        }
        wrapper.put("steps", sa)
        arr.put(wrapper)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_WORKFLOWS, arr.toString()).apply()
    }

    fun names(context: Context): List<String> {
        val arr = loadRaw(context); val out = mutableListOf<String>()
        for (i in 0 until arr.length()) out.add(arr.getJSONObject(i).optString("name"))
        return out
    }

    fun load(context: Context, name: String): List<Step> {
        val arr = loadRaw(context)
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            if (o.optString("name").equals(name, true)) {
                val sa = o.optJSONArray("steps") ?: JSONArray(); val out = mutableListOf<Step>()
                for (j in 0 until sa.length()) {
                    val s = sa.getJSONObject(j)
                    out.add(Step(s.optString("type"), s.optString("packageName"), s.optString("viewId"), s.optString("text"), s.optString("contentDescription"), s.optString("className"), s.optString("value"), s.optInt("direction")))
                }
                return out
            }
        }
        return emptyList()
    }

    private fun loadRaw(context: Context): JSONArray = JSONArray(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_WORKFLOWS, "[]"))
}
