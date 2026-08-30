package com.jiangxin.siterecord.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/**
 * 语音备忘的录制与播放。
 *
 * 老板在工地手上有灰、有手套，打字不方便，语音比文字快得多——
 * 「业主说厨房插座要挪到这边」这种一句话，说比打快十倍。
 */
class VoiceRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private var currentFile: File? = null

    /** 开始录音，返回输出文件；失败（含无麦克风权限）返回 null */
    fun start(): File? {
        return try {
            releaseRecorder()
            val dir = File(context.filesDir, "voices")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "voice_${System.currentTimeMillis()}.m4a")

            val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            r.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(64_000)
                setAudioSamplingRate(44_100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            recorder = r
            currentFile = file
            file
        } catch (t: Throwable) {
            releaseRecorder()
            null
        }
    }

    /** 停止录音并返回文件；录制时长过短导致失败时返回 null */
    fun stop(): File? {
        val r = recorder ?: return null
        return try {
            r.stop()
            val f = currentFile
            releaseRecorder()
            // 置空很关键：文件已交给调用方保管，否则离开页面时 onDispose 会调 cancel()，
            // 把刚录好、还等着保存的语音删掉
            currentFile = null
            f
        } catch (t: Throwable) {
            // 录得太短就停止会抛异常，此时文件不可用，删掉免得留下 0 字节垃圾
            currentFile?.delete()
            releaseRecorder()
            currentFile = null
            null
        }
    }

    /** 放弃本次录音（用户点删除或离开页面） */
    fun cancel() {
        try { recorder?.stop() } catch (_: Throwable) {}
        releaseRecorder()
        currentFile?.delete()
        currentFile = null
    }

    fun play(path: String, onEnd: () -> Unit = {}) {
        try {
            stopPlay()
            val p = MediaPlayer()
            p.setDataSource(path)
            p.setOnCompletionListener { stopPlay(); onEnd() }
            p.prepare()
            p.start()
            player = p
        } catch (t: Throwable) {
            stopPlay()
        }
    }

    fun stopPlay() {
        try { player?.stop() } catch (_: Throwable) {}
        try { player?.release() } catch (_: Throwable) {}
        player = null
    }

    private fun releaseRecorder() {
        try { recorder?.release() } catch (_: Throwable) {}
        recorder = null
    }
}
