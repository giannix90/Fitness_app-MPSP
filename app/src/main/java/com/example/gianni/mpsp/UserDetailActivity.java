package com.example.gianni.mpsp;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.dd.CircularProgressButton;
import com.klinker.android.sliding.SlidingActivity;

import static java.lang.Thread.sleep;

/**
 * Created by gianni on 08/07/17.
 */

public class UserDetailActivity extends SlidingActivity {

    private CircularProgressButton mCircularProgressButton;
    private Context mContext=this;
    private final String TAG="UserDEtailActivity";
    private CircularProgressButton age;
    private CircularProgressButton gender;
    private CircularProgressButton weigth;
    private CircularProgressButton height;
    private GlobalVariables mGlobalVariables;
    private TextView name;
    private EditText nameIns;
    private RadioGroup radioGroup;
    private RadioButton mRadio;

    private int mAge;
    private String mName;
    private String mGender;
    private int mWeight;
    private int mHeight;

    @Override
    public void init(Bundle savedInstanceState) {
        setTitle(R.string.app_name);
        setContent(R.layout.user_layout);

        // no need to set a color here, palette will generate colors for us to be set
        setImage(R.drawable.fitness_tracker_guide_cover_2);

        mGlobalVariables=new SingletonGlobalVariables().getSingleton();

        //Pointer to the buttons
        mCircularProgressButton=(CircularProgressButton) findViewById(R.id.btnWithText);
        age=(CircularProgressButton) findViewById(R.id.age);
        gender=(CircularProgressButton) findViewById(R.id.gender);
        weigth=(CircularProgressButton) findViewById(R.id.weight);
        height=(CircularProgressButton) findViewById(R.id.height);
        name=(TextView) findViewById(R.id.name_view);
        nameIns=(EditText) findViewById(R.id.name);

        //Get the user record from myDB
        Cursor mUser=mGlobalVariables.getmDBHelper().getUser();
        if(mUser!=null) {
            mUser.moveToNext();
            //Get the value from the record
            mName = mUser.getString(mUser.getColumnIndex(fitnessDB.User.NAME));
            mAge = mUser.getInt(mUser.getColumnIndex(fitnessDB.User.AGE));
            mGender=mUser.getString(mUser.getColumnIndex(fitnessDB.User.GENDER));
            mWeight = mUser.getInt(mUser.getColumnIndex(fitnessDB.User.WEIGHT));
            mHeight = mUser.getInt(mUser.getColumnIndex(fitnessDB.User.HEIGHT));

            //Set the text of the button
            name.setText(mName);
            age.setTextSize(10);
            age.setText(String.valueOf(mAge)+" years");
            gender.setTextSize(10);
            gender.setText(mGender);
            weigth.setTextSize(10);
            weigth.setText(String.valueOf(mWeight)+" Kg");
            height.setTextSize(10);
            height.setText(String.valueOf(mHeight)+" cm");

        }

        mCircularProgressButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                mCircularProgressButton.setIndeterminateProgressMode(true); // turn on indeterminate progress
                mCircularProgressButton.setProgress(50); // set progress > 0 & < 100 to display indeterminate progress
                new Thread(new Runnable() {

                    //Launch new thread in order to don't stop the main thread
                    @Override
                    public void run() {
                        try {
                            sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        //I have also to write the value on DB
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //Must be runned on main thread
                                mGlobalVariables.getmDBHelper().insertUser(nameIns.getText().toString(),(mGender.compareTo("Man")==0?1:2),mAge,mWeight,mHeight);
                                mCircularProgressButton.setProgress(100);

                            }
                        });

                    }
                }).start();

            }
        });


        age.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Dialog dialog1 = new Dialog(mContext);
                dialog1.setTitle("Add new Peer");


                LayoutInflater factory = LayoutInflater.from(mContext);
                View dialogView = factory.inflate(R.layout.age_layout,null);

                dialog1.setContentView(dialogView);

                final NumberPicker ed4=(NumberPicker) dialogView.findViewById(R.id.numberPick2); //Voto
                ed4.setMinValue(1);
                ed4.setMaxValue(120);

                Button mButton=(Button) dialogView.findViewById(R.id.addButton);

                mButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        age.setTextSize(10);
                        age.setText(String.valueOf(mAge=ed4.getValue())+" years");
                        dialog1.dismiss();//I close the dialog
                    }
                });


                dialog1.setTitle("Insert your age");

                try {
                    dialog1.show();
                }catch(Exception e){
                    //Handle error
                    Log.e(TAG,"Dialog Error");
                }
            }
        });

        weigth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Dialog dialog1 = new Dialog(mContext);
                dialog1.setTitle("Add weigth");


                LayoutInflater factory = LayoutInflater.from(mContext);
                View dialogView = factory.inflate(R.layout.age_layout,null);

                dialog1.setContentView(dialogView);

                final NumberPicker ed4=(NumberPicker) dialogView.findViewById(R.id.numberPick2); //Voto
                ed4.setMinValue(1);
                ed4.setMaxValue(250);

                Button mButton=(Button) dialogView.findViewById(R.id.addButton);

                mButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        weigth.setTextSize(10);
                        weigth.setText(String.valueOf(mWeight=ed4.getValue())+" Kg");
                        dialog1.dismiss();//I close the dialog
                    }
                });


                dialog1.setTitle("Insert your weigth");

                try {
                    dialog1.show();
                }catch(Exception e){
                    //Handle error
                    Log.e(TAG,"Dialog Error");
                }
            }
        });

        height.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Dialog dialog1 = new Dialog(mContext);
                dialog1.setTitle("Add height");


                LayoutInflater factory = LayoutInflater.from(mContext);
                View dialogView = factory.inflate(R.layout.age_layout,null);

                dialog1.setContentView(dialogView);

                final NumberPicker ed4=(NumberPicker) dialogView.findViewById(R.id.numberPick2); //Voto
                ed4.setMinValue(1);
                ed4.setMaxValue(250);

                Button mButton=(Button) dialogView.findViewById(R.id.addButton);

                mButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        height.setTextSize(10);
                        height.setText(String.valueOf(mHeight=ed4.getValue())+" cm");
                        dialog1.dismiss();//I close the dialog
                    }
                });


                dialog1.setTitle("Insert your heigth");

                try {
                    dialog1.show();
                }catch(Exception e){
                    //Handle error
                    Log.e(TAG,"Dialog Error");
                }
            }
        });


        gender.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View view) {
             final Dialog dialog1 = new Dialog(mContext);
             dialog1.setTitle("Choose Gender");


             LayoutInflater factory = LayoutInflater.from(mContext);
             final View dialogView = factory.inflate(R.layout.gender_layout,null);

             dialog1.setContentView(dialogView);

             Button mButton=(Button) dialogView.findViewById(R.id.addButton);
             radioGroup=(RadioGroup) dialogView.findViewById(R.id.radioGroup);

             mButton.setOnClickListener(new View.OnClickListener() {
                 @Override
                 public void onClick(View view) {

                     //Get id of selected radio button from group
                     int selectedId=radioGroup.getCheckedRadioButtonId();
                     //get the pointer to radio button
                     mRadio=(RadioButton) dialogView.findViewById(selectedId);

                     gender.setTextSize(10);
                     try {
                         gender.setText(mGender = (String) mRadio.getText());
                     }catch (Exception e){
                         //NullPointer exception due to non selected radio button
                     }
                     dialog1.dismiss();//I close the dialog
                 }
             });
             dialog1.setTitle("Insert your gender");

             try {
                 dialog1.show();
             }catch(Exception e){
                 //Handle error
                 Log.e(TAG,"Dialog Error");
             }

         }
     });


    }

}