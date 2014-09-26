package org.votingsystem.android.activity;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.votingsystem.android.R;
import org.votingsystem.android.fragment.MessageDialogFragment;
import org.votingsystem.model.ContextVS;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by jgzornoza on 26/09/14.
 */
public abstract class ActivityVS extends ActionBarActivity {

    public static final String TAG = ActivityVS.class.getSimpleName();

    private AtomicBoolean progressVisible = new AtomicBoolean(false);
    private View progressContainer;
    private FrameLayout mainLayout;
    /*public void showProgress(boolean showProgress, boolean animate);

    public void showMessage(Integer statusCode, String caption, String message);*/

    //@Override public boolean onKeyDown(int keyCode, KeyEvent event) {}

    public void initActivityVS(FrameLayout mainLayout, View progressContainer) {
        this.mainLayout = mainLayout;
        this.mainLayout.getForeground().setAlpha(0);
        this.progressContainer = progressContainer;
    }


    public void showMessage(Integer statusCode, String caption, String message) {
        Log.d(TAG + ".showMessage(...) ", "statusCode: " + statusCode + " - caption: " + caption +
                " - message: " + message);
        MessageDialogFragment newFragment = MessageDialogFragment.newInstance(statusCode, caption,
                message);
        newFragment.show(getSupportFragmentManager(), MessageDialogFragment.TAG);
    }

    public void showProgress(boolean showProgress, boolean animate) {
        //getActivity().setProgressBarIndeterminateVisibility(true); -> for title bar
        //getActivity().setProgressBarVisibility(true);
        if (progressVisible.get() == showProgress)  return;
        progressVisible.set(showProgress);
        if (progressVisible.get() && progressContainer != null) {
            getWindow().getDecorView().findViewById(android.R.id.content).invalidate();
            if (animate) {
                progressContainer.startAnimation(AnimationUtils.loadAnimation(
                        getApplicationContext(), android.R.anim.fade_in));
                //eventContainer.startAnimation(AnimationUtils.loadAnimation(
                //        this, android.R.anim.fade_out));
            }
            progressContainer.setVisibility(View.VISIBLE);
            //eventContainer.setVisibility(View.INVISIBLE);
            mainLayout.getForeground().setAlpha(150); // dim
            progressContainer.setOnTouchListener(new View.OnTouchListener() {
                //to disable touch events on background view
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return true;
                }
            });
        } else {
            if (animate) {
                progressContainer.startAnimation(AnimationUtils.loadAnimation(
                        getApplicationContext(), android.R.anim.fade_out));
                //eventContainer.startAnimation(AnimationUtils.loadAnimation(
                //        this, android.R.anim.fade_in));
            }
            progressContainer.setVisibility(View.GONE);
            //eventContainer.setVisibility(View.VISIBLE);
            mainLayout.getForeground().setAlpha(0); // restore
            progressContainer.setOnTouchListener(new View.OnTouchListener() {
                //to enable touch events on background view
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return false;
                }
            });
        }
    }

    public void showProgressMessage(String message) {
        TextView progressMessage = (TextView) progressContainer.findViewById(R.id.progressMessage);
        progressMessage.setText(getString(R.string.loading_data_msg));
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(ContextVS.LOADING_KEY, progressVisible.get());
    }
}
