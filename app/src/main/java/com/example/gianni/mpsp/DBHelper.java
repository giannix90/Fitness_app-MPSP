package com.example.gianni.mpsp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.text.SimpleDateFormat;


/**
 * Created by gianni on 13/07/17.
 */

public class DBHelper extends SQLiteOpenHelper {

    private static final String TAG="DBHelper";

    private static final String DB_NAME = "Fitness.db";
    private static final int VERSION = 1;
    private Context mContext;

    public class ActivityRecord{
        private int walking_detect;
        private int running_detect;
        private int biking_detect;
        private long step;
        private int total_calories;
        private int walking_calories;
        private String data;

        public ActivityRecord(int walking_detect,int running_detect, int biking_detect,long  step,int total_calories,int  walking_calories,String data){
            this.walking_detect=walking_detect;
            this.running_detect=running_detect;
            this.biking_detect=biking_detect;
            this.step=step;
            this.total_calories=total_calories;
            this.walking_calories=walking_calories;
            this.data=data;
        }

        public int getWalking_detect(){
            return walking_detect;
        }

        public int getRunning_detect() {
            return running_detect;
        }

        public int getBiking_detect() {
            return biking_detect;
        }

        public long getStep() {
            return step;
        }

        public int getTotal_calories() {
            return total_calories;
        }

        public int getWalking_calories() {
            return walking_calories;
        }

        public String getData() {
            return data;
        }

    }


    public DBHelper(Context context)
    {
        super(context, DB_NAME, null, VERSION);
        mContext=context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        // I create te structure of database

        db.execSQL(
                "create table "+fitnessDB.User.TABLE+" " +
                        "("+fitnessDB.User.ID+" INTEGER PRIMARY KEY AUTOINCREMENT,"+fitnessDB.User.NAME+" TEXT NOT NULL,"+fitnessDB.User.GENDER+" TEXT NOT NULL,"+fitnessDB.User.AGE+" INTEGER NOT NULL,"+fitnessDB.User.WEIGHT+" INTEGER NOT NULL,"+fitnessDB.User.HEIGHT+" INTEGER NOT NULL);"
        );
        db.execSQL(
                "create table "+fitnessDB.Activity.TABLE+" " +
                        "("+fitnessDB.Activity.ID+" INTEGER PRIMARY KEY AUTOINCREMENT,"+fitnessDB.Activity.WALKING_DETECT+" INTEGER NOT NULL,"+fitnessDB.Activity.RUNNING_DETECT+" INTEGER NOT NULL,"+fitnessDB.Activity.BIKING_DETECT+" INTEGER NOT NULL,"+fitnessDB.Activity.STEP+" INTEGER NOT NULL,"+fitnessDB.Activity.TOTAL_CALORIES+" INTEGER NOT NULL,"+fitnessDB.Activity.WALKING_CALORIES+" INTEGER NOT NULL,"+fitnessDB.Activity.DATA+" TEXT NOT NULL);"
        );

        db.execSQL(
                "create table "+fitnessDB.Steps.TABLE+" " +
                        "("+fitnessDB.Steps.ID+" INTEGER PRIMARY KEY AUTOINCREMENT,"+fitnessDB.Steps.H0+" INTEGER NOT NULL,"+fitnessDB.Steps.H1+" INTEGER NOT NULL,"+fitnessDB.Steps.H2+" INTEGER NOT NULL,"+fitnessDB.Steps.H3+" INTEGER NOT NULL,"+fitnessDB.Steps.H4+" INTEGER NOT NULL,"+fitnessDB.Steps.H5+" INTEGER NOT NULL,"
                        +fitnessDB.Steps.H6+" INTEGER NOT NULL,"+fitnessDB.Steps.H7+" INTEGER NOT NULL,"+fitnessDB.Steps.H8+" INTEGER NOT NULL,"+fitnessDB.Steps.H9+" INTEGER NOT NULL,"+fitnessDB.Steps.H10+" INTEGER NOT NULL,"+fitnessDB.Steps.H11+" INTEGER NOT NULL,"+fitnessDB.Steps.H12+" INTEGER NOT NULL,"+fitnessDB.Steps.H13+" INTEGER NOT NULL,"
                        +fitnessDB.Steps.H14+" INTEGER NOT NULL,"+fitnessDB.Steps.H15+" INTEGER NOT NULL,"+fitnessDB.Steps.H16+" INTEGER NOT NULL,"+fitnessDB.Steps.H17+" INTEGER NOT NULL,"+fitnessDB.Steps.H18+" INTEGER NOT NULL,"+fitnessDB.Steps.H19+" INTEGER NOT NULL,"+fitnessDB.Steps.H20+" INTEGER NOT NULL,"+fitnessDB.Steps.H21+" INTEGER NOT NULL,"+fitnessDB.Steps.H22+" INTEGER NOT NULL,"+fitnessDB.Steps.H23+" INTEGER NOT NULL,"+fitnessDB.Steps.H24+" INTEGER NOT NULL);"
        );

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Code for update the DB, for simplicity just drop and recreate
        db.execSQL("DROP TABLE IF EXISTS "+fitnessDB.User.TABLE);
        db.execSQL("DROP TABLE IF EXISTS "+fitnessDB.Activity.TABLE);
        onCreate(db);
    }

