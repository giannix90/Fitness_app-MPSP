package com.example.gianni.mpsp;

/**
 * Created by gianni on 08/07/17.
 */

public class ChenEuristicEEEFormula {
    /*
    * This euristic formula give a basic equation to find the calories expenditure in Kj/minutes
    * */

    private int Gen; //Gender of the person (1 for male, 2 for female)
    private int Age; //Age of the person in years
    private int Hei; //Height of the person in cm
    private int Wei; //Weigth of the person in Kg
    private double BMI; //Body to Mass Index calculated as kg/m^2

    //Parameter for the Equation
    private static double a;
    private static double b;
    private static double k;
    private static double m;

    public ChenEuristicEEEFormula(int Gen,int Wei,int Age,int Hei){
        this.Gen=Gen;
        this.Wei=Wei;
        this.a=0.01281*((double)Wei)+0.84322;
        this.b=0.0389*((double)Wei)-0.68244*((double)Gen)+0.69250;
        this.k=0.0266*((double)Wei)+0.14672;
        this.m=(-0.00285)*((double)Wei)+0.96828;
    }

    private double _KjtoKcal(double Kj){
        return Kj*0.23900573614;
    }

    public double getEEE(double Ax,double Ay,double Az){

        double Ah=Math.sqrt((Ax*Ax)+(Ay*Ay));

        return _KjtoKcal(a*Math.pow(Ah,k)+b*Math.pow(Az,m));
    }

    public static double getBasalMetabolicRate(int Gen,int Wei, int Hei, int Age){

        //Mifflin-St. Jeor formula
        //This is the basic Kcal expandiure to mantain basic vital function
        if(Gen==1)
            return 5 + (9.99 * (double) Wei) + (6.25* (double)Hei) - (4.92 * (double) Age);
        else
            return -161 + (9.99 * (double) Wei) + (6.25* (double)Hei) - (4.92 * (double) Age);

    }

}
