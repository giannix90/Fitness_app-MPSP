package com.example.gianni.mpsp;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.BounceInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.dexafree.materialList.card.Card;
import com.dexafree.materialList.card.CardProvider;
import com.dexafree.materialList.view.MaterialListView;
import com.hookedonplay.decoviewlib.DecoView;
import com.hookedonplay.decoviewlib.charts.EdgeDetail;
import com.hookedonplay.decoviewlib.charts.SeriesItem;
import com.hookedonplay.decoviewlib.charts.SeriesLabel;
import com.hookedonplay.decoviewlib.events.DecoEvent;
import com.ohoussein.playpause.PlayPauseView;
import com.robinhood.ticker.TickerUtils;
import com.robinhood.ticker.TickerView;
import com.squareup.picasso.RequestCreator;

import org.eazegraph.lib.charts.BarChart;
import org.eazegraph.lib.models.BarModel;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import im.dacer.androidcharts.BarView;
import im.dacer.androidcharts.LineView;

/**
 * Created by gianni on 07/07/17.
 */

public class FragmentOne extends Fragment implements SensorSamplingService.Result{
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */

    private int mStep;

    MaterialListView mListView;

    MaterialListView mListViewCalories;

    GlobalVariables mGlobalVariables; //This is a pointer to global variables

    BarView stepBarView;
    TickerView tickerView;
    TickerView km;
    TickerView mTickerActivity;

    ArrayList<Integer> stepNumbers;
    ArrayList<String> Stephours;

    Dialog graphDialog;

    PlayPauseView playbutton;
    DecoView arcView;
    SeriesItem seriesItem1;
    int series1Index;

    private static final String ARG_SECTION_NUMBER = "section_number";

    public FragmentOne() {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);


        //init point to Global Variables
        mGlobalVariables=new SingletonGlobalVariables().getSingleton();
        playbutton = (PlayPauseView) rootView.findViewById(R.id.play_pause_view);
        playbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent samplingIntent = new Intent(getContext(),
                        SensorSamplingService.class);
                if(!SensorSamplingService.isStarted()) {
                    getContext().startService(samplingIntent);

                }else{
                    getContext().stopService(samplingIntent);
                }
                playbutton.toggle();
            }
        });

        //This is the intent for the SensorSamplingService
        Intent samplingIntent = new Intent(getContext(),
                SensorSamplingService.class);


        if(SensorSamplingService.isStarted()) {

            getContext().stopService(samplingIntent);//I stop and restart the service
        }
        getContext().startService(samplingIntent);//I restart th service
        playbutton.toggle();

        //I have to pass the pointer to this class to link the implementation in this class of Result interface
        SensorSamplingService.setFragmentContext(this);
        mGlobalVariables.setFragmentOneContext(this);



        mListView = (MaterialListView) rootView.findViewById(R.id.material_listview);
        mListViewCalories = (MaterialListView) rootView.findViewById(R.id.calorieswalking);





        Card card = new Card.Builder(getContext())
                .withProvider(new CardProvider())
                .setLayout(R.layout.material_small_image_card)
                .setTitle("Scheda")
                .setDescription("Mia scheda")
                .setDrawable(R.drawable.running)
                .endConfig()
                .build();


        mListView.getAdapter().add(card);


        mListViewCalories.getAdapter().add(new Card.Builder(getContext())
                .withProvider(new CardProvider())
                .setLayout(R.layout.material_basic_image_buttons_card_layout)
                .setTitle("Walking Calories Counter")
                .setTitleGravity(Gravity.END)
                .setDescription("\n Calories consumed during walk: \n"+0.0)
                .setDescriptionGravity(Gravity.END)
                .setDrawable(R.drawable.runn)
                .setDrawableConfiguration(new CardProvider.OnImageConfigListener() {
                    @Override
                    public void onImageConfigure(@NonNull RequestCreator requestCreator) {
                        requestCreator.fit();
                    }
                })
                .endConfig()
                .build()
        );

        //Initialize the percentage circle

        arcView = (DecoView) rootView.findViewById(R.id.dynamicArcView);
        //arcView.disableHardwareAccelerationForDecoView();

// Create background track
        arcView.addSeries(new SeriesItem.Builder(Color.argb(255, 218, 218, 218))
                .setRange(0, 100, 100)
                .setInitialVisibility(false)
                .setLineWidth(32f)
                .build());

