package com.openminis.app.ui.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.*

class SpeechToText(private val context: Context) {

    private val sr: SpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
    private var mListener: RecognitionListener? = null
    private var isListening = false

    companion object {
        private const val TAG = "SpeechToText"
    }

    init {
        mListener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "onReadyForSpeech")
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "onBeginningOfSpeech")
            }

            override fun onRmsChanged(rmsdB: Float) {
                Log.d(TAG, "onRmsChanged: $rmsdB")
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                Log.d(TAG, "onBufferReceived")
            }

            override fun onEndOfSpeech() {
                Log.d(TAG, "onEndOfSpeech")
                isListening = false
            }

            override fun onError(error: Int) {
                Log.e(TAG, "onError: $error")
                isListening = false
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                Log.d(TAG, "onResults: $matches")
                isListening = false
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.firstOrNull()?.let {
                    Log.d(TAG, "onPartialResults: $it")
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                Log.d(TAG, "onEvent")
            }
        }
        sr.setRecognitionListener(mListener)
    }

    fun startListening(language: String = "fa-IR", timeoutMs: Long = 60000) {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        sr.startListening(intent)
        isListening = true
    }

    fun stopListening() {
        if (isListening) {
            sr.stopListening()
            isListening = false
        }
    }

    fun cancel() {
        sr.cancel()
        isListening = false
    }

    fun destroy() {
        sr.destroy()
        sr.setRecognitionListener(null)
        mListener = null
    }

    private var tts: TextToSpeech? = null
    private val speakQueue = LinkedList<String>()

    private fun speakNext() {
        if (tts?.isSpeaking == true || speakQueue.isEmpty()) return
        val text = speakQueue.poll() ?: return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    fun speak(text: String, lang: String = "fa-IR", onDone: (() -> Unit)? = null) {
        if (tts == null) {
            tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val result = tts?.setLanguage(Locale(lang))
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        tts?.language = Locale.ENGLISH
                    }
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {}
                        override fun onDone(utteranceId: String?) {
                            speakNext()
                        }
                        override fun onError(utteranceId: String?) {}
                    })
                    speakNext()
                }
            }
        } else {
            speakNext()
        }
    }

    fun stopSpeaking() {
        tts?.stop()
    }

    fun destroyTTS() {
        tts?.shutdown()
        tts = null
    }
}
