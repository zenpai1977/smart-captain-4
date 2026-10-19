package com.example.smartcaptain

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment

class RulesFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_rules, container, false)
        val prefs = requireContext().getSharedPreferences("SmartCaptainPrefs", Context.MODE_PRIVATE)

        val chkBike = view.findViewById<CheckBox>(R.id.chkBike)
        val chkAuto = view.findViewById<CheckBox>(R.id.chkAuto)
        val chkParcel = view.findViewById<CheckBox>(R.id.chkParcel)
        val chkCab = view.findViewById<CheckBox>(R.id.chkCab)

        val seekPickup = view.findViewById<SeekBar>(R.id.seekPickupDistance)
        val txtPickupVal = view.findViewById<TextView>(R.id.txtPickupDistanceVal)

        val seekTrip = view.findViewById<SeekBar>(R.id.seekTripDistance)
        val txtTripVal = view.findViewById<TextView>(R.id.txtTripDistanceVal)

        val editMinFare = view.findViewById<EditText>(R.id.editMinFare)
        val btnSave = view.findViewById<Button>(R.id.btnSaveRules)

        // Load existing settings
        chkBike.isChecked = prefs.getBoolean("rule_bike", true)
        chkAuto.isChecked = prefs.getBoolean("rule_auto", true)
        chkParcel.isChecked = prefs.getBoolean("rule_parcel", true)
        chkCab.isChecked = prefs.getBoolean("rule_cab", true)

        val savedPickup = prefs.getInt("max_pickup_km", 3)
        seekPickup.progress = savedPickup
        txtPickupVal.text = "Maximum: $savedPickup km"

        val savedTrip = prefs.getInt("max_trip_km", 50)
        seekTrip.progress = savedTrip
        txtTripVal.text = "Maximum: $savedTrip km"

        val savedFare = prefs.getInt("min_fare", 0)
        if (savedFare > 0) editMinFare.setText(savedFare.toString())

        seekPickup.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                txtPickupVal.text = "Maximum: $progress km"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekTrip.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                txtTripVal.text = "Maximum: $progress km"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnSave.setOnClickListener {
            val minFareVal = editMinFare.text.toString().toIntOrNull() ?: 0
            prefs.edit()
                .putBoolean("rule_bike", chkBike.isChecked)
                .putBoolean("rule_auto", chkAuto.isChecked)
                .putBoolean("rule_parcel", chkParcel.isChecked)
                .putBoolean("rule_cab", chkCab.isChecked)
                .putInt("max_pickup_km", seekPickup.progress)
                .putInt("max_trip_km", seekTrip.progress)
                .putInt("min_fare", minFareVal)
                .apply()

            Toast.makeText(requireContext(), "Rules Saved!", Toast.LENGTH_SHORT).show()
        }

        return view
    }
}
