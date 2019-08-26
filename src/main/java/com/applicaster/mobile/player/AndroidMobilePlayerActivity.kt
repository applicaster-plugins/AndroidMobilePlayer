package com.applicaster.mobile.player

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.ActivityOptionsCompat
import android.util.Pair
import android.view.View
import android.view.View.VISIBLE
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import com.applicaster.analytics.AnalyticsAgentUtil
import com.applicaster.controller.PlayerLoader
import com.applicaster.mobile.player.utils.Constants.KEY_DEBUG_MODE
import com.applicaster.mobile.player.utils.Constants.KEY_DISABLE_OPTIONS
import com.applicaster.mobile.player.utils.Constants.PLAYABLE_ITEM_KEY
import com.applicaster.model.APChannel
import com.applicaster.model.APModel
import com.applicaster.player.PlayerLoaderI
import com.applicaster.player.VideoAdsUtil
import com.applicaster.plugin_manager.playersmanager.Playable
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.ext.ima.ImaAdsLoader
import com.google.android.exoplayer2.mediacodec.MediaCodecRenderer
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.source.ads.AdsMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
import com.google.android.exoplayer2.ui.DebugTextViewHelper
import com.google.android.exoplayer2.ui.PlaybackControlView
import com.google.android.exoplayer2.ui.PlayerControlView
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.util.ErrorMessageProvider
import com.google.android.exoplayer2.util.Util
import kotlinx.android.synthetic.main.activity_fullscreen_player.*


