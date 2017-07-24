
package com.example.gianni.mpsp;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.*;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InterfaceAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

public class SensorSamplingService extends Service
{

	public interface Result{
		void onNewCalories(double Calories);
		void onStepDetected(final AtomicInteger stepNumber);
		void onNewFItBitCalories(double Calories);
		void onNewKmWalked(double Km);
		void onNewStepsGroupOfTheDay(int[] mSteps);
	}

	private static FragmentOne fragmentContext;
	private Result mResult;
	private Context mContext=this;

	private static final String TAG = "SensorSamplingService";
	private static final int SAMPLING_INTERVAL = 100;                  // sampling interval [ms]
	private static final int SAMPLING_FREQ = 1000 / SAMPLING_INTERVAL; // sampling freq [Hz]
	private static final int SENSOR_TYPE_HEARTRATE_GEAR_LIVE = 65562;  // Samsung Gear Live custom HB sensor

	private volatile float[] lastAcc = new float[] {0, 0, 0};
	private volatile float lastStepCount = 0;

	ChenEuristicEEEFormula mEEE;


    private double[] mCaloriesWIndows=new double[10]; //Windows of 10 sample => 1 second becouse of 10Hz sampling rate
    private int windowIndex;

	public static double mCalories;
	private static double mCaloriesFitBit;
	private static double mCaloriesDuringWalking;
	private static AtomicInteger mStep;
	private static int[] mStepDuringDay=new int[24];//24 hours for each day

	private static int samples = 0;
	private static float realSamplingFrequency = 0;
	private long tsInit;
	private long lastUptime;
	private SensorManager mSensorManager;
	private Sensor mAccelerationSensor;
	private Sensor mStepSensor;
	private SensorEventListener selAcc = null;
	private SensorEventListener selStep = null;
	private PowerManager powerManager;
	private PowerManager.WakeLock wakeLock;
	private BufferedWriter bw = null; //Is the buffer writer associated to the logfile and used to buffering data acquired by accelerometer
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss.SSS", Locale.ENGLISH);

	// ADDITIONAL SENSORS, ENABLE IF NEEDED (!) modify the header and format strings accordingly
/*	private Sensor mGyroscopeSensor;
	private Sensor mOrientationSensor;
	private Sensor mHeartRateSensor;
	private Sensor mStepSensor;
	private volatile float[] lastGyro = new float[] {0, 0, 0};
	private volatile float[] lastOrientation = new float[] {0, 0, 0};
	private volatile float lastStepCount = 0;
	private SensorEventListener selGyroscope = null;
	private SensorEventListener selOrientation = null;
	private SensorEventListener selStep = null;
*/
	private static final String LOG_HEADER = "TS,  BPM, AccX, AccY, AccZ\r\n";
	private static final String LOG_FORMAT = "%d,%3.1f,%3.3f,%3.3f,%3.3f\r\n";

	private static volatile boolean isRunning = false;
	private static volatile boolean isStarted = false;

	FirAveragingFilter mFIRx; //Pointer for the FIR filter for Ax
    FirAveragingFilter mFIRy; //Pointer for the FIR filter for Ay
    FirAveragingFilter mFIRz; //Pointer for the FIR filter for Az
	int N=4; //This is the size of the window of the FIR

	ScheduledExecutorService schedulerDB;

	DBHelper.ActivityRecord mActivtyRecord;

	GlobalVariables mGlobalVariables;

	CaloriesRandomForestClassifier mCaloriesRandomForestClassifier;//For detect the activity


	/*
	* A Handler allows you to send and process Message and Runnable objects associated with a thread's MessageQueue.
	 * Each Handler instance is associated with a single thread and that thread's message queue. When you create a new Handler,
	  * it is bound to the thread / message queue of the thread that is creating it -- from that point on, it will deliver messages
	  * and runnables to that message queue and execute them as they come out of the message queue.
		There are two main uses for a Handler: (1) to schedule messages and runnables to be executed as some point in the future;
		 and (2) to enqueue an action to be performed on a different thread than your own.
		 */
	private Handler handler;
	private HandlerThread t = new HandlerThread("MyThread");

