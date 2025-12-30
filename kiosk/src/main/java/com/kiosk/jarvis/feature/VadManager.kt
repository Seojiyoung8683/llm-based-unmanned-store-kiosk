package com.kiosk.jarvis.feature

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.content.res.AssetManager
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.k2fsa.sherpa.onnx.Vad
import com.k2fsa.sherpa.onnx.getVadModelConfig
import com.suda.agent.core.AudioBufferStore

class VadManager(
    private val context: Context,
    assetManager: AssetManager,
    private val onVoiceStart: () -> Unit = {},
    private val onVoiceEnd: (ArrayList<Float>) -> Unit = {},
) {
    private val TAG = VadManager::class.simpleName

    private val sampleRate = 16000
    private val bufferSize by lazy {
        AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
    }

    private var audioRecord: AudioRecord? = null

    @Volatile
    private var isRecording: Boolean = false

    @Volatile
    private var inSpeech: Boolean = false

    private val speechBuffer: ArrayList<Float> = ArrayList()
    private val speechBufferLock = Any()

    private val vad = Vad(
        assetManager = assetManager,
        getVadModelConfig(0)!!
    )

    fun startRecording() {
        Log.d(TAG, "startRecording() called")

        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "RECORD_AUDIO permission not granted; cannot start recording")
            return
        }

        if (audioRecord == null) {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )
        }

        val record = audioRecord
        if (record == null || record.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord initialization failed")
            Log.e(TAG, "AudioRecord state: ${record?.state}")
            Log.e(TAG, "Sample rate: $sampleRate, Buffer size: $bufferSize")
            Log.e(
                TAG,
                "Min buffer size: ${
                    AudioRecord.getMinBufferSize(
                        sampleRate,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT
                    )
                }"
            )
            return
        }

        synchronized(speechBufferLock) {
            speechBuffer.clear()
        }
        AudioBufferStore.releaseBuffer()
        inSpeech = false

        isRecording = true
        record.startRecording()
        Log.d(TAG, "AudioRecord started")

        CoroutineScope(Dispatchers.IO).launch {
            val buffer = ShortArray(bufferSize)
            while (isRecording) {
                val read = record.read(buffer, 0, buffer.size)
                if (read > 0) {
                    AudioBufferStore.writeData(buffer.copyOf(read))

                    val floatBuffer = FloatArray(read) { buffer[it] / 32768.0f }

                    try {
                        vad.acceptWaveform(floatBuffer)
                        val isSpeechDetected = vad.isSpeechDetected()

                        if (isSpeechDetected) {
                            if (!inSpeech) {
                                Log.d(TAG, "Speech detected")
                                inSpeech = true
                                onVoiceStart()

                                val preShortBuffer = AudioBufferStore.getStoredBuffer()
                                val preFloatBuffer =
                                    FloatArray(preShortBuffer.size) { preShortBuffer[it] / 32768.0f }

                                synchronized(speechBufferLock) {
                                    speechBuffer.clear()
                                    speechBuffer.addAll(preFloatBuffer.toList())
                                }
                            }

                            synchronized(speechBufferLock) {
                                speechBuffer.addAll(floatBuffer.toList())
                            }
                        } else {
                            if (inSpeech) {
                                Log.d(TAG, "Speech ended by VAD")
                                inSpeech = false

                                val copied: ArrayList<Float>
                                synchronized(speechBufferLock) {
                                    copied = ArrayList(speechBuffer)
                                    speechBuffer.clear()
                                }

                                if (copied.isNotEmpty()) {
                                    onVoiceEnd(copied)
                                } else {
                                    Log.w(TAG, "Speech buffer empty on VAD end")
                                }

                                stopRecording(fromVad = true)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing audio", e)
                    }
                }
            }
            Log.d(TAG, "Recording loop finished")
        }
    }

    fun stopRecording(fromVad: Boolean = false) {
        Log.d(TAG, "stopRecording(fromVad=$fromVad) called")
        if (isRecording) {
            isRecording = false
            try {
                audioRecord?.stop()
            } catch (e: Exception) {
                Log.w(TAG, "AudioRecord stop error", e)
            }

            if (!fromVad) {
                val copied: ArrayList<Float>
                synchronized(speechBufferLock) {
                    copied = ArrayList(speechBuffer)
                    speechBuffer.clear()
                }

                if (copied.isNotEmpty()) {
                    Log.d(TAG, "Force onVoiceEnd by MicReleased, buffer size=${copied.size}")
                    onVoiceEnd(copied)
                } else {
                    Log.w(TAG, "stopRecording(): no speech buffer to send")
                }
            } else {
                synchronized(speechBufferLock) {
                    speechBuffer.clear()
                }
            }

            AudioBufferStore.releaseBuffer()
            inSpeech = false
        }
    }

    fun release() {
        Log.d(TAG, "release()")
        try {
            audioRecord?.release()
        } catch (e: Exception) {
            Log.w(TAG, "AudioRecord release error", e)
        }
        audioRecord = null
        AudioBufferStore.releaseBuffer()
    }
}
