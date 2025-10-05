package net.forestany.battleship.game

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import net.forestany.battleship.GlobalInstance
import net.forestany.battleship.R

object SoundManager {
    private lateinit var soundPool: SoundPool
    private val soundMap = HashMap<Int, Int>()
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(audioAttributes)
            .build()

        soundMap[1] = soundPool.load(context.applicationContext, R.raw.canon_shot, 1)
        soundMap[2] = soundPool.load(context.applicationContext, R.raw.water_explosion, 1)
        soundMap[3] = soundPool.load(context.applicationContext, R.raw.explosion, 1)
        soundMap[4] = soundPool.load(context.applicationContext, R.raw.fanfare, 1)

        isInitialized = true
    }

    fun playSound(eventId: Int) {
        if (GlobalInstance.get().getPreferences()["sound_effects"] as Boolean) {
            soundMap[eventId]?.let {
                soundPool.play(it, 1f, 1f, 1, 0, 1f)
            }
        }
    }

    fun release() {
        if (isInitialized) {
            soundPool.release()
            isInitialized = false
        }
    }
}