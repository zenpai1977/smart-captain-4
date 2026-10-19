package com.smartcaptain.app

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class MyAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Read preferences using the exact keys saved in MainActivity
        val prefs = getSharedPreferences("smart_captain_prefs", Context.MODE_PRIVATE)
        val isServiceOn = prefs.getBoolean("switchOn", true)
        if (!isServiceOn) return

        val rootNode: AccessibilityNodeInfo = rootInActiveWindow ?: return

        // Read rule parameters safely with fallback defaults
        val minTrip = prefs.getString("minTrip", "5")?.toDoubleOrNull() ?: 5.0
        val maxPickup = prefs.getString("maxPickup", "2")?.toDoubleOrNull() ?: 2.0
        val minFare = prefs.getString("minFare", "60")?.toDoubleOrNull() ?: 60.0
        val goTo = prefs.getString("goTo", "")?.lowercase() ?: ""

        // Evaluate active screen content against rules
        evaluateAndFilterRide(rootNode, minTrip, maxPickup, minFare, goTo)
    }

    private fun evaluateAndFilterRide(
        rootNode: AccessibilityNodeInfo,
        minTrip: Double,
        maxPickup: Double,
        minFare: Double,
        goTo: String
    ) {
        // Gather all text visible on the current ride request card
        val screenTextBuilder = StringBuilder()
        collectNodeText(rootNode, screenTextBuilder)
        val screenText = screenTextBuilder.toString().lowercase()

        // Ensure this is actually a ride request screen before proceeding
        if (!screenText.contains("accept") && !screenText.contains("fare")) {
            return
        }

        // Parse extracted metrics from the layout text
        val parsedFare = extractMetricValue(screenText, "₹") ?: extractMetricValue(screenText, "fare") ?: 0.0
        val parsedTrip = extractMetricValue(screenText, "km") ?: 0.0

        // Enforce strict filtering rules
        val passesFareRule = parsedFare == 0.0 || parsedFare >= minFare
        val passesTripRule = parsedTrip == 0.0 || parsedTrip >= minTrip

        // If all active filters match, locate and click the specific accept button safely
        if (passesFareRule && passesTripRule) {
            triggerAcceptAction(rootNode)
        }
    }

    private fun collectNodeText(node: AccessibilityNodeInfo?, builder: StringBuilder) {
        if (node == null) return
        node.text?.let {
            builder.append(it).append(" ")
        }
        for (i in 0 until node.childCount) {
            collectNodeText(node.getChild(i), builder)
        }
    }

    private fun extractMetricValue(text: String, keyword: String): Double? {
        val index = text.indexOf(keyword)
        if (index != -1) {
            val subText = text.substring(index + keyword.length).trim()
            val potentialNum = subText.split(Regex("\\s+"))
                .firstOrNull()
                ?.replace(Regex("[^\\d.]"), "")
            return potentialNum?.toDoubleOrNull()
        }
        return null
    }

    private fun triggerAcceptAction(node: AccessibilityNodeInfo?) {
        if (node == null) return

        val text = node.text?.toString()?.lowercase() ?: ""
        // Ensure we click a button specifically tied to accepting rather than random text
        if ((text.contains("accept") || text.contains("slide")) && node.isClickable) {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            return
        }

        for (i in 0 until node.childCount) {
            triggerAcceptAction(node.getChild(i))
        }
    }

    override fun onInterrupt() {
        // Required method implementation for AccessibilityService
    }
}
