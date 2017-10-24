package com.matmod.lab2;

import java.util.Arrays;

/**
 * Created by igor on 21.10.17.
 */
public class NashEquilibrium {

    public static int N;

    // Содержит количесто стратегий каждого игрока
    public static int[] sNum;

    // N * S0 * S1 * ... * SN
    public static double[][] W;


    public static int countIndex(int[] i)
    {
        int index = 0;
        for (int k = 0; k < i.length; k++)
        {
            index = (index*sNum[k] + i[k]);
        }
        return index;
    }


    public static void setW(int n, int[] i, double val)
    {
        W[n][countIndex(i)] = val;
    }


    public static double getW(int n, int[] i)
    {
        return W[n][countIndex(i)];
    }

    // Заполняем N, W и sNum
    static {
        // --------- ИЗМЕНЕНИЯ ПАРАМЕТРОВ ПРОИЗВОДИТЬ ЗДЕСЬ -------------

        N = 2;
        sNum = new int[]{3,3};
        W = new double[N][];

        int WLen = 1;
        for (int n = 0; n < sNum.length; n++)
        {
            WLen *= sNum[n];
        }

        for (int n = 0; n < W.length; n++)
        {
            W[n] = new double[WLen];
        }

        double[][][] wMap = new double[][][]{
                // Для игрока 0
                new double[][]{
                        // Стратегии игрока 0
                        new double[]{
                                // Стратегии игрока 1
                                0, 10, -10
                        },
                        new double[]{
                                // Стратегии игрока 1
                                -10, 10, 10
                        },
                        new double[]{
                                // Стратегии игрока 1
                                10, -10, -11
                        }
                },
                // Для игрока 1
                new double[][]{
                        // Стратегии игрока 0
                        new double[]{
                                // Стратегии игрока 1
                                0, -10, 10
                        },
                        new double[]{
                                // Стратегии игрока 2
                                10, 10, -10
                        },
                        new double[]{
                                // Стратегии игрока 2
                                -10, 10, -11
                        }
                }
        };

        for (int n = 0; n < wMap.length; n++)
        {
            for (int s0 = 0; s0 < wMap[n].length; s0++)
            {
                for (int s1 = 0; s1 < wMap[n][s0].length; s1++)
                {
                    setW(n, new int[]{s0, s1}, wMap[n][s0][s1]);
                }
            }
        }

        // --------------------------
/*
        N = 2;
        sNum = new int[]{2, 2};
        W = new double[N][];

        int WLen = 1;
        for (int n = 0; n < sNum.length; n++)
        {
            WLen *= sNum[n];
        }

        for (int n = 0; n < W.length; n++)
        {
            W[n] = new double[WLen];
        }

        // N x S0 x ... x SN
        double[][][] wMap = new double[][][]{
                // Для игрока 0
                new double[][]{
                        // Стратегии игрока 0
                        new double[]{
                                // Стратегии игрока 1
                                10, -10
                        },
                        new double[]{
                                // Стратегии игрока 1
                                -10, 10
                        }
                },
                // Для игрока 1
                new double[][]{
                        // Стратегии игрока 0
                        new double[]{
                                // Стратегии игрока 1
                                -10, 10
                        },
                        new double[]{
                                // Стратегии игрока 2
                                10, -10
                        }
                }
        };


        for (int n = 0; n < wMap.length; n++)
        {
            for (int s0 = 0; s0 < wMap[n].length; s0++)
            {
                for (int s1 = 0; s1 < wMap[n][s0].length; s1++)
                {
                    setW(n, new int[]{s0, s1}, wMap[n][s0][s1]);
                }
            }
        }*/
    }


    // Выходные данные
    public static double[][] P;
    // Используется для хранения измененных вероятностей
    public static double[][] PLast;
    // Используется для хранения градиента N * (Sn-1)
    public static double[][] grad;
    // Коэффициенты плоскостей
    public static CoefAndIndex[][] coefAndIndices;
    // Инициализируем P, PLast и grad
    static {
        P = new double[N][];
        PLast = new double[N][];
        grad = new double[N][];
        coefAndIndices = new CoefAndIndex[N][];
        for (int n = 0; n < N; n++)
        {
            P[n] = new double[sNum[n]];
            PLast[n] = new double[sNum[n]];

            P[n][1] = 0.99;
            P[n][0] = 0.01;
            /*for (int k = 0; k < P[n].length; k++)
            {
                // Первоначальное заполнение - равновероятно
                P[n][k] = 1.0 / P[n].length;
            }*/

            grad[n] = new double[sNum[n]-1];
            coefAndIndices[n] = new CoefAndIndex[sNum[n]];

            for (int t = 0; t < coefAndIndices[n].length; t++)
            {
                coefAndIndices[n][t] = new CoefAndIndex();
            }
        }
    }

