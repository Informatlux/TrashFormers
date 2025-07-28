
package com.informatlux.test

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
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
        val subtitle = findViewById<TextView>(R.id.subtitle)

        motto.text = "TrashFormers"
        subtitle.text = "Transforming Waste, Sustaining Future"
        fact.text = facts[Random.nextInt(facts.size)]

        setupAppleStyleAnimations()
        animateFloatingElements()

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }, 4000)
    }

    private fun setupAppleStyleAnimations() {
        val glassContainer = findViewById<MaterialCardView>(R.id.glass_container)
        val logoContainer = findViewById<MaterialCardView>(R.id.logo_container)
        val motto = findViewById<TextView>(R.id.splashMotto)
        val subtitle = findViewById<TextView>(R.id.subtitle)
        val factContainer = findViewById<MaterialCardView>(R.id.fact_container)
        val progress = findViewById<View>(R.id.splashProgress)

        // Initial state with more dramatic entrance
        val views = listOf(glassContainer, logoContainer, motto, subtitle, factContainer, progress)
        views.forEach { view ->
            view.alpha = 0f
            view.scaleX = 0.3f
            view.scaleY = 0.3f
            view.translationY = 200f
        }

        // Main container with Apple-style spring animation
        val containerAnimator = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(glassContainer, "alpha", 0f, 1f),
                ObjectAnimator.ofFloat(glassContainer, "scaleX", 0.3f, 1.05f, 1f),
                ObjectAnimator.ofFloat(glassContainer, "scaleY", 0.3f, 1.05f, 1f),
                ObjectAnimator.ofFloat(glassContainer, "translationY", 200f, -20f, 0f)
            )
            duration = 1200
            interpolator = OvershootInterpolator(0.6f)
        }

        // Logo with bounce effect
        val logoAnimator = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(logoContainer, "alpha", 0f, 1f),
                ObjectAnimator.ofFloat(logoContainer, "scaleX", 0.3f, 1.2f, 1f),
                ObjectAnimator.ofFloat(logoContainer, "scaleY", 0.3f, 1.2f, 1f),
                ObjectAnimator.ofFloat(logoContainer, "translationY", 100f, -10f, 0f),
                ObjectAnimator.ofFloat(logoContainer, "rotation", -15f, 5f, 0f)
            )
            duration = 1000
            startDelay = 400
            interpolator = OvershootInterpolator(0.8f)
        }

        // Text elements with elegant fade and slide
        animateTextElementsSequentially(listOf(motto, subtitle, factContainer, progress))

        containerAnimator.start()
        logoAnimator.start()
    }

    private fun animateTextElementsSequentially(views: List<View>) {
        views.forEachIndexed { index, view ->
            val animator = AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(view, "alpha", 0f, 1f),
                    ObjectAnimator.ofFloat(view, "scaleX", 0.8f, 1f),
                    ObjectAnimator.ofFloat(view, "scaleY", 0.8f, 1f),
                    ObjectAnimator.ofFloat(view, "translationY", 80f, 0f)
                )
                duration = 800
                startDelay = (800 + index * 200).toLong()
                interpolator = DecelerateInterpolator(1.5f)
            }
            animator.start()
        }
    }

    private fun animateFloatingElements() {
        val element1 = findViewById<View>(R.id.floating_element_1)
        val element2 = findViewById<View>(R.id.floating_element_2)
        val element3 = findViewById<View>(R.id.floating_element_3)

        // Organic floating motion
        val float1Y = ObjectAnimator.ofFloat(element1, "translationY", 0f, -40f, 0f, 30f, 0f).apply {
            duration = 8000
            repeatCount = ObjectAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
        }

        val float1X = ObjectAnimator.ofFloat(element1, "translationX", 0f, 20f, -10f, 0f).apply {
            duration = 6000
            repeatCount = ObjectAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
        }

        val rotate2 = ObjectAnimator.ofFloat(element2, "rotation", 0f, 360f).apply {
            duration = 12000
            repeatCount = ObjectAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
        }

        val scale3 = ObjectAnimator.ofFloat(element3, "scaleX", 1f, 1.4f, 0.8f, 1f).apply {
            duration = 5000
            repeatCount = ObjectAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
        }

        val scaleY3 = ObjectAnimator.ofFloat(element3, "scaleY", 1f, 1.4f, 0.8f, 1f).apply {
            duration = 5000
            repeatCount = ObjectAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
        }

        // Add subtle alpha pulsing
        val pulse1 = ObjectAnimator.ofFloat(element1, "alpha", 0.15f, 0.25f, 0.15f).apply {
            duration = 4000
            repeatCount = ObjectAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
        }

        // Start all animations
        float1Y.start()
        float1X.start()
        rotate2.start()
        scale3.start()
        scaleY3.start()
        pulse1.start()
    }
}
