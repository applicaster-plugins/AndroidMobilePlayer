package com.applicaster.mobile.player

import android.app.Dialog
import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import com.applicaster.mobile.player.utils.Constants.KEY_DEBUG_MODE
import com.applicaster.player.defaultplayer.BasePlayer
import com.applicaster.plugin_manager.playersmanager.Playable
import com.applicaster.plugin_manager.playersmanager.PlayableConfiguration
import com.applicaster.plugin_manager.playersmanager.PlayerContract
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.PlayerView
import com.applicaster.controller.PlayerLoader
import com.applicaster.mobile.player.utils.PlayerUtils
import com.applicaster.player.PlayerLoaderI
import android.support.v4.content.ContextCompat
import android.widget.FrameLayout
import com.google.android.exoplayer2.ui.PlaybackControlView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.applicaster.util.StringUtil
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter


/**
 * Big part of this achievement of making the inline player full screen is thanks to this
 * article https://geoffledak.com/blog/2017/09/11/how-to-add-a-fullscreen-toggle-button-to-exoplayer-in-android/
 */

class AndroidMobilePlayerAdaptor : BasePlayer(), PlayerLoaderI, PlayerController {

    private lateinit var playerView: PlayerView
    private var player: SimpleExoPlayer? = null
    private var loader: PlayerLoader? = null
    private var playable: Playable? = null
    // fullscreen implementation
    private var mFullScreenDialog: Dialog? = null
    private var mExoPlayerFullscreen = false
    private var videoContainerView: ViewGroup? = null
    private var mFullScreenIcon: ImageView? = null
    private var mFullScreenButton: FrameLayout? = null
    private val BANDWIDTH_METER = DefaultBandwidthMeter()

    override fun init(playable: Playable, context: Context) {
        this.init(listOf(playable), context)
    }

    override fun init(playableList: List<Playable>, context: Context) {
        super.init(playableList, context)
        initPlayer()
    }

    override fun init(context: Context) {
        super.init(context)
        initPlayer()
    }

    fun initPlayer() {
        val layoutInflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = layoutInflater.inflate(R.layout.inline_player_view, null)
        playerView = view.findViewById(R.id.player)
        initFullscreenDialog()
        initFullscreenButton()
    }

    fun initFullscreenDialog() {
        mFullScreenDialog = object : Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen) {
            override fun onBackPressed() {
                if (mExoPlayerFullscreen)
                    closeFullscreenDialog()
                super.onBackPressed()
            }
        }
    }

    private fun openFullscreenDialog() {
        (playerView.parent as ViewGroup).removeView(playerView)
        mFullScreenDialog?.addContentView(playerView, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        mFullScreenIcon?.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.icon_fullscreen_skrink))
        mExoPlayerFullscreen = true
        mFullScreenDialog?.show()
    }

    private fun closeFullscreenDialog() {
        (playerView.parent as ViewGroup).removeView(playerView)
        videoContainerView?.addView(playerView)
        mExoPlayerFullscreen = false
        mFullScreenDialog?.dismiss()
        mFullScreenIcon?.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.icon_fullscreen_expand))
    }

    private fun initFullscreenButton() {
        val controlView: PlaybackControlView = playerView.findViewById(R.id.exo_controller)
        mFullScreenIcon = controlView.findViewById(R.id.exo_fullscreen_icon)
        mFullScreenButton = controlView.findViewById(R.id.exo_fullscreen_button)
        mFullScreenButton?.setOnClickListener {
            if (!mExoPlayerFullscreen)
                openFullscreenDialog()
            else
                closeFullscreenDialog()
        }
    }

    override fun goFullScreen() {
        openFullscreenDialog()
    }

    override fun shrinkFullScreen() {
        closeFullscreenDialog()
    }

    override fun getPlayerType(): PlayerContract.PlayerType {
        return PlayerContract.PlayerType.Default
    }

    override fun playInFullscreen(configuration: PlayableConfiguration?, requestCode: Int, context: Context) {
        var debugMode = false
        pluginConfigurationParams?.let {
            debugMode = StringUtil.booleanValue(pluginConfigurationParams[KEY_DEBUG_MODE] as String)
        }
        AndroidMobilePlayerActivity.launchVideoActivity(context, firstPlayable, debugMode)
    }

    override fun attachInline(videoContainerView: ViewGroup) {
        super.attachInline(videoContainerView)
        videoContainerView.addView(playerView)
        this.videoContainerView = videoContainerView
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
            this.player = PlayerUtils().buildPlayer(context)
            playerView.player = player

            player?.prepare(PlayerUtils().buildMediaSource(context,
                    Uri.parse(playable?.contentVideoURL)))

            val httpDataSourceFactory = DefaultHttpDataSourceFactory(
                    Util.getUserAgent(context, "Applicaster Player"),
                    BANDWIDTH_METER,
                    DEFAULT_CONNECT_TIMEOUT_MILLIS,
                    DEFAULT_READ_TIMEOUT_MILLIS,
                    true)
            val mediaDataSourceFactory = DefaultDataSourceFactory(context, BANDWIDTH_METER, httpDataSourceFactory)
            val mediaSource = HlsMediaSource.Factory(mediaDataSourceFactory)
                    .createMediaSource(Uri.parse(it.contentVideoURL))

            player?.prepare(mediaSource)
        }
    }

    override fun removeInline(videoContainerView: ViewGroup) {
        super.removeInline(videoContainerView)
        videoContainerView.removeView(playerView)
        releasePlayer()
    }

    override fun stopInline() {
        super.stopInline()
        if (!mExoPlayerFullscreen) {
            releasePlayer()
        }
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
        return PlayerUtils().getApplicasterModelId(firstPlayable)
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
