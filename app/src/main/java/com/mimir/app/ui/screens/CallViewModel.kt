package com.mimir.app.ui.screens

import android.app.Application
import android.media.*
import androidx.lifecycle.*
import com.mimir.app.data.MimirBridge
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import uniffi.mimir.CallStatus

class CallViewModel(app: Application) : AndroidViewModel(app) {

    val callStatus = MutableStateFlow<CallStatus>(CallStatus.IDLE)
    val callPubkey = MutableStateFlow<String?>(null)
    val callDuration = MutableStateFlow(0L)   // seconds

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var durationJob: Job? = null

    private val SAMPLE_RATE  = 44100
    private val CHANNEL_IN   = AudioFormat.CHANNEL_IN_MONO
    private val CHANNEL_OUT  = AudioFormat.CHANNEL_OUT_MONO
    private val ENCODING      = AudioFormat.ENCODING_AAC_ELD
    private val BUFFER_SIZE   = 4096

    init {
        viewModelScope.launch {
            MimirBridge.events.collect { event ->
                when (event) {
                    is MimirBridge.Event.IncomingCall -> {
                        callPubkey.value = event.pubkeyHex
                        callStatus.value = CallStatus.RECEIVING
                    }
                    is MimirBridge.Event.CallStatusChanged -> {
                        callStatus.value = event.status
                        when (event.status) {
                            CallStatus.IN_CALL -> startAudio(event.pubkeyHex ?: return@collect)
                            CallStatus.HANGUP, CallStatus.IDLE -> stopAudio()
                            else -> {}
                        }
                    }
                    is MimirBridge.Event.CallPacket -> playPacket(event.data)
                    else -> {}
                }
            }
        }
    }

    fun startCall(pubkeyHex: String) {
        callPubkey.value = pubkeyHex
        callStatus.value = CallStatus.CALLING
        MimirBridge.startCall(pubkeyHex)
    }

    fun answerCall(accept: Boolean) {
        val key = callPubkey.value ?: return
        MimirBridge.answerCall(key, accept)
        if (!accept) { callStatus.value = CallStatus.IDLE; callPubkey.value = null }
    }

    fun hangup() {
        callPubkey.value?.let { MimirBridge.hangupCall(it) }
        callStatus.value = CallStatus.IDLE
        callPubkey.value = null
        stopAudio()
    }

    private fun startAudio(pubkeyHex: String) {
        // AudioRecord — захват микрофона и отправка пакетов
        val bufSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_IN, AudioFormat.ENCODING_PCM_16BIT)
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            SAMPLE_RATE, CHANNEL_IN, AudioFormat.ENCODING_PCM_16BIT, bufSize
        ).also { rec ->
            rec.startRecording()
            viewModelScope.launch(Dispatchers.IO) {
                val buf = ByteArray(BUFFER_SIZE)
                while (callStatus.value == CallStatus.IN_CALL) {
                    val read = rec.read(buf, 0, buf.size)
                    if (read > 0) MimirBridge.sendCallPacket(pubkeyHex, buf.copyOf(read))
                }
            }
        }

        // AudioTrack — воспроизведение входящего звука
        val trackBuf = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_OUT, AudioFormat.ENCODING_PCM_16BIT)
        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build())
            .setAudioFormat(AudioFormat.Builder()
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(CHANNEL_OUT)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .build())
            .setBufferSizeInBytes(trackBuf)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
            .also { it.play() }

        // Счётчик длительности
        durationJob = viewModelScope.launch {
            while (callStatus.value == CallStatus.IN_CALL) {
                delay(1000); callDuration.value++
            }
        }
    }

    private fun playPacket(data: ByteArray) {
        audioTrack?.write(data, 0, data.size)
    }

    private fun stopAudio() {
        durationJob?.cancel()
        callDuration.value = 0
        audioRecord?.stop(); audioRecord?.release(); audioRecord = null
        audioTrack?.stop(); audioTrack?.release(); audioTrack = null
    }

    override fun onCleared() { stopAudio(); super.onCleared() }
}
