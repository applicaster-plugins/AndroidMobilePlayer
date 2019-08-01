package com.applicaster.mobile.player.utils

import android.content.Context
import android.net.Uri
import com.applicaster.mobile.player.utils.Constants.APPLICASTER_PLAYER
import com.applicaster.mobile.player.utils.Constants.FORMAT_M3U8
import com.applicaster.mobile.player.utils.Constants.FORMAT_MP3
import com.applicaster.mobile.player.utils.Constants.FORMAT_MP4
import com.applicaster.model.APModel
import com.applicaster.plugin_manager.playersmanager.Playable
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.DynamicConcatenatingMediaSource
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelector
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.util.Util

class PlayerUtils {
    private val BANDWIDTH_METER = DefaultBandwidthMeter()

    fun buildPlayer(context: Context) : SimpleExoPlayer {
        val videoTrackSelectionFactory = AdaptiveTrackSelection.Factory(BANDWIDTH_METER)
        val trackSelector: TrackSelector = DefaultTrackSelector(videoTrackSelectionFactory)

        val player: SimpleExoPlayer = ExoPlayerFactory.newSimpleInstance(context, trackSelector)
        player.playWhenReady = true

        return player
    }

    fun buildMediaSource(context: Context, uri: Uri): MediaSource {
        val userAgent = Util.getUserAgent(context, APPLICASTER_PLAYER)

        val httpDataSourceFactory = DefaultHttpDataSourceFactory(
                userAgent,
                BANDWIDTH_METER,
                DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS,
                DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS,
                true)

        val mediaDataSourceFactory = DefaultDataSourceFactory(context, BANDWIDTH_METER, httpDataSourceFactory)
        return HlsMediaSource.Factory(mediaDataSourceFactory)
                .createMediaSource(uri)
    }

    fun getApplicasterModelId(playable: Playable): String? {
        var id: String? = null

        if (playable is APModel) {
            val model = playable as APModel
            id = model.id
        }

        return id
    }
}
