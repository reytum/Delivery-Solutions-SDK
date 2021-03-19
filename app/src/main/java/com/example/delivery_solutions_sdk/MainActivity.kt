package com.example.delivery_solutions_sdk

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.delivery_solutions.DeliverySolutions

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        DeliverySolutions.startLocationMonitoring(this)
    }
}