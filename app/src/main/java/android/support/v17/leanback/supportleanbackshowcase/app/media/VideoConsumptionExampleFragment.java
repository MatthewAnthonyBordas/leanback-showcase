/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package android.support.v17.leanback.supportleanbackshowcase.app.media;

import android.app.Fragment;
import android.content.Intent;
import android.media.session.MediaSession;
import android.os.Bundle;
import android.support.v17.leanback.app.PlaybackOverlayFragment;
import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.PlaybackControlsRow;
import android.support.v17.leanback.widget.PlaybackControlsRowPresenter;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;


public class VideoConsumptionExampleFragment extends PlaybackOverlayFragment implements
        OnItemViewClickedListener, MediaPlayerGlue.OnMediaStateChangeListener {

    private static final String URL = "http://techslides.com/demos/sample-videos/small.mp4";
    public static final String TAG = "VideoConsumptionExampleFragment";
    private ArrayObjectAdapter mRowsAdapter;
    private VideoMediaPlayerGlue mGlue;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mGlue = new VideoMediaPlayerGlue(getActivity(), this) {

            @Override
            protected void onRowChanged(PlaybackControlsRow row) {
                if (mRowsAdapter == null) return;
                mRowsAdapter.notifyArrayItemRangeChanged(0, 1);
            }
        };



        Fragment videoSurfaceFragment = getFragmentManager()
                .findFragmentByTag(VideoSurfaceFragment.TAG);

        SurfaceView surface = (SurfaceView) videoSurfaceFragment.getView();
        surface.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                mGlue.setDisplay(holder);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                // Nothing to do
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                mGlue.resetPlayer();
                mGlue.releaseMediaSession();
                // Set a null surface holder to keep the mediaplayer state sane in between surfaceDestroyed
                // and the next call to surfaceCreated..
                // Otherwise, the mediaplayer will have an invalid state resulting from
                // invalid display state when the surface is destroyed. The invalid mediastate leads
                // to crash when returning to an stopped playback activity since onResume
                // calls prepareAsync on the mediaplayer while surfaceCreated hasn't been called yet.
                mGlue.setDisplay(null);
                mGlue.enableProgressUpdating(false);;
            }
        });
        setBackgroundType(PlaybackOverlayFragment.BG_LIGHT);
        addPlaybackControlsRow();
    }

    @Override
    public void onStart() {
        super.onStart();
        mGlue.enableProgressUpdating(mGlue.hasValidMedia() && mGlue.isMediaPlaying());
        mGlue.createMediaSessionIfNeeded();
    }

    @Override
    public void onResume() {
        super.onResume();
        MediaMetaData intentMetaData = getActivity().getIntent().getParcelableExtra(
                VideoExampleActivity.TAG);
        MediaMetaData currentMetaData = new MediaMetaData();
        if (intentMetaData != null) {
            currentMetaData.setMediaTitle(intentMetaData.getMediaTitle());
            currentMetaData.setMediaArtistName(intentMetaData.getMediaArtistName());
            currentMetaData.setMediaSourcePath(intentMetaData.getMediaSourcePath());
            currentMetaData.setMediaAlbumArtUrl(intentMetaData.getMediaAlbumArtUrl());
        } else {
            currentMetaData.setMediaTitle("Diving with Sharks");
            currentMetaData.setMediaArtistName("A Googler");
            currentMetaData.setMediaSourcePath(URL);
        }
        mGlue.setOnMediaFileFinishedPlayingListener(this);
        mGlue.prepareIfNeededAndPlay(currentMetaData);
    }

    @Override
    public void onPause() {
        // Enabling the video stay visible and play in the background when home screen is pressed.
        // (gregarious mode)
        if (mGlue.isMediaPlaying()) {
            boolean isVisibleBehind = getActivity().requestVisibleBehind(true);
            boolean isPictureInPictureMode = VideoExampleActivity.supportsPictureInPicture(
                    getContext()) && getActivity().isInPictureInPictureMode();
            if (!isVisibleBehind && !isPictureInPictureMode) {
                mGlue.pausePlayback();
            }
        } else {
            getActivity().requestVisibleBehind(false);
        }
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        mGlue.enableProgressUpdating(false);
        mGlue.resetPlayer();
        mGlue.releaseMediaSession();
        mGlue.saveUIState();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mGlue.releaseMediaPlayer();
    }

    private void addPlaybackControlsRow() {
        final PlaybackControlsRowPresenter controlsPresenter = mGlue
                .createControlsRowAndPresenter();
        mRowsAdapter = new ArrayObjectAdapter(controlsPresenter);
        mRowsAdapter.add(mGlue.getControlsRow());
        setAdapter(mRowsAdapter);
        setOnItemViewClickedListener(this);
    }

    @Override
    public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                              RowPresenter.ViewHolder rowViewHolder, Row row) {
        if (!(item instanceof Action)) return;
        mGlue.onActionClicked((Action) item);
    }


    @Override
    public void onMediaStateChanged(MediaMetaData currentMediaMetaData, int currentMediaState) {
        if (currentMediaState == MediaUtils.MEDIA_STATE_COMPLETED) {
            mGlue.startPlayback();
        }
    }
}
