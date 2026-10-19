package com.example.smartcaptain

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

class AutoAcceptService : AccessibilityService() {

    // Allowed driver apps (prevents accidental clicks on phone calls or other apps)
    private val allowedPackages = setOf(
        "com.rapido.captain",
        "com.rapido.rider",
        "com.ubercab.driver",
        "com.theporter.android.driverapp"
    )

    private var lastActionTime = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val pkgName = event.packageName?.toString() ?: return
        if (!allowedPackages.contains(pkgName)) return

        val prefs = getSharedPreferences("SmartCaptainPrefs", Context.MODE_PRIVATE)

        // 1. Check if Engine is turned ON in Status / Checks tab
        val isEngineActive = prefs.getBoolean("engine_active", false)
        if (!isEngineActive) return

        // Cooldown of 2 seconds to avoid double-counting the same screen update
        val now = System.currentTimeMillis()
        if (now - lastActionTime < 2000) return

        val rootNode = rootInActiveWindow ?: return
        val screenTexts = mutableListOf<String>()
        collectScreenText(rootNode, screenTexts)
        val fullText = screenTexts.joinToString(" ")

        // Check if an Accept button is actually visible on screen
        val acceptNode = findAcceptButton(rootNode) ?: return

        // 2. Load saved Rules from RulesFragment
        val maxPickupKm = prefs.getInt("max_pickup_km", 3)
        val maxTripKm = prefs.getInt("max_trip_km", 50)
        val minFare = prefs.getInt("min_fare", 0)

        // 3. Parse Fare and Distance from the booking card text
        val detectedFare = extractFare(fullText)
        val distances = extractDistances(fullText)
        val pickupDist = distances.firstOrNull() ?: 0.0
        val tripDist = if (distances.size > 1) distances[1] else 0.0

        // 4. Evaluate rules
        var shouldAccept = true
        var reason = "Matched all rules"

        if (minFare > 0 && detectedFare > 0 && detectedFare < minFare) {
            shouldAccept = false
            reason = "Fare ₹$detectedFare is below minimum ₹$minFare"
        } else if (pickupDist > maxPickupKm) {
            shouldAccept = false
            reason = "Pickup ${pickupDist}km exceeds ${maxPickupKm}km limit"
        } else if (tripDist > maxTripKm) {
            shouldAccept = false
            reason = "Trip ${tripDist}km exceeds ${maxTripKm}km limit"
        }

        lastActionTime = now
        val timeStamp = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())

        // 5. Click Accept or leave it, and update Status & Activity tabs
        if (shouldAccept) {
            acceptNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            acceptNode.parent?.performAction(AccessibilityNodeInfo.ACTION_CLICK)

            val accepted = prefs.getInt("accepted_count", 0) + 1
            val oldLog = prefs.getString("activity_log", "") ?: ""
            val newEntry = "[$timeStamp] ACCEPTED (₹$detectedFare | ${pickupDist}km) - $reason"
            val updatedLog = if (oldLog.startsWith("No bookings")) newEntry else "$newEntry\n\n$oldLog"

            prefs.edit()
                .putInt("accepted_count", accepted)
                .putString("activity_log", updatedLog)
                .apply()
        } else {
            val declined = prefs.getInt("declined_count", 0) + 1
            val oldLog = prefs.getString("activity_log", "") ?: ""
            val newEntry = "[$timeStamp] SKIPPED (₹$detectedFare | ${pickupDist}km) - $reason"
            val updatedLog = if (oldLog.startsWith("No bookings")) newEntry else "$newEntry\n\n$oldLog"

            prefs.edit()
                .putInt("declined_count", declined)
                .putString("activity_log", updatedLog)
                .apply()
        }
    }

    private fun collectScreenText(node: AccessibilityNodeInfo?, list: MutableList<String>) {
        if (node == null) return
        node.text?.toString()?.trim()?.let { if (it.isNotEmpty()) list.add(it) }
        node.contentDescription?.toString()?.trim()?.let { if (it.isNotEmpty()) list.add(it) }
        for (i in 0 until node.childCount) {
            collectScreenText(node.getChild(i), list)
        }
    }

    private fun findAcceptButton(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        val text = (node.text?.toString() ?: "").lowercase()
        val desc = (node.contentDescription?.toString() ?: "").lowercase()

        if (text == "accept" || text.contains("accept") || desc.contains("accept")) {
            return node
        }
        for (i in 0 until node.childCount) {
            val found = findAcceptButton(node.getChild(i))
            if (found != null) return found
        }
        return null
    }

    private fun extractFare(text: String): Int {
        val matcher = Pattern.compile("(?:₹|Rs\\.?|INR)\\s*(\\d+)").matcher(text)
        return if (matcher.find()) matcher.group(1)?.toIntOrNull() ?: 0 else 0
    }

    private fun extractDistances(text: String): List<Double> {
        val list = mutableListOf<Double>()
        val matcher = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:km|KM|Km)").matcher(text)
        while (matcher.find()) {
            matcher.group(1)?.toDoubleOrNull()?.let { list.add(it) }
        }
        return list
    }

    override fun onInterrupt() {}
}
