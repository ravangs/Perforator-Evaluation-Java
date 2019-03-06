import java.io.*;
import java.lang.Math;
import java.util.Arrays;

import wavfile.*;
import plotting.*;

public class FindVelocity
{

    //Find the mean array of the signal for threshold
    private static double[] findSignalMeanArray(double[] signalArray,int numFramesNeeded, int totalWindows){
        int frames = 0;
        double[] meanArray = new double[totalWindows];

        for(int count = 0 ; count < totalWindows ; count++){
            double[] absoluteArray = new double[numFramesNeeded];

            for(int i = 0 ; i < numFramesNeeded ; i++) {
                absoluteArray[i] = Math.abs(signalArray[frames + i]);
            }

            meanArray[count] = (Arrays.stream(absoluteArray).sum())/numFramesNeeded;

            frames = frames + numFramesNeeded;
        }
        return meanArray;
    }

    //Find autoCorrelation of an array with maximum_delay = maxLags
    private static double[] autoCorrelation(double[] tempArray, int maxLags, double mean){
        double[] autoCorrelationArray = new double[maxLags];

        for (int t=0 ; t<autoCorrelationArray.length ; t++){
            double numerator = 0;
            double denominator = 0;

            for(int i=0 ; i<tempArray.length ; i++){
                double xit = tempArray[i] - mean;
                numerator = numerator + (xit * (tempArray[(i + t) % tempArray.length] - mean));
                denominator = denominator + (xit * xit);
            }

            autoCorrelationArray[t] = numerator/denominator;
        }

        return autoCorrelationArray;
    }

    private static int[] findMinimumFrame(int totalWindows,double[] meanArray, double threshold, int numFramesNeeded, int maxLags, double[] wienerOutput ){

        int count = 0;
        int frames = 0;
        double[] autoCorrelationArray;
        int[] peakArray = new int[totalWindows];

        while(count < totalWindows){
            if(meanArray[count] >= threshold){
                double[] tempArray = new double[numFramesNeeded];

                System.arraycopy(wienerOutput,frames,tempArray,0,numFramesNeeded);

                autoCorrelationArray = autoCorrelation(tempArray,maxLags,meanArray[count]);


                int peakWidth = 30;
                int peakIndex = peakWidth;
                int verifyCount = 2*peakWidth + 1;
                for(int i = peakWidth;i<autoCorrelationArray.length-peakWidth;i++){
                    int countEqual = 0;
                    for(int j = -peakWidth;j<=peakWidth;j++){
                        if(autoCorrelationArray[i]>=autoCorrelationArray[i+j]){
                            countEqual++;
                        }
                    }
                    if(verifyCount==countEqual){
                        peakIndex = i;
                        break;
                    }
                }


//                int minIndex = 0;
//
//                for(int i = 1 ; i<autoCorrelationArray.length ; i++){
//                    if(autoCorrelationArray[i] < autoCorrelationArray[minIndex]){
//                        minIndex = i;
//                    }
//                }
//
//                int peakIndex = minIndex;
//
//                for(int i = minIndex; i<autoCorrelationArray.length; i++){
//                    if(autoCorrelationArray[i] > autoCorrelationArray[peakIndex]){
//                        peakIndex = i;
//                    }
//                }

                peakArray[count] = peakIndex;
                if(peakIndex == peakWidth){
                    peakArray[count] = 500;
                }
            }
            else{
                peakArray[count] = 500;
            }

            count = count+1;
            frames = frames + numFramesNeeded;
        }
        return peakArray;
    }



    private static double[] wienerFilter(double[] signalArray,int numFramesNeeded){

        double[] localMean = new double[signalArray.length];
        double[] localVariance = new double[signalArray.length];
        double meanVariance = 0.0;
        for(int i=0; i<signalArray.length; i++){
            localMean[i] = 0;
            if(i<=(numFramesNeeded/2)){
                for(int j = 0;j <= i+(numFramesNeeded/2);j++){
                    localMean[i] = localMean[i] + signalArray[j];
                    localVariance[i] = localVariance[i] + (signalArray[j]*signalArray[j]);
                }
            }
            else if(i >=(signalArray.length-(numFramesNeeded/2))){
                for(int j = i-(numFramesNeeded/2);j < signalArray.length;j++){
                    localMean[i] = localMean[i] + signalArray[j];
                    localVariance[i] = localVariance[i] + (signalArray[j]*signalArray[j]);
                }
            }
            else{
                for(int j = i-(numFramesNeeded/2);j <= i+(numFramesNeeded/2);j++){
                    localMean[i] = localMean[i] + signalArray[j];
                    localVariance[i] = localVariance[i] + (signalArray[j]*signalArray[j]);
                }
            }
            localMean[i] = localMean[i]/numFramesNeeded;
            localVariance[i] = (localVariance[i]/numFramesNeeded) - (localMean[i]*localMean[i]);
            meanVariance = meanVariance + localVariance[i];
        }
        meanVariance = meanVariance/localVariance.length;

        double[] filteredOutput = new double[signalArray.length];
        for(int i = 0; i<signalArray.length;i++){
            if(localVariance[i] < meanVariance){
                filteredOutput[i] = localMean[i];
            }
            else{
                filteredOutput[i] = signalArray[i] - localMean[i];
                filteredOutput[i] = filteredOutput[i] * (1 - (meanVariance/localVariance[i]));
                filteredOutput[i] = filteredOutput[i] + localMean[i];
            }
        }

        return filteredOutput;
    }