	/*This is the task that run periodically and write the data acquired by sensors into log file*/
	private Runnable writeTask = new Runnable()
	{
		@Override
		public void run()
		{
			//Log.e("Handler ","avviato");
			if (isRunning)
			{
				lastUptime += SAMPLING_INTERVAL;

				/*
				handler.postAtTime(r, uptimeMillis);

				Causes the Runnable r to be added to the message queue, to be run at a specific time given by uptimeMillis.
				 The time-base is uptimeMillis(). Time spent in deep sleep will add an additional delay to execution.
				 The runnable will be run on the thread to which this handler is attached.
				* */

				handler.postAtTime(writeTask, lastUptime);
			}


			mResult=(Result) mGlobalVariables.getFragmentOneContext();//I bind the address of FragmentOne continuosly becouse if Fragment close i need to retrive again the address to call the callback

			long ts = System.currentTimeMillis();

			double c=mFIRx.filter(lastAcc[0]);
            mCaloriesWIndows[windowIndex]=mEEE.getEEE(c,mFIRy.filter(lastAcc[1]),mFIRz.filter(lastAcc[2]));
            windowIndex++;

			if(mResult!=null)
				mResult.onStepDetected(mStep);

            if(windowIndex==9) {
				//I've reached 1 second
				windowIndex = 0;//Restart window

				try {

                    //I have to guarantee mutual exclusion because of multithreading environment
                    synchronized (this) {

                        mCalories += _getCaloriesMean(mCaloriesWIndows);

                        //Call the callback into the activity
                    }

					if(mResult!=null)
						mResult.onNewCalories(mCalories);

					if(mGlobalVariables.getActivityFounded()==1 || mGlobalVariables.getActivityFounded()==2 || mGlobalVariables.getActivityFounded()==3){

						mCaloriesDuringWalking+=_getCaloriesMean(mCaloriesWIndows);
						//mResult.onNewCaloriesDuringWalking(mCaloriesDuringWalking);
					}

				}catch (Exception e){
					//Catch any exception
				}
			}

			/*

			if (bw != null)
			{
				try {
					//Flush to the file the data
					//bw.write(String.format(Locale.ENGLISH, LOG_FORMAT,ts,1.12,lastAcc[0], c, lastAcc[2]));
					bw.write(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date())+"--#Running: "+mGlobalVariables.getRunningDetected()+"--#Walking: "+mGlobalVariables.getWalkingDetected()+"#Biking: "+mGlobalVariables.getBikingDetected()+"#TotCalories: "+mCalories+"# Step: "+mStep);
					samples++;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			*/
			realSamplingFrequency = 1000.0f / ((float)(ts - tsInit) / (float)samples);
		}
	};

	public SensorSamplingService()
	{
	}

	private double _getCaloriesMean(double[] mCaloriesWIndows){

        long sum=0;

        for(int i=0;i<9;i++){
            sum+=mCaloriesWIndows[i];

        }
        //Log.d("Service","sum: "+sum);
        double mean=((double) sum)/((double)9);

		//I saturate the Kcal for second to 0.333333 because is the energy spent at 18 km/h running
        if (mean<0.533333)
            return mean;
        else return 0.533333;
    }

	private double getKmWalked(){
		double Km=0;
		Cursor mUser=mGlobalVariables.getmDBHelper().getUser();
		if(mUser!=null) {
			mUser.moveToNext();
			//Get the value from the record
			int mHeight = mUser.getInt(mUser.getColumnIndex(fitnessDB.User.HEIGHT));

			if(mUser.getString(mUser.getColumnIndex(fitnessDB.User.GENDER)).compareTo("Man")==0){

				Km=((double)mHeight*0.415*(double)mStep.get())/100000;// divided by 100000 becouse mHeight must be in meters and km in kilometers

			}else{
				Km=(((double)mHeight)*0.413*(double)mStep.get())/100000;// divided by 100000 becouse mHeight must be in meters and km in kilometers

			}
		}
		return Km;
	}

