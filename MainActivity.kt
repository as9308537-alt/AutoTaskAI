package com.example.autotaskai

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.widget.*
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var command: EditText
    private lateinit var workflowName: EditText
    private lateinit var replacement: EditText
    private lateinit var status: TextView
    private lateinit var workflows: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 28, 24, 24) }
        root.addView(TextView(this).apply { text = "AutoTask AI"; textSize = 30f })
        root.addView(TextView(this).apply { text = "Teach once → run again"; textSize = 16f })

        command = EditText(this).apply { hint = "Task bolo / type karo"; minLines = 2 }
        root.addView(command)
        root.addView(Button(this).apply { text = "🎤 Voice Command"; setOnClickListener { speak() } })

        workflowName = EditText(this).apply { hint = "Training name (e.g. Amazon Listing)" }
        root.addView(workflowName)
        root.addView(Button(this).apply { text = "🎓 START TRAINING"; setOnClickListener { startTraining() } })
        root.addView(Button(this).apply { text = "⏹ STOP + SAVE TRAINING"; setOnClickListener { stopTraining() } })

        replacement = EditText(this).apply { hint = "Variables: old=new, old2=new2 (optional)" }
        root.addView(replacement)
        root.addView(Button(this).apply { text = "▶ RUN SELECTED / COMMAND"; setOnClickListener { runRequested() } })
        root.addView(Button(this).apply { text = "⚙ Accessibility Permission"; setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) } })

        status = TextView(this).apply { text = "Enable Accessibility first, then train a workflow."; textSize = 15f; setPadding(0, 12, 0, 12) }
        root.addView(status)
        workflows = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(ScrollView(this).apply { addView(workflows) }, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        refresh()
    }

    private fun startTraining() {
        if (workflowName.text.toString().trim().isEmpty()) { toast("Training name likho"); return }
        AutomationAccessibilityService.startRecording()
        status.text = "TRAINING ON: ab target app me normal kaam karo. Wapas aakar STOP + SAVE dabao."
    }

    private fun stopTraining() {
        val name = workflowName.text.toString().trim()
        val steps = AutomationAccessibilityService.stopRecording()
        if (name.isEmpty() || steps.isEmpty()) { toast("Koi action record nahi hua"); return }
        WorkflowStore.save(this, name, steps)
        status.text = "$name saved: ${steps.size} actions"
        refresh()
    }

    private fun runRequested() {
        val text = command.text.toString().trim()
        val names = WorkflowStore.names(this)
        val name = names.firstOrNull { text.equals("run $it", true) || text.equals(it, true) } ?: names.firstOrNull { it.equals(workflowName.text.toString().trim(), true) }
        if (name == null) { toast("Pehle workflow save karo"); return }
        val map = parseReplacements(replacement.text.toString())
        AutomationAccessibilityService.instance?.run(WorkflowStore.load(this, name), map) ?: toast("Accessibility Service ON nahi hai")
        status.text = "Running: $name"
    }

    private fun parseReplacements(s: String): Map<String, String> = s.split(",").mapNotNull { part -> val x = part.split("=", limit = 2); if (x.size == 2 && x[0].isNotBlank()) x[0].trim() to x[1].trim() else null }.toMap()

    private fun refresh() {
        workflows.removeAllViews()
        WorkflowStore.names(this).forEach { name ->
            workflows.addView(Button(this).apply { text = "▶ $name"; setOnClickListener { workflowName.setText(name); runRequested() } })
        }
    }

    private fun speak() {
        try {
            startActivityForResult(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply { putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM); putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault()) }, 100)
        } catch (_: Exception) { toast("Voice recognition unavailable") }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) { super.onActivityResult(requestCode, resultCode, data); if (requestCode == 100 && resultCode == RESULT_OK) command.setText(data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull() ?: "") }
    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
}
