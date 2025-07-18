package com.informatlux.test

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.informatlux.test.R
import kotlin.random.Random

class SplashActivity : AppCompatActivity() {
    private val facts = listOf(
        "Recycling one aluminum can saves enough energy to run a TV for 3 hours!",
        "Composting food waste reduces methane emissions from landfills.",
        "Plastic can take up to 1,000 years to decompose in landfills.",
        "Recycling one ton of paper saves 17 trees and 7,000 gallons of water.",
        "Glass is 100% recyclable and can be recycled endlessly."
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val motto = findViewById<TextView>(R.id.splashMotto)
        val fact = findViewById<TextView>(R.id.splashFact)
        motto.text = "TrashFormers"
        fact.text = facts[Random.nextInt(facts.size)]

        // TODO: Add animation to logo and fact (can be done in XML or programmatically)

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
        }, 2500)
    }
} 