	public static void setFragmentContext(FragmentOne mContext){
        fragmentContext=mContext;
    }

	public static boolean isStarted ()
	{
		return isStarted;
	}

	public static int getSamples ()
	{
		return samples;
	}

	public static double getmCalories(){return mCalories;}

	public static float getRealSamplingFrequency ()
	{
		return realSamplingFrequency;
	}

	private boolean getUserInfoFromDb(){
		//Get the user record from myDB
		Cursor mUser=mGlobalVariables.getmDBHelper().getUser();
		if(mUser!=null) {
			mUser.moveToNext();
			//Get the value from the record
			mGlobalVariables.getUser().Age = mUser.getInt(mUser.getColumnIndex(fitnessDB.User.AGE));
			mGlobalVariables.getUser().Gender= ((mUser.getString(mUser.getColumnIndex(fitnessDB.User.GENDER)).compareTo("Man")==0)?1:0);
			mGlobalVariables.getUser().Weight = mUser.getInt(mUser.getColumnIndex(fitnessDB.User.WEIGHT));
			mGlobalVariables.getUser().Height = mUser.getInt(mUser.getColumnIndex(fitnessDB.User.HEIGHT));

			return true;
		}
		return false;

	}

	private static void insertStepinStepDuringDay(){

		//get current hour
		int currentHour=Integer.parseInt(new SimpleDateFormat("HH").format(new Date()));


		if(currentHour==24)
			currentHour=0;

		mStepDuringDay[currentHour]++;
	}

	private void setResetValuesPeriodicThread(){

		/*
		* 	This thread is used because i have to reset the Activity values to 0 each day, in order to restart the count
		 * 	of step, calories ecc
		* */

		Calendar calendar = Calendar.getInstance();

		// Set time of execution. Here, we have to run every day 0:01 AM; so,
		// setting all parameters.
		calendar.set(Calendar.HOUR, 0);
		calendar.set(Calendar.MINUTE, 1);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.AM_PM, Calendar.AM);

		Long currentTime = new Date().getTime();

		// Check if current time is greater than our calendar's time. If So,
		// then change date to one day plus. As the time already pass for
		// execution.
		if (calendar.getTime().getTime() < currentTime) {
			calendar.add(Calendar.DATE, 1);
		}

		// Calendar is scheduled for future; so, it's time is higher than
		// current time.
		long startScheduler = calendar.getTime().getTime() - currentTime;


		Log.e("Service","Il thread  partirà tra"+startScheduler+"minuti"+calendar.getTime().getMinutes());

		// Get an instance of scheduler
		final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
		// execute scheduler at fixed time.
		scheduler.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				//I have to reset all values
				mStep.set(0);

