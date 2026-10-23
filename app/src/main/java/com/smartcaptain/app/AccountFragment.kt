package com.example.smartcaptain

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

class AccountFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_account, container, false)
        val prefs = requireContext().getSharedPreferences("SmartCaptainPrefs", Context.MODE_PRIVATE)

        val editMobile = view.findViewById<EditText>(R.id.editMobileNumber)
        val spinnerPassPlan = view.findViewById<Spinner>(R.id.spinnerPassPlan)
        val btnOpenUpi = view.findViewById<Button>(R.id.btnOpenUpi)
        val editUtr = view.findViewById<EditText>(R.id.editUtrNumber)
        val btnSubmitUtr = view.findViewById<Button>(R.id.btnSubmitUtr)
        val txtPassStatus = view.findViewById<TextView>(R.id.txtPassStatus)

        // Pass plans and their corresponding prices
        val plans = arrayOf("1 Day Pass — ₹9 (Unlimited Rides)", "7 Days Pass — ₹49", "30 Days Pass — ₹149")
        val prices = arrayOf("9.00", "49.00", "149.00")
        
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, plans)
        spinnerPassPlan.adapter = adapter

        var selectedAmount = "9.00"
        spinnerPassPlan.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedAmount = prices[position]
                val planName = plans[position]
                val priceText = planName.substringAfter("— ").substringBefore(" ")
                btnOpenUpi.text = "Tap to Pay $priceText via GPay / PhonePe"
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Load saved mobile if available
        val savedMobile = prefs.getString("mobile_number", "") ?: ""
        if (savedMobile.isNotEmpty()) {
            editMobile.setText(savedMobile)
        }

        val passStatus = prefs.getString("pass_status", "No active pass found. Submit UTR to activate.")
        txtPassStatus.text = passStatus

        // Launch UPI Payment App with the dynamic amount
        btnOpenUpi.setOnClickListener {
            val upiId = "kkumae221@okhdfcbank"
            val name = "SmartCaptain"
            val uri = Uri.parse("upi://pay?pa=$upiId&pn=$name&am=$selectedAmount&cu=INR")
            
            val intent = Intent(Intent.ACTION_VIEW, uri)
            try {
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "No UPI app found on device", Toast.LENGTH_SHORT).show()
            }
        }

        btnSubmitUtr.setOnClickListener {
            val mobile = editMobile.text.toString().trim()
            val utr = editUtr.text.toString().trim()

            if (mobile.length != 10) {
                Toast.makeText(requireContext(), "Enter a valid 10-digit mobile number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (utr.length < 12) {
                Toast.makeText(requireContext(), "Enter a valid 12-digit UTR/UPI reference number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            prefs.edit()
                .putString("mobile_number", mobile)
                .putString("last_utr", utr)
                .putString("pass_status", "Pass Verification Pending for +91 $mobile")
                .apply()

            txtPassStatus.text = "Pass Verification Pending for +91 $mobile"
            Toast.makeText(requireContext(), "UTR Submitted Successfully!", Toast.LENGTH_SHORT).show()
        }

        return view
    }
}
