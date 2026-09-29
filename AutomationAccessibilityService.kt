package com.example.autotaskai

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast

class AutomationAccessibilityService : AccessibilityService() {
    companion object {
        @Volatile var instance: AutomationAccessibilityService? = null
        @Volatile var recording = false
        private val recorded = mutableListOf<WorkflowStore.Step>()
        private var lastSignature = ""
        private var lastTime = 0L

        fun startRecording() { synchronized(recorded) { recorded.clear(); lastSignature = "" }; recording = true }
        fun stopRecording(): List<WorkflowStore.Step> { recording = false; return synchronized(recorded) { recorded.toList() } }
    }

    override fun onServiceConnected() { super.onServiceConnected(); instance = this }
    override fun onDestroy() { instance = null; super.onDestroy() }
    override fun onInterrupt() {}

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!recording || event == null) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return
        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_CLICKED -> recordNode("CLICK", event.source)
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                val n = event.source ?: return
                if (n.isPassword) return
                recordNode("TEXT", n, event.text.joinToString(""))
            }
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                val delta = if (android.os.Build.VERSION.SDK_INT >= 28) event.scrollDeltaY else 0f
                val direction = if (delta > 0 || event.fromIndex < event.toIndex) 1 else -1
                add(WorkflowStore.Step("SCROLL", packageName = pkg, direction = direction))
            }
        }
    }

    private fun recordNode(type: String, node: AccessibilityNodeInfo?, changedText: String = "") {
        node ?: return
        val value = if (type == "TEXT") changedText else ""
        val sig = "$type|${node.viewIdResourceName}|${node.text}|${node.contentDescription}|$value"
        val now = System.currentTimeMillis()
        if (sig == lastSignature && now - lastTime < 500) return
        lastSignature = sig; lastTime = now
        add(WorkflowStore.Step(type, node.packageName?.toString() ?: "", node.viewIdResourceName ?: "", node.text?.toString() ?: "", node.contentDescription?.toString() ?: "", node.className?.toString() ?: "", value))
    }

    private fun add(step: WorkflowStore.Step) { synchronized(recorded) { if (recorded.size < 500) recorded.add(step) } }

    fun run(steps: List<WorkflowStore.Step>, replacements: Map<String, String> = emptyMap()) {
        Thread {
            for (step in steps) {
                perform(step, replacements)
                Thread.sleep(350)
            }
            Handler(Looper.getMainLooper()).post { Toast.makeText(this, "Automation finished", Toast.LENGTH_SHORT).show() }
        }.start()
    }

    private fun replace(s: String, r: Map<String, String>) = r.entries.fold(s) { acc, e -> acc.replace(e.key, e.value, true) }

    private fun perform(step: WorkflowStore.Step, r: Map<String, String>) {
        when (step.type) {
            "TEXT" -> {
                val node = find(step, r) ?: return
                if (!node.isPassword) {
                    val b = Bundle(); b.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, replace(step.value, r)); node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, b)
                }
            }
            "CLICK" -> find(step, r)?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            "SCROLL" -> find(step, r)?.performAction(if (step.direction >= 0) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
        }
    }

    private fun find(step: WorkflowStore.Step, r: Map<String, String>): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        if (step.viewId.isNotBlank()) try { root.findAccessibilityNodeInfosByViewId(step.viewId).firstOrNull()?.let { return it } } catch (_: Exception) {}
        val text = replace(step.text, r)
        if (text.isNotBlank()) root.findAccessibilityNodeInfosByText(text).firstOrNull()?.let { return it }
        val desc = replace(step.contentDescription, r)
        if (desc.isNotBlank()) root.findAccessibilityNodeInfosByText(desc).firstOrNull()?.let { return it }
        return findByClass(root, step.className)
    }

    private fun findByClass(node: AccessibilityNodeInfo, cls: String): AccessibilityNodeInfo? {
        if (cls.isNotBlank() && node.className?.toString() == cls) return node
        for (i in 0 until node.childCount) node.getChild(i)?.let { findByClass(it, cls)?.let { x -> return x } }
        return null
    }
}
