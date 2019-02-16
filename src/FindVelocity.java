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

    private static int[] findMinimumFrame(int totalWindows,double[] meanArray, double threshold, int numFramesNeeded, int maxLags, double[] signalArray ){

        int count = 0;
        int frames = 0;
        double[] autoCorrelationArray;
        int[] peakArray = new int[totalWindows];

        while(count < totalWindows){
            if(meanArray[count] >= threshold){
                double[] tempArray = new double[numFramesNeeded];

                System.arraycopy(signalArray,frames,tempArray,0,numFramesNeeded);

                autoCorrelationArray = autoCorrelation(tempArray,maxLags,meanArray[count]);

                int minIndex = 1;

                for(int i = 1 ; i<autoCorrelationArray.length ; i++){
                    if(autoCorrelationArray[i] < autoCorrelationArray[minIndex]){
                        minIndex = i;
                    }
                }

                int peakIndex = minIndex;

                for(int i = minIndex; i<autoCorrelationArray.length; i++){
                    if(autoCorrelationArray[i] > autoCorrelationArray[peakIndex]){
                        peakIndex = i;
                    }
                }

                peakArray[count] = peakIndex;
            }
            else{
                peakArray[count] = 500;
            }

            count = count+1;
            frames = frames + numFramesNeeded;
        }
        return peakArray;
    }

    //main method
    public static void main(String[] args)
    {
        try
        {
            // Open the wav file
            WavFile wavFile = WavFile.openWavFile(new File("D:\\IdeaProjects\\Major Project\\src\\data_921.wav"));

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
            int totalWindows = 400;
            int numFramesNeeded = (int) Math.ceil(time * sampleRate);
            int shiftValue = 0;

            //Get time array and signal array
            double[] timeArray = new double[(totalWindows+2)*numFramesNeeded];
            double[] signalArray = new double[(totalWindows+2)*numFramesNeeded];

            for (int s=0 ; s<timeArray.length ; s++)
            {
                timeArray[s] = (double) s/sampleRate;
                signalArray[s] = bufferArray[shiftValue+s];
            }

            //Find the Mean Array and threshold
            double[] meanArray = findSignalMeanArray(signalArray ,numFramesNeeded ,totalWindows);
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
            double velocity = (frequency * 1540.0) / (4.0*10000);
            System.out.println(velocity);

            double[] newArray = new double[totalWindows];
            double[] newPeakArray = new double[totalWindows];

            for (int s=0 ; s<totalWindows ; s++)
            {
                newArray[s] = (double) s;
                newPeakArray[s] = (double) peakArray[s];
            }

            System.out.println(threshold);
            // Plot the signal
            String plotTitle = "Signal";
            String xAxis = "time";
            String yAxis = "Amplitude";
            SeriesPlotting.plotData(plotTitle,timeArray,signalArray,xAxis,yAxis,threshold);
            SeriesPlotting.plotData(plotTitle,newArray,newPeakArray,xAxis,yAxis,threshold);
        }
        catch (Exception e)
        {
            System.err.println(e);
        }
    }
}