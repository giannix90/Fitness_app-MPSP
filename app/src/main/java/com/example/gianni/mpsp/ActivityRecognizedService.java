package com.example.gianni.mpsp;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.util.List;

/**
 * Created by gianni on 28/03/17.
 *
 * When Google Play Services returns the user's activity, it will be sent to this IntentService.
 * This will allow you to perform your application logic in the background as the user goes about their day.
 */

public class ActivityRecognizedService extends IntentService {

    GlobalVariables mGlobalVariables;


    public ActivityRecognizedService() {
        super("ActivityRecognizedService");
    }

    public ActivityRecognizedService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        /*PARTIAL_WAKE_LOCK =>  Ensures that the CPU is running; the screen and keyboard backlight will be allowed to go off.*/
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SensorSamplingService");
        wakeLock.acquire();

        mGlobalVariables=new SingletonGlobalVariables().getSingleton();
        //For first we validate that the received Intent contains activity recognition data.
        if(ActivityRecognitionResult.hasResult(intent)) {

            //Then we extract the ActivityRecognitionResult from the Intent to see what activities your user might be performing.
            // We can retrieve a list of the possible activities by calling getProbableActivities() on the ActivityRecognitionResult object.
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            handleDetectedActivities( result.getProbableActivities() );
        }
    }

    private void handleDetectedActivities(List<DetectedActivity> probableActivities) {
        for( DetectedActivity activity : probableActivities ) {
            switch( activity.getType() ) {
                case DetectedActivity.IN_VEHICLE: {
                    Log.e( "ActivityRecogition", "In Vehicle: " + activity.getConfidence() );
                    break;
                }
                case DetectedActivity.ON_BICYCLE: {

                    mGlobalVariables.setActivityFounded(3);
                    Log.e( "ActivityRecogition", "On Bicycle: " + activity.getConfidence() );
                     if( activity.getConfidence() >= 50 ) {
                        //The probability that the user walk is 75% => very probable
                         mGlobalVariables.newBikingDetected();
                        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
                        builder.setContentText( "Are you on bike?" );
                        builder.setSmallIcon( R.drawable.cast_ic_notification_0 );
                        builder.setContentTitle( getString( R.string.app_name ) );
                        NotificationManagerCompat.from(this).notify(0, builder.build());
                    }
                    break;
                }
                case DetectedActivity.ON_FOOT: {
                    
                    Log.e( "ActivityRecogition", "On Foot: " + activity.getConfidence() );
                    break;
                }
                case DetectedActivity.RUNNING: {
                    Log.e( "ActivityRecogition", "Running: " + activity.getConfidence() );
                    if( activity.getConfidence() >= 50 ) {
                        mGlobalVariables.newRunningDetected();
                        //The probability that the user walk is 75% => very probable
                        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
                        builder.setContentText( "Are you running?" );
                        builder.setSmallIcon( R.drawable.cast_ic_notification_0 );
                        builder.setContentTitle( getString( R.string.app_name ) );
                        NotificationManagerCompat.from(this).notify(0, builder.build());
                    }
                    break;
                }
                case DetectedActivity.STILL: {
                    mGlobalVariables.setActivityFounded(0);
                    Log.e( "ActivityRecogition", "Still: " + activity.getConfidence() );
                    break;
                }
                case DetectedActivity.TILTING: {
                    Log.e( "ActivityRecogition", "Tilting: " + activity.getConfidence() );
                    break;
                }
                case DetectedActivity.WALKING: {

                    mGlobalVariables.newWalkingDetected();
                    mGlobalVariables.setActivityFounded(1);
                    /*
                    *  We can know how confident Google Play Services is that the user is performing that activity by calling
                    *  getConfidence() on a DetectedActivity instance.
                    *  If a confidence is 75 or higher, then it's safe to assume that the user is performing that activity.
                    * */
                    Log.e( "ActivityRecogition", "Walking: " + activity.getConfidence() );
                    if( activity.getConfidence() >= 40 ) {
                        //The probability that the user walk is 75% => very probable
                        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
                        builder.setContentText( "Are you walking?" );
                        builder.setSmallIcon( R.mipmap.ic_launcher );
                        builder.setContentTitle( getString( R.string.app_name ) );
                        NotificationManagerCompat.from(this).notify(0, builder.build());
                    }
                    break;
                }
                case DetectedActivity.UNKNOWN: {

                    mGlobalVariables.setActivityFounded(4);
                    Log.e( "ActivityRecogition", "Unknown: " + activity.getConfidence() );
                    break;
                }
            }
        }
    }
}