class AndroidMobilePlayerActivity : Activity(), PlayerLoaderI, PlayerControlView.VisibilityListener,
        PlaybackPreparer {

    private val BANDWIDTH_METER = DefaultBandwidthMeter()

    private var adsLoader: ImaAdsLoader? = null

    // there is something weird happening when assigning Playable
    private var playable: Playable? = null
    protected var loader: PlayerLoader? = null
    private var debugMode: Boolean = false
    private var disableOptions: Boolean = false

    private var player: SimpleExoPlayer? = null
    private var trackSelector: DefaultTrackSelector? = null
    private var debugViewHelper: DebugTextViewHelper? = null
    private var lastSeenTrackGroupArray: TrackGroupArray? = null
    private var startPosition: Long = 0
    private var playableLoaded = false
    private var controllerVisible: Boolean = false

    companion object {
        fun launchVideoActivity(context: Context, playable: Playable, debugMode: Boolean) {
            if (context is Activity) {
                val intent = Intent(context, AndroidMobilePlayerActivity::class.java)

                val bundle: Bundle? = ActivityOptionsCompat.makeSceneTransitionAnimation(context).toBundle()
                bundle?.let {
                    it.putSerializable(PLAYABLE_ITEM_KEY, playable)
                    it.putBoolean(KEY_DEBUG_MODE, debugMode)
                    intent.putExtras(it)
                }

                context.startActivity(intent)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN)

        setContentView(R.layout.activity_fullscreen_player)

        player_view.setControllerVisibilityListener(this)
        player_view.setErrorMessageProvider(PlayerErrorMessageProvider())
        player_view.requestFocus()

        debugMode = intent.getBooleanExtra(KEY_DEBUG_MODE, false)
        disableOptions = intent.getBooleanExtra(KEY_DISABLE_OPTIONS, false)
        playable = intent.getSerializableExtra(PLAYABLE_ITEM_KEY) as Playable

        hideFullScreenIcon()

        loadPlayable()
    }

    private fun hideFullScreenIcon() {
        val controlView: PlaybackControlView = player_view.findViewById(R.id.exo_controller)
        controlView.findViewById<ImageView>(R.id.exo_fullscreen_icon).visibility = View.GONE
    }

    private fun loadPlayable() {
        loader = PlayerLoader(this)
        loader?.loadItem()
    }

    private fun getApplicasterModelID(): String? {
        var id: String? = null

        if (playable is APModel) {
            val model = playable as APModel
            id = model.id
        }

        return id
    }

    private fun initializePlayer(restoringSession: Boolean) {
        val videoTrackSelectionFactory = AdaptiveTrackSelection.Factory(BANDWIDTH_METER)
        trackSelector = DefaultTrackSelector(videoTrackSelectionFactory)
        lastSeenTrackGroupArray = null

        player = ExoPlayerFactory.newSimpleInstance(this, trackSelector)
        player?.let { player ->
            player.addListener(PlayerEventListener(player, this))
            player.playWhenReady = true
            player.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING

            if (playable?.playableName != null) {
                playable_text_view.text = playable?.playableName
            }

            player_view?.let { playerView ->
                playerView.player = player
                playerView.setPlaybackPreparer(this)
                playerView.resizeMode = RESIZE_MODE_ZOOM
                playerView.setShowBuffering(true)
            }

            // Debug setup
            if (debugMode) {
                debug_text_view.visibility = VISIBLE
                debugViewHelper = DebugTextViewHelper(player, debug_text_view)
                debugViewHelper?.start()
            }

            val adUnit = VideoAdsUtil.getItemPreroll(playable?.playableId, playable!!.isLive, false)

            val httpDataSourceFactory = DefaultHttpDataSourceFactory(
                    Util.getUserAgent(this, "Applicaster Player"),
                    BANDWIDTH_METER,
                    DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS,
                    DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS,
                    true)

            val mediaDataSourceFactory = DefaultDataSourceFactory(this, BANDWIDTH_METER, httpDataSourceFactory)
            val mediaSource = HlsMediaSource.Factory(mediaDataSourceFactory)
                    .createMediaSource(Uri.parse(playable?.contentVideoURL))

            if (adUnit != null && !restoringSession) {
                adsLoader = ImaAdsLoader(this, Uri.parse(adUnit))
                val mediaSourceWithAds = AdsMediaSource(mediaSource, mediaDataSourceFactory, adsLoader, player_view?.overlayFrameLayout)
                player.prepare(mediaSourceWithAds)
            } else {
                player.prepare(mediaSource)
            }

            if (restoringSession) {
                player.seekTo(startPosition)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (player == null) {
            if (playableLoaded && !playable!!.isLive) {
                initializePlayer(true)
            }
        }
    }

    public override fun onPause() {
        super.onPause()
        AnalyticsAgentUtil.logPlayerEnterBackground()
        savePlayerState()
        releasePlayer()
    }

    public override fun onStop() {
        super.onStop()
        savePlayerState()
        releasePlayer()
    }

    private fun savePlayerState() {
        startPosition = Math.max(0, if (player != null) player!!.contentPosition else 0)
    }

    private fun releasePlayer() {

        player?.let {
            if (debugMode) {
                debugViewHelper?.stop()
                debugViewHelper = null
            }
            it.release()
            player = null
            trackSelector = null
        }

        adsLoader?.release()
    }

    override fun getItemId(): String? {
        return getApplicasterModelID()
    }

    override fun getPlayable(): Playable? {
        return playable
    }

    override fun onItemLoaded(playable: Playable?) {
        this.playable = playable
        initializeAnalyticsEvent()
        initializePlayer(false)
        playableLoaded = true
    }

    private fun initializeAnalyticsEvent() {
        val params: MutableMap<String, String> = playable?.analyticsParams as MutableMap<String, String>
        try {
            val channel = playable as APChannel
            params["Program Name"] = channel.next_program.name
        } catch (e: ClassCastException) {
            e.printStackTrace();
        }

        AnalyticsAgentUtil.generalPlayerInfoEvent(params)
        val eventName = if (playable!!.isLive) AnalyticsAgentUtil.PLAY_CHANNEL else AnalyticsAgentUtil.PLAY_VOD_ITEM
        AnalyticsAgentUtil.logTimedEvent(eventName, params)
    }

    override fun showMediaErroDialog() {
        // do nothing
    }

    override fun onVisibilityChange(visibility: Int) {
        controllerVisible = visibility == VISIBLE
        controls_root.visibility = visibility
    }

    override fun preparePlayback() {
        // do nothing
    }

    private class PlayerErrorMessageProvider : ErrorMessageProvider<ExoPlaybackException> {
        override fun getErrorMessage(e: ExoPlaybackException?): Pair<Int, String> {
            var errorString = "Generic Error"
            if (e?.type == ExoPlaybackException.TYPE_RENDERER) {
                val cause: Exception = e.rendererException
                if (cause is MediaCodecRenderer.DecoderInitializationException) {
                    // Special case for decoder initialization failures.
                    errorString = if (cause.decoderName == null) {
                        when {
                            cause.cause is MediaCodecUtil.DecoderQueryException -> "Decoder error"
                            cause.secureDecoderRequired -> "Decoder error"
                            else -> "Decoder error"
                        }
                    } else {
                        "Decoder error"
                    }
                }
            }
            return Pair.create(0, errorString);
        }
    }

    class PlayerEventListener(var player: Player, var activity: Activity) : Player.DefaultEventListener() {
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    AnalyticsAgentUtil.logBufferingStartEvent()
                }
                Player.STATE_READY -> {
                    AnalyticsAgentUtil.logBufferingEndEvent()
                    if (playWhenReady) {
                        AnalyticsAgentUtil.logPlayEvent(player.contentPosition)
                        AnalyticsAgentUtil.getInstance().enableVideoEvents()
                    } else {
                        AnalyticsAgentUtil.logPauseEvent(player.currentPosition)
                    }
                }
                Player.STATE_ENDED -> {
                    AnalyticsAgentUtil.logVideoEndEvent(player.currentPosition)
                    activity.finish()
                }
            }
        }

        override fun onPlayerError(error: ExoPlaybackException?) {
            AnalyticsAgentUtil.handlePlayerError(error?.message)
            activity.finish()
        }
    }
}
