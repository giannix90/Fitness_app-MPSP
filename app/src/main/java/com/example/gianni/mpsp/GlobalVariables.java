package com.example.gianni.mpsp;

import android.content.Context;

/**
 * Created by gianni on 09/07/17.
 */

public class GlobalVariables {

    public static class User{
        public int Gender;
        public int Weight;
        public int Height;
        public int Age;

        public User(){ }

        public User(int Gender,int Weight, int Height, int Age){
            this.Gender=Gender;
            this.Weight=Weight;
            this.Height=Height;
            this.Age=Age;
        }
    }

    private FragmentOne FragmentOneContext; //Is used to pass context to SensorSamplingService

    private DBHelper mDBHelper;

    private int ActivityFounded; //This is the activity founded by ActivityRecognizedService by Google Services
    /*
    * 0 == Still
    * 1 == Walking
    * 2 == Running
    * 3 == Bike
    * 4 == Unknow
    * */

    private int mWalkingDetected;
    private int mRunningDetected;
    private int mBikingDetected;

    private Context mActivityLoader;

    public User usr;

    public GlobalVariables(){
        //Constructor
        usr=new User();
    }
    public User getUser(){
        return this.usr;
    }

    public void setmDBHelper(DBHelper mDBHelper){
        this.mDBHelper=mDBHelper;
    }

    public DBHelper getmDBHelper() {
        return mDBHelper;
    }

    public void setFragmentOneContext(FragmentOne mContext){
        this.FragmentOneContext=mContext;
    }

    public FragmentOne getFragmentOneContext(){
        return this.FragmentOneContext;
    }

    public void setActivityFounded(int activityFounded) {
        ActivityFounded = activityFounded;
    }

    public int getActivityFounded() {
        return ActivityFounded;
    }

    public void setActivityLoader(Context mContext){
        this.mActivityLoader=mContext;
    }

    public Context getmActivityLoader(){
        return mActivityLoader;
    }

    public void newRunningDetected(){
        this.mRunningDetected++;
    }

    public void newWalkingDetected(){
        this.mWalkingDetected++;
    }

    public void newBikingDetected(){
        this.mBikingDetected++;
    }


    public int getRunningDetected(){
        return this.mRunningDetected;
    }

    public int getWalkingDetected(){
        return this.mWalkingDetected;
    }

    public int getBikingDetected(){
        return this.mBikingDetected;
    }

    //This must be synchronized because we wont guarantee mutual exclusion

    public synchronized void setmBikingDetected(int mBikingDetected) {
        this.mBikingDetected = mBikingDetected;
    }

    public synchronized void setmRunningDetected(int mRunningDetected) {
        this.mRunningDetected = mRunningDetected;
    }

    public synchronized void setmWalkingDetected(int mWalkingDetected) {
        this.mWalkingDetected = mWalkingDetected;
    }

    public synchronized void resetnewRunningDetected(){
        this.mRunningDetected=0;
    }

    public synchronized void resetWalkingDetected(){
        this.mWalkingDetected=0;
    }

    public synchronized void resetBikingDetected(){
        this.mBikingDetected=0;
    }


}
