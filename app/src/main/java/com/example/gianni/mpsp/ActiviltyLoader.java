package com.example.gianni.mpsp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.github.jlmd.animatedcircleloadingview.AnimatedCircleLoadingView;

import static android.R.attr.value;

/**
 * Created by gianni on 10/07/17.
 */

public class ActiviltyLoader extends AppCompatActivity {

    Context mContext=this;

    AnimatedCircleLoadingView mAnimatedCircleLoadingView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loader);
        new SingletonGlobalVariables().getSingleton().setActivityLoader(mContext);
        Log.d("ActivityLoader","Partita!!!!!!!!!!!!");

        mAnimatedCircleLoadingView= (AnimatedCircleLoadingView) findViewById(R.id.circle_loading_view);
        mAnimatedCircleLoadingView.startDeterminate();
        startPercentMockThread();
        //mAnimatedCircleLoadingView.setPercent(100);

        new Thread() {

            @Override
            public void run() {
                try {
                    super.run();
                    sleep(5000);  //Delay of 10 seconds
                } catch (Exception e) {

                } finally {

                    Intent i = new Intent(mContext,
                            MainActivity.class);
                    startActivity(i);
                    finish();
                }
            }
        }
        .start();

    }


    private void startPercentMockThread() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1500);
                    for (int i = 0; i <= 100; i++) {
                        Thread.sleep(25);
                        changePercent(i);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        new Thread(runnable).start();
    }

    private void changePercent(final int percent) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAnimatedCircleLoadingView.setPercent(percent);
            }
        });
    }

    public void resetLoading() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAnimatedCircleLoadingView.resetLoading();
            }
        });
    }


}
