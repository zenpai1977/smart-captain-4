package com.smartcaptain.app

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
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
        val editReferral = view.findViewById<EditText>(R.id.editReferralCode)
        val btnSave = view.findViewById<Button>(R.id.btnSaveAccount)
        val txtStatus = view.findViewById<TextView>(R.id.txtAccountStatus)

        val savedMobile = prefs.getString("mobile_number", "") ?: ""
        if (savedMobile.isNotEmpty()) {
            editMobile.setText(savedMobile)
            txtStatus.text = "Status: Registered (+91 $savedMobile)"
        }

        btnSave.setOnClickListener {
            val mobile = editMobile.text.toString().trim()
            if (mobile.length == 10) {
                prefs.edit()
                    .putString("mobile_number", mobile)
                    .putString("referral_code", editReferral.text.toString().trim())
                    .apply()
                txtStatus.text = "Status: Registered (+91 $mobile)"
                Toast.makeText(requireContext(), "Account Saved!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Enter a valid 10-digit number", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }
}