    // Содержит индексы при итерации по стратегиям игроков
    public static int[] i = new int[N];

    // Хранит мат ожидания выигрыша каждого игрока
    public static double[] EW = new double[N];
    static {
        Arrays.fill(EW, Double.NEGATIVE_INFINITY);
    }

    // Используется для хранения мат ожидания выигрыша игроков при выполнении изменения
    public static double[] EWLast = new double[N];


    public static void main(String[] args) throws Exception {

        int iNum = 0;
        boolean continueRun = true;

        // TODO - проблема в том, что мы можем иметь большой отрицательный коэффициент и отнять определенное значение, но это отнятое значение
        // пойдет не равномерно в соответствии с наибольшими знаениями, а уйдет в конец
        while (continueRun)
        {
            continueRun = false;
            for (int n = 0; n < N; n++) {
                final int fn = n;

                if (++iNum % 5000 == 0) Thread.sleep((3_000));
                //Thread.sleep(10);


                // Считаем градиент

                Arrays.fill(grad[n], 0);

                performIter(() -> {
                    // Последнюю итерацию не учитываем - она уже учтена в других итерациях
                    if (i[fn] >= grad[fn].length) return;

                    // p = P[0][S0]*...*P[n][Sn]
                    double p = 1;
                    System.out.println("i: "+Arrays.toString(i));
                    for (int k = 0; k < N; k++)
                    {
                        if (k != fn) p *= P[k][i[k]];
                    }
System.out.println("P: "+p);
                    // (W(n,S0,...,Sn) - W(n,S0,...,ScurLast,...,Sn))*P[0][S0]*...*P[n][Sn]
                    int curI = i[fn];
                    grad[fn][curI] += getW(fn, i)*p;
                    System.out.println("G1: "+grad[fn][curI]);
                    i[fn] = sNum[fn]-1; // устанавливаем последней стратегии
                    // TODO - возможно, проблемы с вероятностью...
                    grad[fn][curI] -= getW(fn, i)*p;
                    System.out.println("G2: "+grad[fn][curI]);
                    i[fn] = curI;
                });

                System.out.println("Grad: "+Arrays.toString(grad[n]));

                // Считаем изменение коэффициентов

                // double lambda = 0.001;
                double lambda = getLambda(grad[n], P[n]);
                System.out.println("Lambda: "+lambda);

                double pSum = 0;
                for (int k = 0; k < P[n].length-1; k++)
                {
                    PLast[n][k] = P[n][k] + lambda*grad[n][k];
                    /*if (PLast[n][k] < 0){
                        PLast[n][k] = 0;
                        System.out.println("Correction! 0");
                    }
                    if (PLast[n][k] > 1)
                    {
                        PLast[n][k] = 1;
                        System.out.println("Correction! 1");
                    }*/

                    pSum += PLast[n][k];
                }
                PLast[n][PLast[n].length-1] = 1-pSum;

                // Корректируем вероятности
                correctP(coefAndIndices[n], grad[n], PLast[n]);

                //if (PLast[n][PLast[n].length-1] < 0 || PLast[n][PLast[n].length-1] > 1) throw new RuntimeException("Wrong value: "+PLast[n][PLast[n].length-1]);

                // Считаем изменение мат ожидания выигрыша
                Arrays.fill(EWLast, 0);

                performIter(() -> {
                    double p = 1;
                    for (int k = 0; k < N; k++)
                    {
                        p *= (k == fn ? PLast[k][i[k]] : P[k][i[k]]);
                    }

                    for (int k = 0; k < N; k++) {
                        EWLast[k] += getW(k, i) * p;
                    }
                });


                System.out.println("EChange for "+n+" "+EW[n]+" "+EWLast[n]);
                System.out.println("PLast: "+Arrays.toString(PLast[n]));

                // Проверяем условие приема изменений и остановки
                if (EWLast[n] - EW[n] >= 0.00000001)
                {
                    continueRun = true;

                    for (int k = 0; k < N; k++)
                    {
                        EW[k] = EWLast[k];
                    }

                    // Принимаем изменения коэффициентов
                    for (int k = 0; k < P[n].length; k++)
                    {
                        P[n][k] = PLast[n][k];
                    }
                }

                System.out.println("--------------------");
            }
        }
    }


