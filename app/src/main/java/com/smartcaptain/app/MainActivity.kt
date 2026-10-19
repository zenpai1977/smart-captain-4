package com.smartcaptain.app

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
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

        applyExactWebStyling()
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

            saveLocalSubmissionRecord(phone, selectedPlanLabel, utr)
            syncWithCloudBackend(phone, selectedPlanLabel, utr, minTrip, maxPickup, minFare, goTo)

            val txtStatus = findViewById<TextView>(R.id.txtCloudStatus)
            txtStatus.text = "Rules Saved! UTR $utr submitted (Pending Admin Approval)"
            txtStatus.setTextColor(Color.parseColor("#D29922"))

            Toast.makeText(this, "Submitted for verification!", Toast.LENGTH_LONG).show()
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

    private fun getSubmissionsArray(): JSONArray {
        val prefs = getSharedPreferences("smart_captain_prefs", Context.MODE_PRIVATE)
        val raw = prefs.getString("admin_records", "[]") ?: "[]"
        return JSONArray(raw)
    }

    private fun saveLocalSubmissionRecord(phone: String, plan: String, utr: String) {
        val prefs = getSharedPreferences("smart_captain_prefs", Context.MODE_PRIVATE)
        val arr = getSubmissionsArray()
        val updated = JSONArray()
        
        val newObj = JSONObject().apply {
            put("phone", phone)
            put("plan", plan)
            put("utr", utr)
            put("status", "Pending")
        }
        updated.put(newObj)

        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            if (obj.getString("utr") != utr) {
                updated.put(obj)
            }
        }

        prefs.edit().putString("admin_records", updated.toString()).apply()
    }

    private fun syncWithCloudBackend(
        phone: String, plan: String, utr: String,
        minTrip: String, maxPickup: String, minFare: String, goTo: String
    ) {
        thread {
            try {
                val url = URL("$backendBaseUrl/api/activate")
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