    public void deleteDB(){
        SQLiteDatabase db = this.getWritableDatabase();

        db.delete(fitnessDB.User.TABLE,null,null);
        db.delete(fitnessDB.Activity.TABLE,null,null);
    }

    public boolean insertUser  (String Name, int Gender, int age, int weight, int height)
    {

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(fitnessDB.User.NAME, Name);
        contentValues.put(fitnessDB.User.GENDER, Gender==1?"Man":"Woman");
        contentValues.put(fitnessDB.User.AGE, age);
        contentValues.put(fitnessDB.User.WEIGHT, weight);
        contentValues.put(fitnessDB.User.HEIGHT, height);

        long r=0;
        if((int) DatabaseUtils.queryNumEntries(db,fitnessDB.User.TABLE)>=1)
            //If user was inserted previously i update it
            r=db.update(fitnessDB.User.TABLE, contentValues, "id = ? ", new String[] { "1" } );
        else
            //else i insert it
            r=db.insert(fitnessDB.User.TABLE, null, contentValues);

        return r!=-1;
    }

    public Cursor getUser(){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res=null;
        if((int) DatabaseUtils.queryNumEntries(db,fitnessDB.User.TABLE)>=1)
            res =  db.rawQuery( "select * from "+fitnessDB.User.TABLE+" where id="+1+"", null );
        return res;
    }

    public int numberOfRows(){
        //Number of rows in Activity table
        SQLiteDatabase db = this.getReadableDatabase();
        int numRows = (int) DatabaseUtils.queryNumEntries(db,fitnessDB.Activity.TABLE);
        return numRows;
    }

    public ActivityRecord getActivityOfTheDay() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = null;
        if ((int) DatabaseUtils.queryNumEntries(db, fitnessDB.Activity.TABLE) >= 1)
            res = db.rawQuery("select * from " + fitnessDB.Activity.TABLE + " where id=" + numberOfRows() + "", null);

