package com.applicaster.mobile.player

import android.content.Context
import android.view.ViewGroup
import com.applicaster.player.defaultplayer.BasePlayer
import com.applicaster.plugin_manager.playersmanager.PlayableConfiguration
import com.applicaster.plugin_manager.playersmanager.PlayerContract
import com.google.android.exoplayer2.ui.PlayerView

class AndroidMobilePlayerAdaptor : BasePlayer() {

    private lateinit var playerView: PlayerView

    override fun init(context: Context) {
        super.init(context)
        playerView = PlayerView(context)
    }

    override fun getPlayerType(): PlayerContract.PlayerType {
        return PlayerContract.PlayerType.Default
    }

    override fun playInFullscreen(configuration: PlayableConfiguration?, requestCode: Int, context: Context) {
        super.playInFullscreen(configuration, requestCode, context)
    }

    override fun attachInline(videoContainerView: ViewGroup) {
        super.attachInline(videoContainerView)
        videoContainerView.addView(playerView)
    }

    override fun playInline(configuration: PlayableConfiguration?) {
        super.playInline(configuration)

    }

    override fun removeInline(videoContainerView: ViewGroup) {
        super.removeInline(videoContainerView)
        videoContainerView.removeView(playerView)
    }

    override fun stopInline() {
        super.stopInline()
    }

    override fun pauseInline() {
        super.pauseInline()
    }

    override fun resumeInline() {
        super.resumeInline()
    }
}
