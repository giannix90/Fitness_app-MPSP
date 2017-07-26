package com.example.gianni.mpsp;


import android.app.Dialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.dexafree.materialList.card.Card;
import com.dexafree.materialList.card.CardProvider;
import com.dexafree.materialList.card.OnActionClickListener;
import com.dexafree.materialList.card.action.TextViewAction;
import com.dexafree.materialList.view.MaterialListView;
import com.everseat.textviewlabel.TextViewLabel;

import org.eazegraph.lib.charts.StackedBarChart;
import org.eazegraph.lib.models.BarModel;
import org.eazegraph.lib.models.StackedBarModel;

import java.text.DecimalFormat;
import java.util.ArrayList;
import im.dacer.androidcharts.LineView;
import static android.R.attr.value;
import static java.lang.Thread.sleep;

/**
 * Created by gianni on 07/07/17.
 */

public class FragmentTwo extends Fragment {
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";
    private MaterialListView mListView;

    GlobalVariables mGlobalVariables;
    ArrayList<Integer> caloriesChenEEEList;
    ArrayList<Integer> caloriesFitBitList;
    ArrayList<String> datas;
    ArrayList<ArrayList<Integer>> caloriesLists;
    MaterialListView mInfoTabView;
    Dialog graphDialog;
    StackedBarChart mFurherInfo;
    MaterialListView mFurterListView;

    double mBasalMetabolicRate;
    double mCalories;
    TextViewLabel mTextView;

    public FragmentTwo(){

    }

    private void fillCaloriesGraphValues(){
        Cursor mCursor=mGlobalVariables.getmDBHelper().getAllActivities();
        while(mCursor.moveToNext()){
            datas.add(mCursor.getString(mCursor.getColumnIndex(fitnessDB.Activity.DATA)));
            caloriesChenEEEList.add(mCursor.getInt(mCursor.getColumnIndex(fitnessDB.Activity.TOTAL_CALORIES)));
            caloriesFitBitList.add(mCursor.getInt(mCursor.getColumnIndex(fitnessDB.Activity.WALKING_CALORIES)));
        }

        caloriesLists.add(caloriesChenEEEList);
        caloriesLists.add(caloriesFitBitList);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragmentwo_main, container, false);
        TextView textView = (TextView) rootView.findViewById(R.id.section_label);
        textView.setText("Mio tab");

        mGlobalVariables=new SingletonGlobalVariables().getSingleton();
        mGlobalVariables.setmDBHelper(new DBHelper(getActivity()));

        /*This are for initilize the plot*/
        LineView lineView = (LineView) rootView.findViewById(R.id.Caloriesgraph);
        lineView.setDrawDotLine(false); //optional
        lineView.setShowPopup(LineView.SHOW_POPUPS_MAXMIN_ONLY); //optional


        datas = new ArrayList<String>();
        caloriesChenEEEList=new ArrayList<>();
        caloriesFitBitList=new ArrayList<>();
        caloriesLists = new ArrayList<>();
        fillCaloriesGraphValues();

        lineView.setBottomTextList(datas);
        lineView.setColorArray(new int[]{Color.parseColor("#F44336"), Color.parseColor("#9C27B0")});
        lineView.setDrawDotLine(true);
        lineView.setShowPopup(LineView.SHOW_POPUPS_All);

        lineView.setDataList(caloriesLists);

        graphDialog = new Dialog(getActivity());
        graphDialog.setTitle("Step in this Day");


        LayoutInflater factory = LayoutInflater.from(getActivity());
        final View dialogView = factory.inflate(R.layout.furtherinfo_layout,null);

        graphDialog.setContentView(dialogView);
        graphDialog.setTitle("Further Information");

        mFurherInfo= dialogView.findViewById(R.id.stackedbarchart);

        mTextView=(TextViewLabel) dialogView.findViewById(R.id.totalcalories);


        mFurterListView = (MaterialListView) dialogView.findViewById(R.id.furtherinfotab);
        mFurterListView.getAdapter().add(new Card.Builder(getContext())
                .withProvider(new CardProvider())
                .setLayout(R.layout.material_small_image_card)
                .setTitle("Info")
                .setDescription("BMR\n\n" +
                        "TID\n\n" +
                        "EEE")
                .endConfig()
                .build());


