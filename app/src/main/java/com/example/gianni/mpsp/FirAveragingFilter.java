package com.example.gianni.mpsp;

import android.util.Log;

/**
 * Created by gianni on 08/07/17.
 */

public class FirAveragingFilter {
    /**
     * this is the recoursive eq:
     *
     *       y(k)=y(k-1)+(1/N)*(u(k)-u(k-N))
     *
     * **/
    private int N; //This is the size of the moving window, determine the cut frequency 0.442947/(sqrt((n^2)-1))*fs  where fs is the sampling frequency
    private double[] u; //window of moving filter
    private double PreviousOutput; //y(k-1)
    private double[] y;
    private int nSample; //It is a counter of sample

    public FirAveragingFilter(int N){
        this.N=N;
        this.u=new double[N];
        this.y=new double[N];
        this.nSample=0;
    }

    private void _shift(){
        for(int i=0;i<N-1;i++){
            //shift left
            u[i]=u[i+1];
            y[i]=y[i+1];
        }
    }

    public double filter(double u){
        //u is the input
        //Moving Averaging Filter

        if(nSample==0){
            //this is the 1° sample and i cannot apply the filter
            this.u[0]=u;
            //PreviousOutput=u;
            y[0]=u;

            nSample++;
            return  0;
        }

        else if(nSample<N){
            //PreviousOutput=PreviousOutput+((double)(1/N))*(u-this.u[0]);
            y[nSample]=u;
            this.u[nSample]=u;
            nSample++;
            return 0;
        }

        _shift();
        this.u[N-1]=u;
        y[N-1]=((double)1/(double)N)*(this.u[N-1]+this.u[N-2]+this.u[N-3]+this.u[N-4]);
        nSample++;
        return  y[N-1];


    }
}