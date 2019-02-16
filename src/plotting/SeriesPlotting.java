package plotting;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class SeriesPlotting extends JFrame {

    public SeriesPlotting(String title , double[] plotDataX , double[] plotDataY , String xAxisLabel , String yAxisLabel , double threshold) {
        super(title);

        // Create dataset
        XYDataset dataset = createDataset(title, plotDataX , plotDataY, threshold);

        // Create chart
        JFreeChart chart = ChartFactory.createXYLineChart(
                title,
                xAxisLabel,
                yAxisLabel,
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false);

        // Create Panel
        ChartPanel panel = new ChartPanel(chart);
        setContentPane(panel);
    }

    private XYDataset createDataset(String title, double[] plotDataX , double[] plotDataY , double threshold) {
        XYSeriesCollection dataset = new XYSeriesCollection();

        XYSeries series1 = new XYSeries(title);
        for (int i = 0 ; i < plotDataX.length ; i++){
            series1.add(plotDataX[i],plotDataY[i]);
        }

        XYSeries series2 = new XYSeries("threshold");
        for (int i = 0 ; i < plotDataX.length ; i++){
            series2.add(plotDataX[i],threshold);
        }
        //Add series to dataset
        dataset.addSeries(series1);
        dataset.addSeries(series2);

        return dataset;
    }

    public static void plotData(String title , double[] plotDataX , double[] plotDataY , String xAxisLabel , String yAxisLabel , double threshold) {
        SeriesPlotting example = new SeriesPlotting(title , plotDataX , plotDataY, xAxisLabel, yAxisLabel , threshold);
        example.setSize(800, 400);
        example.setLocationRelativeTo(null);
        example.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        example.setVisible(true);
    }
}