                synchronized (this) {

                    mCalories = 0;
                    mCaloriesDuringWalking = 0;
                    mGlobalVariables.resetBikingDetected();
                    mGlobalVariables.resetnewRunningDetected();
                    mGlobalVariables.resetWalkingDetected();

                }
			}
		}, startScheduler/1000, 86400, SECONDS);

		//We have 86400 second in a day
	}


	private void setWriteFeaturesPeriodicThread(){

		//This function lauch a periodic thread scheduled at fixed rate every day at 23:00 pm

		// Create a calendar instance
		Calendar calendar = Calendar.getInstance();

		// Set time of execution. Here, we have to run every day 4:20 PM; so,
		// setting all parameters.
		calendar.set(Calendar.HOUR, 11);
		calendar.set(Calendar.MINUTE, 59);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.AM_PM, Calendar.PM);

		Long currentTime = new Date().getTime();

		// Check if current time is greater than our calendar's time. If So,
		// then change date to one day plus. As the time already pass for
		// execution.
		if (calendar.getTime().getTime() < currentTime) {
			calendar.add(Calendar.DATE, 1);
		}

		// Calendar is scheduled for future; so, it's time is higher than
		// current time.
		long startScheduler = calendar.getTime().getTime() - currentTime;


		Log.e("Service","Il thread partirà tra"+startScheduler+"minuti"+calendar.getTime().getMinutes());

		// Get an instance of scheduler
		final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
		// execute scheduler at fixed time.
		scheduler.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {

					try {
						//Flush to the file the data
						//bw.write(String.format(Locale.ENGLISH, LOG_FORMAT,ts,1.12,lastAcc[0], c, lastAcc[2]));
						Log.e("Service","Thread Partito");
						//bw.write(new SimpleDateFormat("yyyy/MM/d HH:mm:ss").format(new Date())+"--#Running: "+mGlobalVariables.getRunningDetected()+"--#Walking: "+mGlobalVariables.getWalkingDetected()+"#Biking: "+mGlobalVariables.getBikingDetected()+"#TotCalories: "+mCalories+"# Step: "+mStep+"\n");
						//bw.flush();
						FileWriter fw=new FileWriter(Environment.getExternalStorageDirectory() + File.separator + "logs" + File.separator + "ActivitiesTrainingSet" + ".arff",true);
						fw.write("\n"+new SimpleDateFormat("yyyy/MM/d HH:mm:ss").format(new Date())+" "+mGlobalVariables.getRunningDetected()+","+mGlobalVariables.getWalkingDetected()+","+mGlobalVariables.getBikingDetected()+","+(int)mCalories+","+mStep.get()+",?");
						fw.flush();
						fw.close();
						samples++;
					} catch (IOException e) {
						e.printStackTrace();
					}

			}
		}, startScheduler/1000, 86400, SECONDS);

		//We have 86400 second in a day
	}

	private void setWriteDataIntoDbPeriodicThread(){

		//every 5 SECONDS i write the current value op parameter into my fitnessDB.Activity table
		schedulerDB = Executors.newScheduledThreadPool(1);
		schedulerDB.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {

				//Log.e("SchedulerDB: ", "avviato");
				double Km=getKmWalked();
				//double Km=((double)mStep.get()*0.415*(double)192)/1000;

				//Prepare the input instance for the random forest classifier
				FileWriter fw= null;
				try {
					fw = new FileWriter(Environment.getExternalStorageDirectory() + File.separator + "RandomForestClassifier" + File.separator + "calorieInstancy" + ".arff",false);
					fw.write("@relation whatever\n"+

					"@attribute Passi numeric\n"+
					"@attribute Distanza numeric\n"+
					"@attribute Calorieattività numeric\n\n"+


					"@data\n"+mStep.get()+" ,"+Km+" , ?");
					fw.flush();
					fw.close();

				} catch (IOException e) {
					e.printStackTrace();
				}

				mCaloriesFitBit=mCaloriesRandomForestClassifier.classify();

				if(mResult!=null)
					mResult.onNewFItBitCalories(mCaloriesFitBit);

				//Log.e(TAG,"Inserisco i dati dell'Activity of User nel DB");

				if (mGlobalVariables.getmDBHelper()==null) //If i lost bind with DB i recreate
					mGlobalVariables.setmDBHelper(new DBHelper(mContext));

				mGlobalVariables.getmDBHelper().insertActivity(mGlobalVariables.getWalkingDetected(),mGlobalVariables.getRunningDetected(),mGlobalVariables.getBikingDetected(),mStep.get(),(int)mCalories,(int)mCaloriesFitBit,getCurrentData());
				//Log.e(TAG,"Dati inseriti nel Db");

				try{
					mResult.onNewKmWalked(Km);
					mResult.onNewStepsGroupOfTheDay(mStepDuringDay);
				}catch (Exception e ){
					//Something goes wrong, for ex mResult==null
					e.printStackTrace();
					Log.e(TAG,"Thread DB crashed");
				}
				//Log.e(TAG,"Thread Db completato");
			}
		},5000,5000,MILLISECONDS);
	}

	private String getCurrentData(){
		//Log.e("New Data",new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
		return new SimpleDateFormat("yyyy-MM-dd").format(new Date());
	}

	@Override
	public IBinder onBind(Intent intent)
	{
		return null; // service is unbounded so no need to bind
	}

	@Override
	public void onCreate ()
	{
		super.onCreate();
		Log.i(TAG, "onCreate()");

		mGlobalVariables=new SingletonGlobalVariables().getSingleton();

		mGlobalVariables.setmDBHelper(new DBHelper(this));

		//Retrive the RecordActivity of the day
		mActivtyRecord=mGlobalVariables.getmDBHelper().getActivityOfTheDay();
		if(mActivtyRecord!=null) {
			if (mActivtyRecord.getData().compareTo(getCurrentData()) == 0) {
				//I already have a session for this day, i fill the parameter
				mStep = new AtomicInteger((int) mActivtyRecord.getStep());
				mCalories = mActivtyRecord.getTotal_calories();
				mCaloriesDuringWalking = mActivtyRecord.getWalking_calories();
				mGlobalVariables.setmBikingDetected(mActivtyRecord.getBiking_detect());
				mGlobalVariables.setmWalkingDetected(mActivtyRecord.getWalking_detect());
				mGlobalVariables.setmRunningDetected(mActivtyRecord.getRunning_detect());
			}
		}else {
            mStep = new AtomicInteger(0);
        }

		mCaloriesRandomForestClassifier=new CaloriesRandomForestClassifier();

		/*
		* 	Initialize periodic thread that write Activity data into DB
		* */
		setWriteDataIntoDbPeriodicThread();

		/*
			Initialize Thread to write every day the features for RandomForestClassifier
		*/
		setWriteFeaturesPeriodicThread();

		setResetValuesPeriodicThread();

		getUserInfoFromDb();

		//Instantiate the FirAveragingFilterObject
		mFIRx=new FirAveragingFilter(N);
        mFIRy=new FirAveragingFilter(N);
        mFIRz=new FirAveragingFilter(N);


        //Instantiate the ChenEuristicEEEFormula Object
		mEEE=new ChenEuristicEEEFormula(mGlobalVariables.getUser().Gender,mGlobalVariables.getUser().Weight,mGlobalVariables.getUser().Age,mGlobalVariables.getUser().Height);

		mResult=(Result) fragmentContext;

		//I update mCalories DuringWalking on main UI
		mResult.onNewFItBitCalories(mCaloriesDuringWalking);


		mSensorManager = ((SensorManager)getSystemService(SENSOR_SERVICE));

		/*for (Sensor s : mSensorManager.getSensorList(Sensor.TYPE_ALL)) // print all the available sensors
		{
			Log.d(TAG, "Sensor: " + s.getName() + ", type = " + s.getType());
		}*/

		mAccelerationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
		selAcc = new SensorEventListener() {
			@Override
			public void onSensorChanged(SensorEvent sensorEvent) {
				lastAcc = sensorEvent.values;
			}

			@Override
			public void onAccuracyChanged(Sensor sensor, int i) {}
		};
		mSensorManager.registerListener(selAcc, this.mAccelerationSensor, SAMPLING_INTERVAL * 750); // sampling interval * 0.75

		mStepSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
		selStep = new SensorEventListener() {
			@Override
			public void onSensorChanged(SensorEvent sensorEvent) {
				//if(mGlobalVariables.getActivityFounded()==1 || mGlobalVariables.getActivityFounded()==2)
					mStep.getAndIncrement();
					//Insert step in current correct hour of the day
					insertStepinStepDuringDay();

				lastStepCount = sensorEvent.values[0];
			}
			@Override
			public void onAccuracyChanged(Sensor sensor, int i) {}
		};
		mSensorManager.registerListener(selStep, this.mStepSensor, 1000000); // Forced to 1s

		// ENABLE IF REQUIRED
/*		mGyroscopeSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
		selGyroscope = new SensorEventListener() {
			@Override
			public void onSensorChanged(SensorEvent sensorEvent) { lastGyro = sensorEvent.values; }
			@Override
			public void onAccuracyChanged(Sensor sensor, int i) {}
		};
		mSensorManager.registerListener(selGyroscope, this.mGyroscopeSensor, SAMPLING_INTERVAL * 1000);
		mOrientationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
		selOrientation = new SensorEventListener() {
			@Override
			public void onSensorChanged(SensorEvent sensorEvent) { lastOrientation = sensorEvent.values; }
			@Override
			public void onAccuracyChanged(Sensor sensor, int i) {}
		};
		mSensorManager.registerListener(selOrientation, this.mOrientationSensor, SAMPLING_INTERVAL * 1000);
		mStepSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
		selStep = new SensorEventListener() {
			@Override
			public void onSensorChanged(SensorEvent sensorEvent) { lastStepCount = sensorEvent.values[0]; }
			@Override
			public void onAccuracyChanged(Sensor sensor, int i) {}
		};
		mSensorManager.registerListener(selStep, this.mStepSensor, 1000000); // Forced to 1s
*/

		/*Create the new logfile*/
		File logFile = new File(Environment.getExternalStorageDirectory() + File.separator + "logs" + File.separator + "ActivitiesTrainingSet" + ".arff");
		try {
			logFile.getParentFile().mkdirs();
			logFile.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}



		/*
		try {
			Log.d (TAG, "Log saved to : " + logFile.getAbsolutePath());
			bw = new BufferedWriter(new FileWriter(logFile)); //I associate the BufferedWriter bw to the logFile
			bw.write("Instancies");
			bw.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
		*/
		/*PARTIAL_WAKE_LOCK =>  Ensures that the CPU is running; the screen and keyboard backlight will be allowed to go off.*/
		powerManager = (PowerManager) getSystemService(POWER_SERVICE);
		wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SensorSamplingService");
	}

	@Override
	public int onStartCommand (Intent intent, int flags, int startId)
	{
		Log.i(TAG, "onStartCommand()");

		isStarted = true;
		wakeLock.acquire(); //CPU will remain on

		Notification notification = new NotificationCompat.Builder(this)
				.setContentTitle("Logger")
				.setContentText("Logger").build();
		startForeground(101, notification);

		samples = 0;
		isRunning = true;
		if(!t.isAlive())
			t.start();

		handler = new Handler(t.getLooper());
		tsInit = System.currentTimeMillis();
		lastUptime = SystemClock.uptimeMillis() + SAMPLING_INTERVAL;
		handler.postAtTime(writeTask, lastUptime);

		return START_STICKY;
	}

	@Override
	public void onDestroy ()
	{
		Log.i(TAG, "onDestroy()");

		isRunning = false;
		t.quit();

		stopForeground(true);

		if (wakeLock != null)
			if (wakeLock.isHeld())
				wakeLock.release();

		if (bw != null) {
			try {
				bw.close();
				Log.d (TAG, "File closed");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		if (selStep != null)
			mSensorManager.unregisterListener(selStep);
		if (selAcc != null)
			mSensorManager.unregisterListener(selAcc);
/*		if (selGyroscope != null)
			mSensorManager.unregisterListener(selGyroscope);
		if (selOrientation != null)
			mSensorManager.unregisterListener(selOrientation);

*/
		isStarted = false;

		schedulerDB.shutdown();//Shutdown the periodic thread used to write into DB every 5 seconds
		t.quit();

		super.onDestroy();
	}
}