        mInfoTabView = (MaterialListView) rootView.findViewById(R.id.InfoTab);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //I wait that the service update the Global values and then i write it on the cardView
                    sleep(2000);
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mInfoTabView.getAdapter().add(new Card.Builder(getContext()).withProvider(new CardProvider())
                                    .setLayout(R.layout.material_image_with_buttons_card)
                                    .setTitle("Fitness Information")
                                    .setDescription("- Basal Metabolic Rate= "+new DecimalFormat("##.##").format(mBasalMetabolicRate=ChenEuristicEEEFormula.getBasalMetabolicRate(mGlobalVariables.getUser().Gender,mGlobalVariables.getUser().Weight,mGlobalVariables.getUser().Height,mGlobalVariables.getUser().Age))+" KCal"+
                                            "\n\n-(Stride) Step Length= "+new DecimalFormat("##.##").format((mGlobalVariables.getUser().Gender==1?(((double)mGlobalVariables.getUser().Height)*0.415):(double)mGlobalVariables.getUser().Height*0.413))+" cm"+
                                            "\n\n-BMI= "+new DecimalFormat("##.##").format(((double)mGlobalVariables.getUser().Weight)/(Math.pow(((double)mGlobalVariables.getUser().Height)/(double)100,2)))+ "Kg/m^2")
                                    .addAction(R.id.right_text_button, new TextViewAction(getContext())
                                    .setText("Further Information")
                                    .setTextResourceColor(R.color.accent_material_dark)
                                    .setListener(new OnActionClickListener() {
                                        @Override
                                        public void onActionClicked(View view, Card card) {
                                            try {
                                                Log.d("Fragment One","i want show a dialog");
                                                try {
                                                    mCalories = SensorSamplingService.getmCalories();
                                                }catch (Exception e){
                                                    Log.e("Fragment 2","Maybe the serveice is not ready");
                                                }


                                                StackedBarModel s1 = new StackedBarModel("Total Calories (KCal)");
                                                s1.addBar(new BarModel((float) ((int)mBasalMetabolicRate), 0xFF56B7F1));
                                                s1.addBar(new BarModel((int)getTID(mCalories,mBasalMetabolicRate) , 0xFFCDA67F)); //Termogenesi indotta dalla dieta
                                                s1.addBar(new BarModel((float) ((int)mCalories), 0xFF63CBB0));
                                                s1.setShowLabel(true);

                                                mTextView.setText("Total Calories= "+(int)(mBasalMetabolicRate+getTID(mCalories,mBasalMetabolicRate)+mCalories)+" KCal");

                                                mFurherInfo.clearChart();
                                                mFurherInfo.addBar(s1);
                                                mFurherInfo.startAnimation();
                                                graphDialog.show();

                                                mFurherInfo.startAnimation();

                                                }catch(Exception e){
                                                //Handle error
                                                    Log.e("FragmentOne","Dialog Error");
                                                }
                                        }
                                    }))
                                    .endConfig()
                                    .build());


                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();



        mListView = (MaterialListView) rootView.findViewById(R.id.MapTab);


        mListView.getAdapter().add(new Card.Builder(getContext())
                .withProvider(new CardProvider())
                .setLayout(R.layout.material_image_with_buttons_card)
                .setTitle("Maps Track")
                .setDescription("Determine the maps track")
                .setDrawable(R.drawable.map)
                .addAction(R.id.left_text_button, new TextViewAction(getContext())
                        .setText("MAP")
                        .setTextResourceColor(R.color.black_button)
                        .setListener(new OnActionClickListener() {
                            @Override
                            public void onActionClicked(View view, Card card) {

                            }
                        }))
                .addAction(R.id.right_text_button, new TextViewAction(getContext())
                        .setText("Chose the calories to burn")
                        .setTextResourceColor(R.color.accent_material_dark)
                        .setListener(new OnActionClickListener() {
                            @Override
                            public void onActionClicked(View view, Card card) {
                                Intent myIntent = new Intent(getContext(), MapsActivity.class);
                                myIntent.putExtra("key", value); //Optional parameters
                                getContext().startActivity(myIntent);
                            }
                        }))
                .endConfig()
                .build()
        );

        return rootView;
    }

    private double getTID(double mCalories, double mBasalMetabolicRate){
        //Get Termogenesi indotta dalla dieta
        //I assume TID 13% of total calories expanditure
        return (mCalories+mBasalMetabolicRate)*(0.13/0.87);
    }

}
