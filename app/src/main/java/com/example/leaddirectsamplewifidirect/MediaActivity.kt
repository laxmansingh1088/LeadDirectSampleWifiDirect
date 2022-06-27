package com.example.leaddirectsamplewifidirect

import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.leaddirectsamplewifidirect.databinding.ActivityVideoPlayerBinding
import com.example.leaddirectsamplewifidirect.utils.FileType
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.MediaSourceFactory
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.util.Util
import java.io.File

class MediaActivity : AppCompatActivity() {

    private lateinit var simpleExoPlayer: ExoPlayer
    private lateinit var binding: ActivityVideoPlayerBinding
    private var fileType: FileType = FileType.VIDEO
    private var listFilePaths: ArrayList<String>? = null
    private var STREAM_URL = "http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4"

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        fetchDataFromIntent()
        setData()
        Log.d("VideoPlayerActivity", "OnNewIntent()---------------------------------")
    }

    private fun fetchDataFromIntent() {
        listFilePaths = intent.getStringArrayListExtra("filePathsList") as ArrayList<String>
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        fetchDataFromIntent()
        setData()
    }


   private fun setScreenOrientation(){
       if (STREAM_URL.contains(".mp4") || STREAM_URL.contains(".mp3")) {
           setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
       }  else  {
           setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
       }
   }


    private fun setData() {
        STREAM_URL = listFilePaths?.get(0).toString()
        setScreenOrientation()
        if (STREAM_URL.contains(".pdf")) {
            fileType = FileType.PDF
            binding.pdfView.visibility = View.VISIBLE
            binding.playerView.visibility = View.GONE
            binding.imageView.visibility = View.GONE
            showPdf()
        } else if (STREAM_URL.contains(".mp4") || STREAM_URL.contains(".mp3") || STREAM_URL.contains(
                ".gif"
            )
        ) {
            fileType = FileType.VIDEO
            binding.playerView.visibility = View.VISIBLE
            binding.pdfView.visibility = View.GONE
            binding.imageView.visibility = View.GONE
            if (fileType == FileType.VIDEO) {
                if (Util.SDK_INT > 23) initializePlayer()
            }
        } else if (STREAM_URL.contains(".jpg") || STREAM_URL.contains(".jpeg")) {
            fileType = FileType.IMAGE
            binding.imageView.visibility = View.VISIBLE
            binding.pdfView.visibility = View.GONE
            binding.playerView.visibility = View.GONE
            showImage()
        }
    }

    private fun showImage() {
        binding.imageView.setImageURI(Uri.fromFile(File(STREAM_URL)));
    }


    private fun initializePlayer() {

        val mediaDataSourceFactory: DataSource.Factory = DefaultDataSource.Factory(this)

        val mediaSource = ProgressiveMediaSource.Factory(mediaDataSourceFactory)
            .createMediaSource(MediaItem.fromUri(STREAM_URL))

        val mediaSourceFactory: MediaSourceFactory =
            DefaultMediaSourceFactory(mediaDataSourceFactory)

        if (!::simpleExoPlayer.isInitialized) {
            simpleExoPlayer = ExoPlayer.Builder(this)
                .setMediaSourceFactory(mediaSourceFactory)
                .build()
        }
        simpleExoPlayer.clearMediaItems()

        simpleExoPlayer.addMediaSource(mediaSource)

        simpleExoPlayer.playWhenReady = true
        simpleExoPlayer.getPlaybackState()
        // Prepare the player with the source.
        simpleExoPlayer.setMediaSource(mediaSource)
        simpleExoPlayer.prepare()
        binding.playerView.player = simpleExoPlayer
        binding.playerView.requestFocus()
    }

    private fun showPdf() {
        binding.pdfView.fromUri(Uri.fromFile(File(STREAM_URL)))
            .load()
    }

    private fun releasePlayer() {
        if (::simpleExoPlayer.isInitialized) {
            simpleExoPlayer.release()
        }
    }

    public override fun onStart() {
        super.onStart()
    }

    public override fun onResume() {
        super.onResume()
        if (fileType == FileType.VIDEO) {
            if (Util.SDK_INT <= 23) initializePlayer()
        }
    }

    public override fun onPause() {
        super.onPause()
        if (fileType == FileType.VIDEO) {
            if (Util.SDK_INT <= 23) releasePlayer()
        }
    }

    public override fun onStop() {
        super.onStop()
        if (fileType == FileType.VIDEO) {
            if (Util.SDK_INT > 23) releasePlayer()
        }
    }

}