//Create data series track
        seriesItem1 = new SeriesItem.Builder(Color.argb(255, 64, 196, 0))
                .setRange(0, 100, 0)
                .setSeriesLabel(new SeriesLabel.Builder("Percent %.0f%%")
                        .setColorBack(Color.argb(218, 0, 0, 0))
                        .setColorText(Color.argb(255, 255, 255, 255))
                                                .build())
                .setLineWidth(32f)
                .addEdgeDetail(new EdgeDetail(EdgeDetail.EdgeType.EDGE_INNER, Color.parseColor("#22000000"), 0.4f))
                .setSeriesLabel(new SeriesLabel.Builder("Percent %.0f%%").build())
                .setInterpolator(new OvershootInterpolator())
                .setShowPointWhenEmpty(false)
                .setCapRounded(true)
                .setInset(new PointF(32f, 32f))
                .setShadowSize(15)
                .setShadowColor(Color.DKGRAY)
                .setDrawAsPoint(false)
                .setSpinClockwise(true)
                .setSpinDuration(6000)
                .setChartStyle(SeriesItem.ChartStyle.STYLE_DONUT)
                .build();

        series1Index = arcView.addSeries(seriesItem1);

        arcView.addEvent(new DecoEvent.Builder(DecoEvent.EventType.EVENT_SHOW, true)
                .setDelay(400) //4000 mSec before start the animation
                .setDuration(2000)
                .build());

        arcView.addEvent(new DecoEvent.Builder(85).setIndex(series1Index).setDelay(500).build());
        /*To change percentage
        *
            arcView.addEvent(new DecoEvent.Builder(85).setIndex(series1Index).setDelay(4000).build());
        * */


        arcView.configureAngles(360, 0);


                /*For Step counter graph dialog*/
        graphDialog = new Dialog(getActivity());
        graphDialog.setTitle("Step in this Day");


        LayoutInflater factory = LayoutInflater.from(getActivity());
        View dialogView = factory.inflate(R.layout.step_graph,null);

        graphDialog.setContentView(dialogView);
        graphDialog.setTitle("Step in this day" );

        stepBarView=(BarView) dialogView.findViewById(R.id.Stepgraph);
        Stephours= new ArrayList<>();
        stepNumbers=new ArrayList<>();


        tickerView = rootView.findViewById(R.id.tickerView);
        tickerView.setCharacterList(TickerUtils.getDefaultNumberList());
        tickerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    Log.d("Fragment One","i want show a dialog");
                    graphDialog.show();
                }catch(Exception e){
                    //Handle error
                    Log.e("FragmentOne","Dialog Error");
                }

            }
        });
        km = rootView.findViewById(R.id.km);
        km.setCharacterList(TickerUtils.getDefaultNumberList());


        mTickerActivity=rootView.findViewById(R.id.ticketActivity);
        mTickerActivity.setCharacterList(TickerUtils.getDefaultNumberList());


        return rootView;
    }
    @Override
    public void onDestroyView(){
        mGlobalVariables.setFragmentOneContext(null);
        super.onDestroyView();
    }

    @Override
    public void onNewCalories(final double Calories) {
        //Log.d("Fragment 1","Calorie: "+Calories);
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {

                mListView.getAdapter().clearAll();

                mListView.getAdapter().add(new Card.Builder(getContext())
                        .withProvider(new CardProvider())
                        .setLayout(R.layout.material_small_image_card)
                        .setTitle("Chen Euristic EEE Formula")
                        .setDescription("Calories consumed: "+new DecimalFormat("##.##").format(Calories)+" KCal")
                        .endConfig()
                        .build()
                );


            }
        });

    }

    @Override
    public void onStepDetected(final AtomicInteger stepNumber) {

        //Log.d("During Walking","Calorie: "+stepNumber);
        mStep=stepNumber.get();

        try{
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    arcView.addEvent(new DecoEvent.Builder((int)((double)mStep*((double)100/(double)6000))).setIndex(series1Index).setDelay(500).build());
                    tickerView.setText(""+stepNumber);

                }
            });
        }catch (java.lang.NullPointerException e){
            //Do Nothing
        }

    }
    @Override
    public void onNewFItBitCalories(final double Calories){

        //Log.d("During Walking","Calorie: "+Calories);

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {

                mListViewCalories.getAdapter().clearAll();
                mListViewCalories.getAdapter().add(new Card.Builder(getContext())
                                .withProvider(new CardProvider())
                                .setLayout(R.layout.material_basic_image_buttons_card_layout)
                                .setTitle("FitBit Calories Burned Estimation")
                                .setTitleGravity(Gravity.END)
                                .setDescription("\n Calories consumed: \n"+new DecimalFormat("##.##").format(Calories)+" KCal")
                                .setDescriptionGravity(Gravity.END)
                                .setDrawable(R.drawable.runn)
                                .setDrawableConfiguration(new CardProvider.OnImageConfigListener() {
                                    @Override
                                    public void onImageConfigure(@NonNull RequestCreator requestCreator) {
                                        requestCreator.fit();
                                    }
                                })
                                .endConfig()
                                .build()
                );


            }
        });
    }
    @Override
    public void onNewKmWalked(final double Km){
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String s=new DecimalFormat("##.##").format(Km);
            km.setText(""+s+" Km");
            }
        });
    }

    @Override
    public void onNewStepsGroupOfTheDay(int[] mSteps){

        //Clear the structures
        stepNumbers.clear();
        Stephours.clear();

        for(int i=0;i<mSteps.length;i++){
            stepNumbers.add(mSteps[i]);
            Stephours.add(i+" ");
        }

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                stepBarView.setBottomTextList(Stephours);
                stepBarView.setDataList(stepNumbers,4000);
            }
        });


    }
    @Override
    public void onNewActivity(final String mActivity){
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String currentActivity;
                if(mActivity.compareTo("sedentary")==0)
                    currentActivity="Sedentary";
                else if(mActivity.compareTo("poorlyactive")==0)
                    currentActivity="Not Very Active";
                else if(mActivity.compareTo("active")==0)
                    currentActivity="Active";
                else if(mActivity.compareTo("veryactive")==0)
                    currentActivity="Highly Active";
                else
                    currentActivity=mActivity;

                mTickerActivity.setText("Current level of Activity: "+currentActivity);
            }
        });

    }

}
