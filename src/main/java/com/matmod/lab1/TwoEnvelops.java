package com.matmod.lab1;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by igor on 13.09.17.
 */
public class TwoEnvelops extends ApplicationFrame {

    public TwoEnvelops(String title, double[] distrib, double[] res, double[] expectRes) {
        super(title);


        final XYSeries seriesDistr = new XYSeries("Xmin distrib");
        final XYSeries seriesRes = new XYSeries("E[w] - x");
        final XYSeries seriesResExp = new XYSeries("Expected: E[w] - x");
        for (int i = 0; i < distrib.length; i++)
        {
            seriesDistr.add(i, distrib[i]);
            seriesRes.add(i, res[i]);
            seriesResExp.add(i, expectRes[i]);
        }



        final XYSeriesCollection data = new XYSeriesCollection();
        //data.addSeries(seriesDistr);
        data.addSeries(seriesRes);
        data.addSeries(seriesResExp);

        final JFreeChart chart = ChartFactory.createXYLineChart(
                "XY Series Demo",
                "X",
                "Y",
                data,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        final ChartPanel chartPanel = new ChartPanel(chart);

        chartPanel.setPreferredSize(new java.awt.Dimension(1500, 700));
        setContentPane(chartPanel);

    }


    public static void main(String[] args) {

        // Заполняем распределение вероятностей Xmin

        int distribCount = 10;

        double[] distrib = new double[distribCount];

        double sum = 0;
        for (int i = 0; i < distrib.length; i++) {
            distrib[i] = function(i);
            sum += distrib[i];
        }

        // Нормализуем
        for (int i = 0; i < distrib.length; i++)
        {
            distrib[i] /= sum;
        }

        // --------------------------

        int iterCount = 1_000_000;

        // Сумма значений у оппонента
        double[] sumSecond = new double[distribCount*2];
        int[] countSecond = new int[distribCount*2];

        for(int i = 0; i < iterCount; i++)
        {
            int xMin = nextRandom(distrib);
            boolean gotMin = ThreadLocalRandom.current().nextBoolean();
            if (gotMin)
            {
                sumSecond[xMin] += xMin*2;
                countSecond[xMin]++;
            }
            else
            {
                sumSecond[xMin*2] += xMin;
                countSecond[xMin*2]++;
            }
        }


        // Значения графика
        // 1 - distrib
        // 2 - E[w] - x
        double[] res = new double[sumSecond.length];
        double[] distFull = new double[sumSecond.length];
        for (int i = 0; i < sumSecond.length; i++)
        {
            if ( i < distrib.length) distFull[i] = distrib[i];

            if (countSecond[i] > 0) {
                res[i] = sumSecond[i] / countSecond[i];
            }

            if (res[i] > 0) res[i] -= i;
        }

        // Считаем ожидаемый результат
        double[] resExpect = new double[res.length];

        for (int i = 0; i < res.length; i++)
        {
            if (i%2 != 0) {
                continue;
            }

            // Вероятность, что у нас меньше
            double pMin = distFull[i]/(distFull[i] + distFull[i/2]);
            double pMax = 1 - pMin;

            resExpect[i] = pMin*2*i + pMax*i/2 - i;
        }

        // Отображаем на графику

        final TwoEnvelops demo = new TwoEnvelops("Plot", distFull, res, resExpect);
        demo.pack();
        RefineryUtilities.centerFrameOnScreen(demo);
        demo.setVisible(true);

    }


    public static int nextRandom(double[] distrib)
    {
        double rand = ThreadLocalRandom.current().nextDouble(1);
        double sum = 0;
        for (int i = 0; i < distrib.length; i++)
        {
            sum += distrib[i];
            if (rand < sum)
            {
                return i;
            }
        }

        throw new RuntimeException("Should no happen");
    }


    public static double function(double x)
    {
        if (x == 4) return 0.5;
        else if(x == 8) return 0.5;

        return 0;
    }

}
