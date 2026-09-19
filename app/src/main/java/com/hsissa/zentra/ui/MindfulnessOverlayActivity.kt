package com.hsissa.zentra.ui

import android.os.Bundle
import android.os.CountDownTimer
import androidx.appcompat.app.AppCompatActivity
import com.hsissa.zentra.R
import com.hsissa.zentra.databinding.ActivityMindfulnessOverlayBinding

class MindfulnessOverlayActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMindfulnessOverlayBinding
    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMindfulnessOverlayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val appName = intent.getStringExtra(EXTRA_APP_NAME)
            ?: getString(R.string.mindfulness_default_app)
        binding.tvMessage.text = getString(R.string.mindfulness_message, appName)

        startMindfulTimer()

        binding.btnClose.setOnClickListener {
            finish()
        }
    }

    private fun startMindfulTimer() {
        countDownTimer = object : CountDownTimer(5000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val sec = (millisUntilFinished / 1000) + 1
                binding.tvBreathTimer.text = getString(R.string.mindfulness_breathe, sec)
                binding.tvBreathTimer.contentDescription = binding.tvBreathTimer.text
            }

            override fun onFinish() {
                binding.tvBreathTimer.text = getString(R.string.mindfulness_complete)
                binding.tvBreathTimer.contentDescription = binding.tvBreathTimer.text
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
