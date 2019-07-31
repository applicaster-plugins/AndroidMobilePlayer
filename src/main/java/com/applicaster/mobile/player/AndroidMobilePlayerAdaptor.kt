package com.applicaster.mobile.player

import android.content.Context
import android.net.Uri
import android.view.ViewGroup
import com.applicaster.player.defaultplayer.BasePlayer
import com.applicaster.plugin_manager.playersmanager.Playable
import com.applicaster.plugin_manager.playersmanager.PlayableConfiguration
import com.applicaster.plugin_manager.playersmanager.PlayerContract
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.util.Util


class AndroidMobilePlayerAdaptor : BasePlayer() {

    private lateinit var playerView: PlayerView
    private var player: SimpleExoPlayer? = null
    private val BANDWIDTH_METER = DefaultBandwidthMeter()
    private var trackSelector: DefaultTrackSelector? = null

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
    }

    override fun attachInline(videoContainerView: ViewGroup) {
        super.attachInline(videoContainerView)
        videoContainerView.addView(playerView)
    }

    override fun playInline(configuration: PlayableConfiguration?) {
        super.playInline(configuration)
        firstPlayable?.let {
            val videoTrackSelectionFactory = AdaptiveTrackSelection.Factory(BANDWIDTH_METER)
            trackSelector = DefaultTrackSelector(videoTrackSelectionFactory)
            // player init
            player = ExoPlayerFactory.newSimpleInstance(context, trackSelector)
            // todo: add listener
            player?.playWhenReady
            // player view
            playerView.player = player


            val httpDataSourceFactory = DefaultHttpDataSourceFactory(
                    Util.getUserAgent(context, "Applicaster Player"),
                    BANDWIDTH_METER,
                    DEFAULT_CONNECT_TIMEOUT_MILLIS,
                    DEFAULT_READ_TIMEOUT_MILLIS,
                    true)
            val mediaDataSourceFactory = DefaultDataSourceFactory(context, BANDWIDTH_METER, httpDataSourceFactory)
            val mediaSource = HlsMediaSource.Factory(mediaDataSourceFactory)
                    .createMediaSource(Uri.parse(firstPlayable.contentVideoURL))

            player?.prepare(mediaSource)
        }
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
