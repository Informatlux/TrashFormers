package com.informatlux.test

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar

class LeaderboardActivity : AppCompatActivity() {

    private val viewModel: LeaderboardViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leaderboard)

        setupToolbar()
        setupRecyclerView()
        val listView = ListView(this)
        setContentView(listView)

        val scores = ScoreManager.getAllScores().toList().sortedByDescending { it.second }
        val leaderboard = scores.mapIndexed { index, (user, score) ->
            "${index + 1}. $user: $score points"
        }
        listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, leaderboard)
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.leaderboard_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Observe the LiveData from the ViewModel
        viewModel.leaderboardEntries.observe(this) { entries ->
            if (entries != null) {
                recyclerView.adapter = LeaderboardAdapter(entries)
                // Run the entry animation after the adapter has been set
                runEntryAnimation(recyclerView)
            }
        }
    }

    private fun runEntryAnimation(recyclerView: RecyclerView) {
        recyclerView.post {
            for (i in 0 until recyclerView.childCount) {
                val view: View = recyclerView.getChildAt(i)
                view.alpha = 0f
                view.translationY = 50f

                val animator = AnimatorSet().apply {
                    playTogether(
                        ObjectAnimator.ofFloat(view, "alpha", 0f, 1f),
                        ObjectAnimator.ofFloat(view, "translationY", 50f, 0f)
                    )
                    duration = 500
                    startDelay = (i * 80).toLong()
                    interpolator = DecelerateInterpolator()
                }
                animator.start()
            }
        }
    }
}