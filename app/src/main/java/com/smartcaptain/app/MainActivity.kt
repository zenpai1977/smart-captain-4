package com.smartcaptain.app

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private val upiId = "kkumae221@okhdfcbank"
    private val backendBaseUrl = "https://smart-captain.onrender.com"

    private val planNames = arrayOf(
        "1 Day Pass — ₹9 (Unlimited Rides)",
        "7 Days Pass — ₹49 (Unlimited Rides)",
        "30 Days Pass — ₹149 (Unlimited Rides)"
    )
    private val planAmounts = arrayOf(9, 49, 149)
    private val planShortLabels = arrayOf("1 Day Pass (₹9)", "7 Days Pass (₹49)", "30 Days Pass (₹149)")

    private var selectedAmount = 9
    private var selectedPlanLabel = planShortLabels[0]

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        seedDefaultAdminEntriesIfNeeded()
        applyExactWebStyling()
        setupTabs()
        setupPlanSpinner()
        setupButtons()
        loadSavedRulesIntoUi()
        refreshPassStatus()
    }

    private fun applyExactWebStyling() {
        val cardBorder = { bgColor: String, strokeColor: String ->
            GradientDrawable().apply {
                setColor(Color.parseColor(bgColor))
                setStroke(2, Color.parseColor(strokeColor))
                cornerRadius = 18f
            }
        }

        findViewById<View>(R.id.cardTopBanner).background = cardBorder("#12161F", "#6E5A28")
        findViewById<View>(R.id.cardRules).background = cardBorder("#12161F", "#6E5A28")
        findViewById<View>(R.id.cardActivation).background = cardBorder("#12161F", "#6E5A28")
        findViewById<View>(R.id.qrContainer).background = cardBorder("#0B0E14", "#21262D")

        val inputBoxIds = listOf(
            R.id.inputMinTrip, R.id.inputMaxPickup, R.id.inputMinFare,
            R.id.inputPhone, R.id.inputUtr, R.id.spinnerPlan
        )
        for (id in inputBoxIds) {
            findViewById<View>(id).background = cardBorder("#0D1117", "#30363D")
        }

        findViewById<View>(R.id.inputGoTo).background = cardBorder("#0D1117", "#238636")
    }

    private fun setupTabs() {
        val btnDriver = findViewById<Button>(R.id.btnTabDriver)
        val btnAdmin = findViewById<Button>(R.id.btnTabAdmin)
        val driverView = findViewById<View>(R.id.driverDashboardContainer)
        val adminView = findViewById<View>(R.id.adminPanelContainer)

        btnDriver.setOnClickListener {
            driverView.visibility = View.VISIBLE
            adminView.visibility = View.GONE
            btnDriver.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#1F6FEB"))
            btnAdmin.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#21262D"))
            refreshPassStatus()
        }

        btnAdmin.setOnClickListener {
            driverView.visibility = View.GONE
            adminView.visibility = View.VISIBLE
            btnAdmin.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#1F6FEB"))
            btnDriver.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#21262D"))
            renderAdminTable()
        }

        findViewById<Button>(R.id.btnRefreshAdmin).setOnClickListener {
            renderAdminTable()
            Toast.makeText(this, "Admin list refreshed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupPlanSpinner() {
        val spinner = findViewById<Spinner>(R.id.spinnerPlan)
        val btnPay = findViewById<Button>(R.id.btnPayGpay)

        val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, planNames) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val tv = super.getView(position, convertView, parent) as TextView
                tv.setTextColor(Color.WHITE)
                tv.textSize = 13f
                return tv
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val tv = super.getDropDownView(position, convertView, parent) as TextView
                tv.setTextColor(Color.WHITE)
                tv.setBackgroundColor(Color.parseColor("#161B22"))
                tv.setPadding(24, 24, 24, 24)
                return tv
            }
        }
        spinner.adapter = adapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedAmount = planAmounts[position]
                selectedPlanLabel = planShortLabels[position]
                btnPay.text = "Tap to Pay ₹$selectedAmount Directly on GPay / UPI App"
                loadQrCodeForAmount(selectedAmount)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadQrCodeForAmount(amount: Int) {
        val imgQr = findViewById<ImageView>(R.id.imgQrCode)
        val upiUri = "upi://pay?pa=$upiId&pn=Smart%20Captain&am=$amount&cu=INR"
        val qrApiUrl = "https://api.qrserver.com/v1/create-qr-code/?size=350x350&data=" +
                URLEncoder.encode(upiUri, "UTF-8")

        thread {
            try {
                val stream = URL(qrApiUrl).openStream()
                val bitmap = BitmapFactory.decodeStream(stream)
                runOnUiThread {
                    imgQr.setImageBitmap(bitmap)
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun setupButtons() {
        val btnPay = findViewById<Button>(R.id.btnPayGpay)
        val btnSave = findViewById<Button>(R.id.btnSaveRules)
        val btnStatus = findViewById<Button>(R.id.btnCheckStatus)

        btnPay.setOnClickListener {
            val uri = Uri.parse("upi://pay").buildUpon()
                .appendQueryParameter("pa", upiId)
                .appendQueryParameter("pn", "Smart Captain")
                .appendQueryParameter("am", selectedAmount.toString())
                .appendQueryParameter("cu", "INR")
                .build()

            val intent = Intent(Intent.ACTION_VIEW, uri)
            try {
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "No UPI app found. Please scan the QR code or pay to $upiId", Toast.LENGTH_LONG).show()
            }
        }

        btnSave.setOnClickListener {
            val phone = findViewById<EditText>(R.id.inputPhone).text.toString().trim()
            val utr = findViewById<EditText>(R.id.inputUtr).text.toString().trim()
            val minTrip = findViewById<EditText>(R.id.inputMinTrip).text.toString().trim()
            val maxPickup = findViewById<EditText>(R.id.inputMaxPickup).text.toString().trim()
            val minFare = findViewById<EditText>(R.id.inputMinFare).text.toString().trim()
            val goTo = findViewById<EditText>(R.id.inputGoTo).text.toString().trim()
            val switchOn = findViewById<CheckBox>(R.id.switchService).isChecked

            if (phone.length != 10) {
                Toast.makeText(this, "Please enter a valid 10-digit mobile number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (utr.length != 12) {
                Toast.makeText(this, "Please enter a valid 12-digit UTR number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val prefs = getSharedPreferences("smart_captain_prefs", Context.MODE_PRIVATE)
            prefs.edit()
                .putBoolean("switchOn", switchOn)
                .putString("minTrip", minTrip)
                .putString("maxPickup", maxPickup)
                .putString("minFare", minFare)
                .putString("goTo", goTo)
                .putString("phone", phone)
                .apply()

            saveSubmissionRecord(phone, selectedPlanLabel, utr)
            syncWithCloudBackend(phone, selectedPlanLabel, utr, minTrip, maxPickup, minFare, goTo)

            val txtStatus = findViewById<TextView>(R.id.txtCloudStatus)
            txtStatus.text = "Rules Saved! UTR $utr submitted (Pending Admin Approval)"
            txtStatus.setTextColor(Color.parseColor("#D29922"))

            Toast.makeText(this, "Submitted! Approve it in the Admin Panel tab.", Toast.LENGTH_LONG).show()
        }

        btnStatus.setOnClickListener {
            refreshPassStatus()
        }
    }

    private fun refreshPassStatus() {
        val phone = findViewById<EditText>(R.id.inputPhone).text.toString().trim()
        val txtStatus = findViewById<TextView>(R.id.txtCloudStatus)
        txtStatus.text = "Checking cloud status..."
        txtStatus.setTextColor(Color.parseColor("#58A6FF"))

        val records = getSubmissionsArray()
        var foundStatus: String? = null
        var foundPlan: String? = null

        for (i in 0 until records.length()) {
            val item = records.getJSONObject(i)
            if (item.getString("phone") == phone) {
                foundStatus = item.getString("status")
                foundPlan = item.getString("plan")
            }
        }

        if (foundStatus == "Active") {
            txtStatus.text = "✅ Pass ACTIVE ($foundPlan) • Filtering Enabled"
            txtStatus.setTextColor(Color.parseColor("#3FB950"))
        } else if (foundStatus == "Pending") {
            txtStatus.text = "⏳ Verification Pending for $phone ($foundPlan)"
            txtStatus.setTextColor(Color.parseColor("#D29922"))
        } else {
            txtStatus.text = "No active pass found for $phone. Submit UTR to activate."
            txtStatus.setTextColor(Color.parseColor("#8B949E"))
        }
    }

    private fun loadSavedRulesIntoUi() {
        val prefs = getSharedPreferences("smart_captain_prefs", Context.MODE_PRIVATE)
        findViewById<CheckBox>(R.id.switchService).isChecked = prefs.getBoolean("switchOn", true)
        findViewById<EditText>(R.id.inputMinTrip).setText(prefs.getString("minTrip", "5"))
        findViewById<EditText>(R.id.inputMaxPickup).setText(prefs.getString("maxPickup", "2"))
        findViewById<EditText>(R.id.inputMinFare).setText(prefs.getString("minFare", "60"))
        findViewById<EditText>(R.id.inputGoTo).setText(prefs.getString("goTo", ""))
        findViewById<EditText>(R.id.inputPhone).setText(prefs.getString("phone", "9656896787"))
    }

    private fun seedDefaultAdminEntriesIfNeeded() {
        val prefs = getSharedPreferences("smart_captain_prefs", Context.MODE_PRIVATE)
        if (!prefs.contains("admin_records")) {
            val arr = JSONArray()
            arr.put(JSONObject().apply {
                put("phone", "4567654345")
                put("plan", "1 Day Pass (₹9)")
                put("utr", "876545654343")
                put("status", "Active")
            })
            arr.put(JSONObject().apply {
                put("phone", "5654586787")
                put("plan", "1 Day Pass (₹9)")
                put("utr", "665653564656")
                put("status", "Active")
            })
            arr.put(JSONObject().apply {
                put("phone", "9656896787")
                put("plan", "1 Day Pass (₹9)")
                put("utr", "986579967985")
                put("status", "Active")
            })
            prefs.edit().putString("admin_records", arr.toString()).apply()
        }
    }

    private fun getSubmissionsArray(): JSONArray {
        val prefs = getSharedPreferences("smart_captain_prefs", Context.MODE_PRIVATE)
        val raw = prefs.getString("admin_records", "[]") ?: "[]"
        return JSONArray(raw)
    }

    private fun saveSubmissionRecord(phone: String, plan: String, utr: String) {
        val prefs = getSharedPreferences("smart_captain_prefs", Context.MODE_PRIVATE)
        val arr = getSubmissionsArray()
        val updated = JSONArray()
        var exists = false

        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            if (obj.getString("phone") == phone && obj.getString("utr") == utr) {
                obj.put("plan", plan)
                obj.put("status", "Pending")
                exists = true
            }
            updated.put(obj)
        }

        if (!exists) {
            val newObj = JSONObject().apply {
                put("phone", phone)
                put("plan", plan)
                put("utr", utr)
                put("status", "Pending")
            }
            val freshArr = JSONArray()
            freshArr.put(newObj)
            for (i in 0 until updated.length()) {
                freshArr.put(updated.getJSONObject(i))
            }
            prefs.edit().putString("admin_records", freshArr.toString()).apply()
            return
        }

        prefs.edit().putString("admin_records", updated.toString()).apply()
    }

    private fun approveRecordAt(index: Int) {
        val prefs = getSharedPreferences("smart_captain_prefs", Context.MODE_PRIVATE)
        val arr = getSubmissionsArray()
        if (index in 0 until arr.length()) {
            val obj = arr.getJSONObject(index)
            obj.put("status", "Active")
            prefs.edit().putString("admin_records", arr.toString()).apply()
            renderAdminTable()
            Toast.makeText(this, "Approved ${obj.getString("phone")}!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun renderAdminTable() {
        val container = findViewById<LinearLayout>(R.id.tableAdminRows)
        container.removeAllViews()

        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#21262D"))
            setPadding(12, 16, 12, 16)
        }
        headerRow.addView(makeTableCell("Phone", 260, "#8B949E", true))
        headerRow.addView(makeTableCell("Plan", 310, "#8B949E", true))
        headerRow.addView(makeTableCell("UTR Number", 300, "#8B949E", true))
        headerRow.addView(makeTableCell("Status", 180, "#8B949E", true))
        headerRow.addView(makeTableCell("Action", 240, "#8B949E", true))
        container.addView(headerRow)

        val arr = getSubmissionsArray()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val phone = obj.getString("phone")
            val plan = obj.getString("plan")
            val utr = obj.getString("utr")
            val status = obj.getString("status")

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setBackgroundColor(Color.parseColor("#0D1117"))
                setPadding(12, 18, 12, 18)
            }

            row.addView(makeTableCell(phone, 260, "#C9D1D9", false))
            row.addView(makeTableCell(plan, 310, "#C9D1D9", false))
            row.addView(makeTableCell(utr, 300, "#C9D1D9", false))

            val statusColor = if (status == "Active") "#3FB950" else "#D29922"
            row.addView(makeTableCell(status, 180, statusColor, true))

            if (status == "Active") {
                row.addView(makeTableCell("Approved", 240, "#C9D1D9", false))
            } else {
                val approveBtn = Button(this).apply {
                    text = "Approve"
                    textSize = 11f
                    isAllCaps = false
                    setTextColor(Color.WHITE)
                    backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#238636"))
                    layoutParams = LinearLayout.LayoutParams(220, 95)
                    setOnClickListener { approveRecordAt(i) }
                }
                row.addView(approveBtn)
            }

            container.addView(row)

            val divider = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2)
                setBackgroundColor(Color.parseColor("#21262D"))
            }
            container.addView(divider)
        }
    }

    private fun makeTableCell(textStr: String, widthPx: Int, hexColor: String, bold: Boolean): TextView {
        return TextView(this).apply {
            text = textStr
            textSize = 12f
            setTextColor(Color.parseColor(hexColor))
            if (bold) setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(widthPx, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    private fun syncWithCloudBackend(
        phone: String, plan: String, utr: String,
        minTrip: String, maxPickup: String, minFare: String, goTo: String
    ) {
        thread {
            try {
                val url = URL("$backendBaseUrl/api/submit")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                    connectTimeout = 5000
                }
                val payload = JSONObject().apply {
                    put("phone", phone)
                    put("plan", plan)
                    put("utr", utr)
                    put("minTrip", minTrip)
                    put("maxPickup", maxPickup)
                    put("minFare", minFare)
                    put("goTo", goTo)
                }
                conn.outputStream.use { it.write(payload.toString().toByteArray()) }
                conn.responseCode
            } catch (_: Exception) {
            }
        }
    }
}
