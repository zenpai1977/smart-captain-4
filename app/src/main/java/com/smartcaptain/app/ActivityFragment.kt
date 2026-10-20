package com.smartcaptain.app

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment

class ActivityFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_activity, container, false)
        val prefs = requireContext().getSharedPreferences("SmartCaptainPrefs", Context.MODE_PRIVATE)

        val txtLog = view.findViewById<TextView>(R.id.txtActivityLog)
        val btnClear = view.findViewById<Button>(R.id.btnClearLogs)

        val defaultText = "No bookings seen yet.\n\nGo on duty in your ride app and leave this running."
        txtLog.text = prefs.getString("activity_log", defaultText)

        btnClear.setOnClickListener {
            val editor = prefs.edit()
            editor.remove("activity_log")
            editor.apply()
            txtLog.text = defaultText
        }

        return view
    }
}