    // Lambda = 0.01*(1-maxP)/speed
    public static double getLambda(double[] grad, double[] p)
    {
        double maxV = grad[0];
        int maxI = 0;

        for (int k = 1; k < grad.length; k++)
        {
            if (maxV < grad[k])
            {
                maxI = k;
                maxV = grad[k];
            }
        }

        double maxP = p[maxI];
        if (maxV < 0)
        {
            maxP = p[p.length-1];
        }

        // TODO - стоит добавить скоровть, основанную на градиенте (сумме градиента).
        return 0.0001*(1-maxP);
    }


    /**
     * Если какое-либо из значений не удовлетворяет огранибению >= 0, <= 1 - корректируем оптимальным образом -
     * перебрасываем лишние значения в вероятность с наибольшим коэффициентом уравнения плоскости. Или отнимаем
     * из наименьшего.
     *
     * @param grad
     * @param p
     */
    public static void correctP(CoefAndIndex[] coefAndIndices, double[] grad, double[] p)
    {
        /**
         * 1) Считаем суммарный избыток/недостаток
         * 2) Сортируем по зрачению grad вероятности
         * 3) Распределяем избыток/недостаток оптимальным образом
         */
        double del = 0;
        for (int k = 0; k < p.length; k++)
        {
            if (p[k] < 0)
            {
                del += p[k];
                p[k] = 0;
            }
            else if(p[k] > 1)
            {
                del += p[k]-1;
                p[k] = 1;
            }
        }

        if (del == 0) return;

        // Распределяем избыток

        for (int k = 0; k < grad.length; k++)
        {
            coefAndIndices[k].coef = grad[k];
            coefAndIndices[k].index = k;
        }
        coefAndIndices[grad.length].coef = 0;
        coefAndIndices[grad.length].index = grad.length;

        int c = del < 0 ? 1 : -1;

        Arrays.sort(coefAndIndices, (c1,c2) -> c1.coef < c2.coef ? -1*c : c1.coef == c2.coef ? 0 : 1*c);

        double pSum = 0;
        for (int k = 0 ; k < coefAndIndices.length; k++)
        {
            p[coefAndIndices[k].index] += del;
            del = 0;

            if (p[coefAndIndices[k].index] < 0)
            {
                del = p[coefAndIndices[k].index];
                p[coefAndIndices[k].index] = 0;
            }
            else if(p[coefAndIndices[k].index] > 1)
            {
                del = p[coefAndIndices[k].index]-1;
                p[coefAndIndices[k].index] = 1;
            }

            pSum += p[coefAndIndices[k].index];

            if (pSum > 1)
            {
                p[coefAndIndices[k].index] -= pSum-1;
                del += pSum-1;
                pSum = 1;
            }
        }

        if (pSum < 1)
        {
            p[coefAndIndices[coefAndIndices.length-1].index] += 1-pSum;
        }
    }


    public static int getMaxIndex(double[] arr)
    {
        int maxI = 0;
        double maxV = arr[0];
        int s = arr[0] >= 0 ? 1 : -1;
        for (int i = 1; i < arr.length; i++)
        {
            if (maxV < Math.abs(arr[i]))
            {
                maxV = Math.abs(arr[i]);
                maxI = i;
                s = arr[i] >= 0 ? 1 : -1;
            }
        }

        return maxI*s;
    }


    public static void performIter(Runnable run)
    {
        for (int n = 0; n < N; n++)
        {
            i[n] = 0;
        }
        performIterIntern(0, run);
    }


    public static void performIterIntern(int n, Runnable run)
    {
        if (n < N)
        {
            for (i[n] = 0; i[n] < sNum[n]; i[n]++)
            {
                performIterIntern(n+1, run);
            }
        }
        else
            {
            run.run();
        }
    }


    public static class CoefAndIndex
    {
        public double coef;
        public int index;
    }
}