    private static double findVelocity(double[] signalArray,int numFramesNeeded,int totalWindows,double sampleRate){
        double[] meanArray = findSignalMeanArray(signalArray,numFramesNeeded ,totalWindows);
        double threshold = (Arrays.stream(meanArray).sum())/totalWindows;

        //Find velocity

        int maxLags = 250;
        int[] peakArray;
        int minFrame = 500;

        peakArray = findMinimumFrame(totalWindows,meanArray,threshold,numFramesNeeded,maxLags,signalArray);

        for(int i : peakArray){
            if(i<minFrame){
                minFrame = i;
            }
        }

        double frequency = sampleRate/minFrame;

        return (frequency * 1540.0) / (4.0*10000);
    }

    private static double findMedian(double[] a, int lengthOfArray)
    {
        // First we sort the array
        Arrays.sort(a);

        // check for even case
        if (lengthOfArray % 2 != 0)
            return a[lengthOfArray / 2];

        return (a[(lengthOfArray - 1) / 2] + a[lengthOfArray / 2]) / 2.0;
    }
    //main method
    public static void main(String[] args)
    {
        try
        {
            long startTime = System.nanoTime();// Open the wav file
            WavFile wavFile = WavFile.openWavFile(new File("D:\\IdeaProjects\\Major Project\\src\\recorded_audio.wav"));

            // Display information about the wav file
            wavFile.display();

            //Get the number of frames in the wav file
            int numFrames = (int) wavFile.getNumFrames();

            // Create a buffer of num frames
            double[] bufferArray = new double[numFrames];

            //Get sampling rate
            double sampleRate = wavFile.getSampleRate();

            // Read frames into bufferArray
            wavFile.readFrames(bufferArray, numFrames);

            // Close the wavFile
            wavFile.close();

            //Signal Processing pre-requirements declaration
            double time = 0.05;
            double interval = 2.5;
            int numWindowsNeeded = (int) (interval/time) ;
            int numFramesNeeded = (int) Math.ceil(time * sampleRate);
            int totalWindows = (numFrames/numFramesNeeded);
            int numVelocitySamples = (totalWindows/numWindowsNeeded)-1;
            double[] velocityArray = new double[numVelocitySamples];

            for(int i = 0;i<velocityArray.length;i++){
                double[] signalArray = new double[(numWindowsNeeded+2)*numFramesNeeded];

                System.arraycopy(bufferArray,i*((numWindowsNeeded+2)*numFramesNeeded),signalArray,0,
                        (numWindowsNeeded+2)*numFramesNeeded);
                velocityArray[i] = findVelocity(signalArray,numFramesNeeded,numWindowsNeeded,sampleRate);
            }

//            //Get time array and signal array
//            double[] timeArray = new double[(totalWindows+2)*numFramesNeeded];
//            double[] signalArray = new double[(totalWindows+2)*numFramesNeeded];

//            for (int s=0 ; s<timeArray.length ; s++)
//            {
//                timeArray[s] = (double) s/sampleRate;
//                signalArray[s] = bufferArray[shiftValue+s];
//            }

            //double[] wienerOutput = wienerFilter(signalArray,numFramesNeeded);
            //Find the Mean Array and threshold
            double velocityMedian = findMedian(velocityArray,velocityArray.length);

            System.out.println(velocityMedian);
            long endTime = System.nanoTime();
            long totalTime = endTime-startTime;
            System.out.println(totalTime);

            double[] newArray = new double[velocityArray.length];
            double[] newPeakArray = new double[velocityArray.length];
//
            for (int s=0 ; s<velocityArray.length ; s++)
            {
                newArray[s] = (double) s;
                newPeakArray[s] = velocityArray[s];
            }


            // Plot the signal
            String plotTitle = "Signal";
            String xAxis = "time";
            String yAxis = "Amplitude";
            //SeriesPlotting.plotData(plotTitle,timeArray,signalArray,xAxis,yAxis,threshold);
            //SeriesPlotting.plotData(plotTitle,timeArray,wienerOutput,xAxis,yAxis,threshold);
            SeriesPlotting.plotData(plotTitle,newArray,newPeakArray,xAxis,yAxis,(Arrays.stream(velocityArray).sum())/velocityArray.length);

        }
        catch (Exception e)
        {
            System.err.println(e);
        }
    }
}