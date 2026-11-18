package com.example.data.ai

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

class AudioRecorderHelper {
  private var isRecording = false
  private val pcmBuffer = ByteArrayOutputStream()

  @SuppressLint("MissingPermission")
  suspend fun startRecordingLoop() =
    withContext(Dispatchers.IO) {
      val sampleRate = 16000
      val channelConfig = AudioFormat.CHANNEL_IN_MONO
      val audioFormat = AudioFormat.ENCODING_PCM_16BIT
      val minBuf = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat).coerceAtLeast(4096)

      val recorder =
        AudioRecord(
          MediaRecorder.AudioSource.MIC,
          sampleRate,
          channelConfig,
          audioFormat,
          minBuf,
        )
      if (recorder.state != AudioRecord.STATE_INITIALIZED) {
        recorder.release()
        throw IllegalStateException("Microphone AudioRecord failed to initialize.")
      }

      pcmBuffer.reset()
      isRecording = true
      val chunk = ByteArray(minBuf)
      try {
        recorder.startRecording()
        while (isRecording && isActive) {
          val read = recorder.read(chunk, 0, chunk.size)
          if (read > 0) {
            synchronized(pcmBuffer) { pcmBuffer.write(chunk, 0, read) }
          }
        }
      } finally {
        try {
          recorder.stop()
        } catch (_: Exception) {}
        recorder.release()
      }
    }

  fun stopAndBuildWav(): ByteArray {
    isRecording = false
    val pcmBytes = synchronized(pcmBuffer) { pcmBuffer.toByteArray() }
    return wrapPcmAsWav(pcmBytes, sampleRate = 16000, channels = 1, bitsPerSample = 16)
  }

  private fun wrapPcmAsWav(
    pcm: ByteArray,
    sampleRate: Int,
    channels: Int,
    bitsPerSample: Int,
  ): ByteArray {
    val byteRate = sampleRate * channels * bitsPerSample / 8
    val blockAlign = channels * bitsPerSample / 8
    val totalDataLen = pcm.size + 36
    val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
    header.put("RIFF".toByteArray(Charsets.US_ASCII))
    header.putInt(totalDataLen)
    header.put("WAVE".toByteArray(Charsets.US_ASCII))
    header.put("fmt ".toByteArray(Charsets.US_ASCII))
    header.putInt(16) // PCM subchunk size
    header.putShort(1.toShort()) // AudioFormat = PCM
    header.putShort(channels.toShort())
    header.putInt(sampleRate)
    header.putInt(byteRate)
    header.putShort(blockAlign.toShort())
    header.putShort(bitsPerSample.toShort())
    header.put("data".toByteArray(Charsets.US_ASCII))
    header.putInt(pcm.size)
    return header.array() + pcm
  }
}
