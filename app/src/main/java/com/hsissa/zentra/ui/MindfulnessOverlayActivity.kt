package com.hsissa.zentra.ui

import android.os.Bundle
import android.os.CountDownTimer
import androidx.appcompat.app.AppCompatActivity
import com.hsissa.zentra.databinding.ActivityMindfulnessOverlayBinding

class MindfulnessOverlayActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMindfulnessOverlayBinding
    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMindfulnessOverlayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: "this app"
        binding.tvMessage.text = "You've reached your configured usage quota for $appName. Take a moment to reflect before continuing."

        startMindfulTimer()

        binding.btnClose.setOnClickListener {
            finish()
        }
    }

    private fun startMindfulTimer() {
        countDownTimer = object : CountDownTimer(5000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val sec = (millisUntilFinished / 1000) + 1
                binding.tvBreathTimer.text = "Breathe in... ($sec s)"
            }

            override fun onFinish() {
                binding.tvBreathTimer.text = "Mindful Pause Complete"
                binding.btnClose.isEnabled = true
            }
        }.start()
    }

    override fun onDestroy() {
        countDownTimer?.cancel()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_APP_NAME = "extra_app_name"
    }
}