        if (res != null) {
            res.moveToNext();

            if(new SimpleDateFormat("yyyy-MM-dd").format(new Date()).compareTo(res.getString(res.getColumnIndex(fitnessDB.Activity.DATA)))==0)
                //this means i have a session today
                return new ActivityRecord(res.getInt(res.getColumnIndex(fitnessDB.Activity.WALKING_DETECT)), res.getInt(res.getColumnIndex(fitnessDB.Activity.RUNNING_DETECT)), res.getInt(res.getColumnIndex(fitnessDB.Activity.BIKING_DETECT)), res.getInt(res.getColumnIndex(fitnessDB.Activity.STEP)), res.getInt(res.getColumnIndex(fitnessDB.Activity.TOTAL_CALORIES)), res.getInt(res.getColumnIndex(fitnessDB.Activity.WALKING_CALORIES)), res.getString(res.getColumnIndex(fitnessDB.Activity.DATA)));

        }
        return null;
    }

    public boolean insertActivity(int walking_detect,int running_detect,int biking_detect,long step,int total_calories,int  walking_calories,String data){

        ContentValues contentValues = new ContentValues();
        contentValues.put(fitnessDB.Activity.WALKING_DETECT, walking_detect);
        contentValues.put(fitnessDB.Activity.RUNNING_DETECT, running_detect);
        contentValues.put(fitnessDB.Activity.BIKING_DETECT, biking_detect);
        contentValues.put(fitnessDB.Activity.STEP, step);
        contentValues.put(fitnessDB.Activity.TOTAL_CALORIES, total_calories);
        contentValues.put(fitnessDB.Activity.WALKING_CALORIES, walking_calories);
        contentValues.put(fitnessDB.Activity.DATA, data);

        SQLiteDatabase db = this.getWritableDatabase();

        ActivityRecord mActivityRecord=getActivityOfTheDay();
        if(mActivityRecord!=null){
            if(mActivityRecord.getData().compareTo(data)==0){
                //means that we already have the record of the day
                //then we have to update it
                db.update(fitnessDB.Activity.TABLE, contentValues, "id = ? ", new String[] { String.valueOf(numberOfRows()) } );
            }
        }
        else {
            db.insert(fitnessDB.Activity.TABLE, null, contentValues);
        }

        return true;

    }

    public boolean updateActivity(String walking_detect,String running_detect, String biking_detect,String step,String total_calories,String walking_calories,String data){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(fitnessDB.Activity.WALKING_DETECT, walking_detect);
        contentValues.put(fitnessDB.Activity.RUNNING_DETECT, running_detect);
        contentValues.put(fitnessDB.Activity.BIKING_DETECT, biking_detect);
        contentValues.put(fitnessDB.Activity.STEP, step);
        contentValues.put(fitnessDB.Activity.TOTAL_CALORIES, total_calories);
        contentValues.put(fitnessDB.Activity.WALKING_CALORIES, walking_calories);
        contentValues.put(fitnessDB.Activity.DATA, data);

        //update the record of the day
        db.update(fitnessDB.Activity.TABLE, contentValues, "id = ? ", new String[] { String.valueOf(numberOfRows()) } );
        return true;
    }


    public Cursor getAllActivities(){
        String[] projection = new String[]
                {fitnessDB.Activity.TOTAL_CALORIES,fitnessDB.Activity.WALKING_CALORIES,fitnessDB.Activity.DATA};
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor mCursor = db.query(fitnessDB.Activity.TABLE,projection,null,null,null,null,null);

        return mCursor;
    }

    /*
    public ArrayList<materia> getAllVotes()
    {

        String[] projection = new String[]
                {votiDB.Voti.MATERIA,votiDB.Voti.VOTO,votiDB.Voti.CFU};

        /*
        //If we want selection with CFU>9
        String selection =
                votiDB.Voti.CFU + " > ?";
        String[] selectionArgs = new String[]{"9"};
        */
     /*   ArrayList<materia> array_list = new ArrayList<materia>();

        SQLiteDatabase db = this.getReadableDatabase();



        Cursor mCursor = db.query(votiDB.Voti.TABLE,projection,null,null,null,null,null);
        //Cursor mCursor =  db.rawQuery( "select * from Voti", null );
        //Because i want scroll all Relation
        //mCursor.moveToFirst();

        while(mCursor.moveToNext()){
            materia m=new materia(mCursor.getString(mCursor.getColumnIndex(votiDB.Voti.MATERIA)),mCursor.getInt(mCursor.getColumnIndex(votiDB.Voti.VOTO)),mCursor.getInt(mCursor.getColumnIndex(votiDB.Voti.CFU)));
            array_list.add(m);


        }

        return array_list;
    }
    */
}