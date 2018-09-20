package net.markkoh.harmonikoh

import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.SeekBar
import kotlinx.android.synthetic.main.activity_frequency_picker.*
import java.lang.Math.pow
import kotlin.math.log


class FrequencyPickerActivity : AppCompatActivity() {

    val TAG = "FrequencyPickerActivity"

    val MIN_FREQ = 20  // 20Hz
    val MAX_FREQ = 12000  // 12kHz

    val LOG_BASE = 10.toDouble()
    val MIN_LOG = log(MIN_FREQ.toDouble(), LOG_BASE)
    val MAX_LOG = log(MAX_FREQ.toDouble(), LOG_BASE)
    val LOG_DIFF = MAX_LOG - MIN_LOG

    var isPlaying = false
    var updatingFreq = false
    var currentFreq = 440
    var currentTrack: AudioTrack? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_frequency_picker)

        // Initialize some stuff
        this.isPlaying = false
        updateFrequency(currentFreq)


        slider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!updatingFreq && seekBar is SeekBar) {
                    // Get the slider value and calculate the log-scaled progress
                    val freq = logScaleFrequency(seekBar.progress)
                    updateFrequency(freq)
                }
            }
        })
    }

    fun updateFrequency(freq: Int) {
        Log.d(TAG, "Updating freqency to $freq")
        this.updatingFreq = true
        this.currentFreq = freq
        frequencyField.setText("$freq")
        val newSliderProgress = logScaleSliderPos(freq)
        Log.d(TAG, "Setting current slider value to $newSliderProgress")
        slider.progress = newSliderProgress
        this.currentTrack = generateTone(currentFreq.toDouble(), 2000)
        this.updatingFreq = false
    }

    /**
     * Given a value between 0 and 100, determine the log scale frequency for that value
     */
    fun logScaleFrequency(value: Int): Int {
        if (value == 0) {
            return MIN_FREQ
        }
        val scaledFreq = (value / 100.0) * LOG_DIFF + MIN_LOG
        val inverse = pow(LOG_BASE, scaledFreq)
        return inverse.toInt()
    }

    /**
     * Given a frequency, determine the correct log-scale slider position for that frequency
     */
    fun logScaleSliderPos(freq: Int): Int {
        if (freq <= MIN_FREQ) {
            return 0
        }
        val logFreq = log(freq.toDouble(), LOG_BASE)
        val scaled = (logFreq - MIN_LOG) / LOG_DIFF
        return (scaled * 100).toInt()
    }


    fun playClicked(view: View) {
        this.isPlaying = true
        Log.i(TAG, "Playing tone $currentFreq")
        currentTrack?.play()
    }

    fun stopClicked(view: View) {
        this.isPlaying = false
        currentTrack?.stop()
    }

    private fun generateTone(freqHz: Double, durationMs: Int): AudioTrack {
        val count = (44100.0 * 2.0 * (durationMs / 1000.0)).toInt() and 1.inv()
        val samples = ShortArray(count)
        var i = 0
        while (i < count) {
            val sample = (Math.sin(2.0 * Math.PI * i.toDouble() / (44100.0 / freqHz)) * 0x7FFF).toShort()
            samples[i + 0] = sample
            samples[i + 1] = sample
            i += 2
        }

        val bufferSize = count * (java.lang.Short.SIZE / 8)

        val format = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                .setSampleRate(44100)
                .build()

        val track = AudioTrack.Builder()
                .setAudioFormat(format)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(bufferSize)
                .build()

        track.write(samples, 0, count)
        return track
    }
}
