package com.applicaster.mobile.player

import android.content.Context
import android.net.Uri
import android.view.ViewGroup
import com.applicaster.player.defaultplayer.BasePlayer
import com.applicaster.plugin_manager.playersmanager.Playable
import com.applicaster.plugin_manager.playersmanager.PlayableConfiguration
import com.applicaster.plugin_manager.playersmanager.PlayerContract
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.PlayerView
import com.applicaster.controller.PlayerLoader
import com.applicaster.mobile.player.utils.PlayerBuilder
import com.applicaster.player.PlayerLoaderI
import com.applicaster.model.APModel



class AndroidMobilePlayerAdaptor : BasePlayer(), PlayerLoaderI {

    private lateinit var playerView: PlayerView
    private var player: SimpleExoPlayer? = null
    private var loader: PlayerLoader? = null
    private var playable: Playable? = null

    override fun init(playable: Playable, context: Context) {
        this.init(listOf(playable), context)
    }

    override fun init(playableList: List<Playable>, context: Context) {
        super.init(playableList, context)
        playerView = PlayerView(context)
    }

    override fun init(context: Context) {
        super.init(context)
        playerView = PlayerView(context)
    }

    override fun getPlayerType(): PlayerContract.PlayerType {
        return PlayerContract.PlayerType.Default
    }

    override fun playInFullscreen(configuration: PlayableConfiguration?, requestCode: Int, context: Context) {
        super.playInFullscreen(configuration, requestCode, context)
        // todo: second phase of internal development
    }

    override fun attachInline(videoContainerView: ViewGroup) {
        super.attachInline(videoContainerView)
        videoContainerView.addView(playerView)
    }

    override fun playInline(configuration: PlayableConfiguration?) {
        super.playInline(configuration)
        loadPlayable()
    }

    private fun loadPlayable() {
        loader = PlayerLoader(this)
        loader?.loadItem()
    }

    // after the playable is loaded call this method
    private fun play(playable: Playable?) {
        firstPlayable?.let {
            // player view
            this.player = PlayerBuilder().buildPlayer(context)
            playerView.player = player

            player?.prepare(PlayerBuilder().buildMediaSource(context,
                    Uri.parse(playable?.contentVideoURL)))
        }
    }

    override fun removeInline(videoContainerView: ViewGroup) {
        super.removeInline(videoContainerView)
        videoContainerView.removeView(playerView)
        releasePlayer()
    }

    override fun stopInline() {
        super.stopInline()
        releasePlayer()
    }

    override fun pauseInline() {
        super.pauseInline()
        releasePlayer()
    }

    override fun resumeInline() {
        super.resumeInline()
        play(this.playable)
    }

    private fun releasePlayer() {
        player?.release()
        player = null
        playerView.player = null
    }

    // region PlayerLoaderI
    override fun getItemId(): String? {
        return getApplicasterModelId()
    }

    private fun getApplicasterModelId(): String? {
        var id: String? = null

        if (firstPlayable is APModel) {
            val model = firstPlayable as APModel
            id = model.id
        }

        return id
    }

    override fun getPlayable(): Playable? {
        return firstPlayable
    }

    override fun onItemLoaded(playable: Playable?) {
        this.playable = playable
        play(playable)
    }

    override fun isFinishing(): Boolean {
        TODO("not implemented")
    }

    override fun showMediaErroDialog() {
        TODO("not implemented")
    }
    // endregion
}
