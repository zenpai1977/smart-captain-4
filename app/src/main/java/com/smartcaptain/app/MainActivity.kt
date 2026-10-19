package com.smartcaptain.app

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Find buttons by their generic order or structure if needed, 
        // or add IDs to your XML layout buttons. For now, let's add click feedback:
        val payBtn = findViewById<Button>(android.R.id.button1) // or attach click listeners once IDs are assigned
        
        // Let's make sure the activity loads smoothly and shows a confirmation toast on actions
        Toast.makeText(this, "Smart Captain Dashboard Loaded", Toast.LENGTH_SHORT).show()
    }
}
