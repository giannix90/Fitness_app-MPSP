package com.example.gianni.mpsp;

import android.provider.BaseColumns;

/**
 * Created by gianni on 13/07/17.
 */

public class fitnessDB {
    public static abstract class User implements BaseColumns {
        public static final String TABLE= "User";
        public static final String ID="id";
        public static final String NAME="Name";
        public static final String GENDER="Gender";
        public static final String AGE="Age";
        public static final String WEIGHT="Weight";
        public static final String HEIGHT="Height";
    }

    public static abstract class Activity implements BaseColumns {
        public static final String TABLE= "Activity";
        public static final String ID="id";
        public static final String WALKING_DETECT="Walking_detect";
        public static final String RUNNING_DETECT="Running_detect";
        public static final String BIKING_DETECT="Biking_detect";
        public static final String STEP="Step";
        public static final String TOTAL_CALORIES="Total_calories";
        public static final String WALKING_CALORIES="Walking_calories";
        public static final String DATA="Data";
    }

    public static abstract class Steps implements BaseColumns{
        public static final String TABLE="Steps";
        public static final String ID="id";
        public static final String H0="H0";
        public static final String H1="H1";
        public static final String H2="H2";
        public static final String H3="H3";
        public static final String H4="H4";
        public static final String H5="H5";
        public static final String H6="H6";
        public static final String H7="H7";
        public static final String H8="H8";
        public static final String H9="H9";
        public static final String H10="H10";
        public static final String H11="H11";
        public static final String H12="H12";
        public static final String H13="H13";
        public static final String H14="H14";
        public static final String H15="H15";
        public static final String H16="H16";
        public static final String H17="H17";
        public static final String H18="H18";
        public static final String H19="H19";
        public static final String H20="H20";
        public static final String H21="H21";
        public static final String H22="H22";
        public static final String H23="H23";
        public static final String H24="H24";

    }
}
