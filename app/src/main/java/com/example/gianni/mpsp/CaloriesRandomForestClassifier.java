package com.example.gianni.mpsp;

import android.os.Environment;
import android.util.Log;

import java.io.File;

import weka.classifiers.Classifier;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;

/**
 * Created by gianni on 11/07/17.
 */

public class CaloriesRandomForestClassifier {

    public CaloriesRandomForestClassifier(){

    }

    public double classify(){
        //load model
        String rootPath= Environment.getExternalStorageDirectory() + File.separator + "RandomForestClassifier" + File.separator;
        Classifier cls = null;
        try {
            //I load the random forest classifier previously created through Weka
            cls = (Classifier) weka.core.SerializationHelper.read(rootPath+"CaloriesRandomForest.model");
        } catch (Exception e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        ConverterUtils.DataSource source1 = null;
        try {
            source1 = new ConverterUtils.DataSource(rootPath+"OneYearFitBitData2.arff");
        } catch (Exception e2) {
            // TODO Auto-generated catch block
            e2.printStackTrace();
        }
        Instances train = null;
        try {
            train = source1.getDataSet();
        } catch (Exception e2) {
            // TODO Auto-generated catch block
            e2.printStackTrace();
        }
        // setting class attribute if the data format does not provide this information
        // For example, the XRFF format saves the class attribute information as well
        if (train.classIndex() == -1)
            train.setClassIndex(train.numAttributes() - 1);


        Instances test = null;
        ConverterUtils.DataSource source2 = null;
        try {
            source2 = new ConverterUtils.DataSource(rootPath+"calorieInstancy.arff");
        } catch (Exception e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        try {
            test = source2.getDataSet();
        } catch (Exception e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        if (test.classIndex() == -1)
            test.setClassIndex(train.numAttributes() - 1);


        // this does the trick
        double label = 0;
        try {
            label = cls.classifyInstance(test.instance(0));
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        Log.e("Classifier: ",label+"");
        return label;
    }
}
