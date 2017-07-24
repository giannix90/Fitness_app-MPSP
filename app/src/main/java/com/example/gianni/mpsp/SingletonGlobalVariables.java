package com.example.gianni.mpsp;

import android.util.Log;

/**
 * Created by gianni on 09/07/17.
 */

public class SingletonGlobalVariables {

    private static volatile GlobalVariables l;

    public GlobalVariables getSingleton(){

        GlobalVariables tmp=l;

        if(tmp==null){

            synchronized(this){ //Synchronized on SingletonListOfPeer.class

                if(tmp==null){

                    l=tmp=new GlobalVariables(); //Lazy initialization
                    Log.e("Singleton","New instance");
                }
            }

        }
        return tmp;
    }

}
