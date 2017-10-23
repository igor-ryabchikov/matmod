package com.matmod.lab3;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Created by igor on 06.10.17.
 */
public class TicTacToe {

    private static long c = 0;

    private static final int X = 1;
    private static final int O = -1;
    private static final int DRAW = 0;
    private static final int EMPTY = 0;
    private static final int NONE = -2;
    private static final int SYM = -3;

    private static final Symmetry[] SYM_FUNCTIONS = new Symmetry[]{new HorizRef(), new VertRef(), new MainDiagRef(),
            new SideDiagRef(), new Rot1Ref(), new Rot2Ref(), new Rot3Ref()};

    private static final int N = 4;

    private static final int MAX_DEPTH_FOR_SYM = 9; // 11

    public static final Map<Long, IterationResult> cachedResults = new HashMap<>(1_000_000);

    public static final int MAX_DEPTH_FOR_CACH = 15;
    // -----------------

    /** Используется
     * для записи функций отражения, которые справедливы для доски с текущим. Заполнена null перед следующим
     * вызовом функции*/
    private static final Symmetry[] symmetries = new Symmetry[7];

    /** Используется для хранения предварительных результатов */
    private static final int[] placedCount = new int[N*2+2];

    /** Используется для хранения предварительных результатов */
    private static final int[] placedSum = new int[N*2+2];

    /** Используется для хранения списка точкек, которые будут проверяться при очередном вызове */
    private static final int[][] checkingPositions = new int[N*N][N*N*2];
    private static final int[][] symmetriesToAccount = new int[N*N][N*N];

    private static final int[] varRes = new int[N*N];
    private static final int[] varRecI = new int[N*N];
    private static final int[] varRecJ = new int[N*N];
    private static final double[] varWinXP = new double[N*N];
    private static final double[] varWinOP = new double[N*N];
    private static final double[] varDrawP = new double[N*N];

    private static int notDrawCounter = 2*N+2;

    // ------------------

    public static void main(String[] args) {

        MutLong board = new MutLong();

        //playGame(board);
        performBest(board);

        printBoard(board);
    }

    /**
     * X O O X
     * O X X O
     * X O - -
     * X - X O
     *
     * @param board
     */
    public static void performBest(MutLong board)
    {
        //board[1][1] = X;
        //board[1][0] = O;

        int turn = X;

        MutInteger result = new MutInteger(), recI = new MutInteger(), recJ = new MutInteger();
        MutDouble winXP = new MutDouble(), winOP = new MutDouble(), drawP = new MutDouble();

        int num = 0;
        for (int i = 0; i < N*N; i++) {
// TODO - возможно. ошибка - очень странно рекомендует - рекомендукт не в середину ходить.
            // !!! Можно заменить на полную проверку и т.о.  проверить, не с Draw ли проблема
            countNextMove(X, board, turn, result, recI, recJ, winXP, winOP, drawP, 0, -1, -1, -1);
            if (recI.get() == -1)
            {
                break;
            }

            BU.set(recI.get(),recJ.get(), board, turn);

            turn = -1*turn;

            System.out.println("n:"+(num++)+" res:"+getConstName(result.get())+" i:"+recI.get()+" j:"+recJ.get()+" winXP:"+winXP.get()+" winOP:"+winOP.get()+" drawP:"+drawP.get());
        }

        System.out.format("Result: %s, i: %d, j: %d, xP: %f, oP: %f, drawP: %f \n", getConstName(result.get()), recI.get(), recJ.get(), winXP.get(), winOP.get(), drawP.get());

    }


    public static void printBoard(MutLong board)
    {
        System.out.println();

        for (int i = 0; i < N; i++)
        {
            for (int j = 0; j < N; j++)
            {
                String name = "";
                switch (BU.get(i,j,board))
                {
                    case X: name = "X"; break;
                    case O: name = "O"; break;
                    default: name = "-";
                }

                System.out.print(name+" ");
            }
            System.out.println();
        }
    }


    public static void playGame(MutLong board)
    {
        MutInteger result = new MutInteger(), recI = new MutInteger(), recJ = new MutInteger();
        MutDouble winXP = new MutDouble(), winOP = new MutDouble(), drawP = new MutDouble();

        int turn = X;
        Scanner sc = new Scanner(System.in);
        for (;;)
        {
            countNextMove(X, board, turn, result, recI, recJ, winXP, winOP, drawP, 0, -1, -1, -1);
            if (recI.get() == -1)
            {
                break;
            }

            System.out.println("res:"+getConstName(result.get())+" i:"+recI.get()+" j:"+recJ.get()+" winXP:"+winXP.get()+" winOP:"+winOP.get()+" drawP:"+drawP.get());

            System.out.print("Input i j for "+getConstName(turn)+": ");
            int readI = sc.nextInt();
            int readJ = sc.nextInt();

            if (BU.get(readI, readJ, board) != EMPTY)
            {
                System.out.println("Already set!");
                continue;
            }

            BU.set(readI, readJ, board, turn);

            turn = -1*turn;
        }
    }


    public static String getConstName(int c)
    {
        String name = "";
        switch (c)
        {
            case X: name = "X"; break;
            case O: name = "O"; break;
            case DRAW: name = "DRAW"; break;
            case NONE: name = "NONE"; break;
        }

        return name;
    }


    /**
     * @param board текущая доска
     * @param turn чья очередь сейчас - {@link #X} или {@link #O}
     * @param result кузультат, который может получиться при выборе рекомендованного хода - {@link #X}, или {@link #O} или {@link #DRAW}
     * @param recI сюда записывается I лучшего хода
     * @param recJ сюда записывается J лучшего хода
     */
    public static void countNextMove(int player, MutLong board, int turn, MutInteger result, MutInteger recI,
                                     MutInteger recJ, MutDouble winXP, MutDouble winOP, MutDouble drawP, int depth, int lastI, int lastJ, int lastPosIndex)
    {
        if (depth < MAX_DEPTH_FOR_CACH)
        {
            IterationResult res = cachedResults.get(board.get());
            if (res != null)
            {
                result.set(res.result);
                recI.set(res.recI);
                recJ.set(res.recJ);
                winXP.set(res.winXP);
                winOP.set(res.winOP);
                drawP.set(1-res.winOP-res.winXP);

                return;
            }
        }

        // ---

        c++;
        if (c % 10_000_000 == 0) System.out.println("C: "+c+" "+System.currentTimeMillis());

        if (depth == 0)
        {
            init(board);
        }

        winXP.set(0);
        winOP.set(0);
        drawP.set(0);

        // Проверяем условие остановки
        result.set(checkFinished(board, lastI, lastJ));
        //result.set(depth == N*N ? DRAW : NONE);
        if (result.get() == X)
        {
            winXP.set(1);
        }
        else if (result.get() == O)
        {
            winOP.set(1);
        }
        else if(result.get() == DRAW)
        {
            drawP.set(1);
        }

        if (result.get() != NONE)
        {
            recI.set(-1);
            recJ.set(-1);

            clearFinishCheck(board, lastI, lastJ);
            return;
        }
        
        // Определяем симметрии
        int posIndex = 0;
        if (depth <= MAX_DEPTH_FOR_SYM)
        {
            countSymmetry(board, symmetries);

            for (int i = 0; i < N; i++)
            {
                for (int j = 0; j < N; j++)
                {
                    if (BU.get(i,j, board) == EMPTY)
                    {
                        BU.set(i,j,board,SYM);

                        checkingPositions[depth][posIndex] = i;
                        checkingPositions[depth][posIndex+1] = j;
                        symmetriesToAccount[depth][posIndex/2] = 0;
                        posIndex += 2;

                        // В последнюю итерацию проверку симметрии не производим
                        if (depth != MAX_DEPTH_FOR_SYM) {
                            for (int k = 0; k < symmetries.length; k++) {
                                if (symmetries[k] == null) {
                                    break;
                                }

                                int refI = symmetries[k].refI(i, j, N);
                                int refJ = symmetries[k].refJ(i, j, N);
                                if (BU.get(refI,refJ,board) == EMPTY) {
                                    BU.set(refI,refJ,board,SYM);
                                    symmetriesToAccount[depth][(posIndex-2)/2]++;
                                }
                            }
                        }
                    }
                }
            }

            checkingPositions[depth][posIndex] = -1;

            // Очищаем массив
            for (int i = 0; i < N; i++)
            {
                for (int j = 0; j < N; j++)
                {
                    if (BU.get(i,j,board) == SYM)
                    {
                        BU.set(i,j,board,EMPTY);
                    }
                }
            }
        }
        else
        {
            posIndex = performDeepIter(depth, lastPosIndex);
            /*for (int k = 0; k < checkingPositions[depth-1].length; k += 2)
            {
                if (checkingPositions[depth-1][k] == -1)
                {
                    break;
                }

                if (k == lastPosIndex)
                {
                    continue;
                }

                checkingPositions[depth][posIndex] = checkingPositions[depth-1][k];
                checkingPositions[depth][posIndex+1] = checkingPositions[depth-1][k+1];
                posIndex += 2;
            }

            checkingPositions[depth][posIndex] = -1;*/
        }

        // Проверяем каждую возможную позицию (с учетом симметрии)

        // Определяем проверяемые позиции


        // Проверяем позиции
        varRes[depth]=(-2*turn); // На первой итерации значение будет выставлено

        double winXPSum = 0, winOPSum = 0, drawPSum = 0;
        int accountedPos = 0;
        for (int k = 0; k < posIndex; k += 2)
        {
            int i = checkingPositions[depth][k];
            int j = checkingPositions[depth][k+1];

            BU.set(i,j,board,turn);

            //BU.print(board);
            countNextMove(player, board, -1*turn, result, recI, recJ, winXP, winOP, drawP, depth+1, i, j, k);

            // Определяем выходные res, recI, recJ, а также winXP, winOP, drawP в случае player == turn
            checkAndUpdate(turn, varRes, varRecI, varRecJ, varWinXP, varWinOP, varDrawP, result.get(), i, j,
                    winXP.get(), winOP.get(), drawP.get(), depth);

            if (player != turn)
            {
                // Считаем вероятности как сумму
                winXPSum += winXP.get()*(1+symmetriesToAccount[depth][k/2]);
                winOPSum += winOP.get()*(1+symmetriesToAccount[depth][k/2]);
                drawPSum += drawP.get()*(1+symmetriesToAccount[depth][k/2]);
                accountedPos += 1+symmetriesToAccount[depth][k/2];
            }

            BU.set(i,j,board,EMPTY);
        }

        if (player == turn)
        {
            winXP.set(varWinXP[depth]);
            winOP.set(varWinOP[depth]);
            drawP.set(varDrawP[depth]);
        }
        else
        {
            winXP.set(winXPSum / accountedPos);
            winOP.set(winOPSum / accountedPos);
            drawP.set(drawPSum / accountedPos);
        }

        result.set(varRes[depth]);
        recI.set(varRecI[depth]);
        recJ.set(varRecJ[depth]);

        clearFinishCheck(board, lastI, lastJ);

        // Caching
        if (depth < MAX_DEPTH_FOR_CACH)
        {
            IterationResult res = new IterationResult((byte)result.get(), (short)recI.get(), (short)recJ.get(), (float)winXP.get(), (float)winOP.get());
            cachedResults.put(board.get(), res);
        }

    }


    public static int performDeepIter(int depth, int lastPosIndex)
    {
        int posIndex = 0;
        for (int k = 0; k < checkingPositions[depth-1].length; k += 2)
        {
            if (checkingPositions[depth-1][k] == -1)
            {
                break;
            }

            if (k == lastPosIndex)
            {
                continue;
            }

            checkingPositions[depth][posIndex] = checkingPositions[depth-1][k];
            checkingPositions[depth][posIndex+1] = checkingPositions[depth-1][k+1];
            posIndex += 2;
        }

        checkingPositions[depth][posIndex] = -1;

        return posIndex;
    }


    public static void checkAndUpdate(int player, int[] res, int[] recI, int[] recJ, double[] varWinXP,
                                      double[] varWinOP, double[] varDrawP, int newRes, int newRecI, int newRecJ,
                                      double newWinXP, double newWinOP, double newDrawP, int depth)
    {
        // Мат ожидание от результата
        if (player == X && (res[depth] < newRes || res[depth] == newRes && (varWinXP[depth]+varWinOP[depth] < newWinXP+newWinOP))
                || player == O && (res[depth] > newRes || res[depth] == newRes && (varWinXP[depth]+varWinOP[depth] > newWinXP+newWinOP)))
        {
            res[depth]= newRes;
            recI[depth]=newRecI;
            recJ[depth]=newRecJ;
            varWinXP[depth]=newWinXP;
            varWinOP[depth]=newWinOP;
            varDrawP[depth]=newDrawP;
        }

        /*if (player == X && (res[depth] < newRes || res[depth] == newRes && varWinXP[depth] < newWinXP || res[depth] == newRes && varWinXP[depth] == newWinXP && varDrawP[depth] < newDrawP)
                || player == O && (res[depth] > newRes || res[depth] == newRes && varWinOP[depth] < newWinOP || res[depth] == newRes && varWinOP[depth] == newWinOP && varDrawP[depth] < newDrawP))
        {
            res[depth]= newRes;
            recI[depth]=newRecI;
            recJ[depth]=newRecJ;
            varWinXP[depth]=newWinXP;
            varWinOP[depth]=newWinOP;
            varDrawP[depth]=newDrawP;
        }*/
    }


    public static void clearFinishCheck(MutLong board, int lastI, int lastJ)
    {
        if (lastI == -1 || lastJ == -1)
        {
            return;
        }

        int oldC1 = placedCount[lastI];
        int oldS1 = placedSum[lastI];
        int oldC2 = placedCount[N+lastJ];
        int oldS2 = placedSum[N+lastJ];
        int oldC3 = placedCount[2 * N];
        int oldS3 = placedSum[2 * N];
        int oldC4 = placedCount[2 * N + 1];
        int oldS4 = placedSum[2 * N + 1];

        placedCount[lastI]--;
        placedSum[lastI] -= BU.get(lastI,lastJ,board);

        placedCount[N+lastJ]--;
        placedSum[N+lastJ] -= BU.get(lastI,lastJ,board);

        if (lastI == lastJ) {
            placedCount[2 * N]--;
            placedSum[2 * N] -= BU.get(lastI,lastJ,board);
        }
        if (lastI == N-1-lastJ) {
            placedCount[2 * N + 1]--;
            placedSum[2 * N + 1] -= BU.get(lastI,lastJ,board);
        }

        if (oldS1/oldC1 == 0 && placedSum[lastI]/placedCount[lastI] != 0)
        {
            notDrawCounter++;
        }
        if(oldS2/oldC2 == 0 && placedSum[N+lastJ]/placedCount[N+lastJ] != 0)
        {
            notDrawCounter++;
        }
        if(lastI == lastJ && oldS3/oldC3 == 0 && placedSum[2*N]/placedCount[2*N] != 0)
        {
            notDrawCounter++;
        }
        if(lastI == N-1-lastJ && oldS4/oldC4 == 0 && placedSum[2*N+1]/placedCount[2*N+1] != 0)
        {
            notDrawCounter++;
        }
    }

    /**
     * Определяет, завершена ли партия.
     * Проверяем все горизонтали, все вертикали и две диагонали. Если есть выстроенная линия - выигрыш, если на всех
     * линиях есть и X и O - ничья, иначе - игра продолжается.
     *
     * @return {@link #X} - если выиграл X, {@link #O} - если выиграл O, {@link #DRAW} - если ничья, {@link #NONE} - если игра продолжается
     */
    public static int checkFinished(MutLong board, int lastI, int lastJ)
    {
        if (lastI == -1 || lastJ == -1)
        {
            return checkFinishedFull(board);
        }

        int oldC1 = placedCount[lastI];
        int oldS1 = placedSum[lastI];
        int oldC2 = placedCount[N+lastJ];
        int oldS2 = placedSum[N+lastJ];
        int oldC3 = placedCount[2 * N];
        int oldS3 = placedSum[2 * N];
        int oldC4 = placedCount[2 * N + 1];
        int oldS4 = placedSum[2 * N + 1];


        placedCount[lastI]++;
        placedSum[lastI] += BU.get(lastI,lastJ,board);

        placedCount[N+lastJ]++;
        placedSum[N+lastJ] += BU.get(lastI,lastJ,board);

        if (lastI == lastJ) {
            placedCount[2 * N]++;
            placedSum[2 * N] += BU.get(lastI,lastJ,board);
        }
        if (lastI == N-1-lastJ) {
            placedCount[2 * N + 1]++;
            placedSum[2 * N + 1] += BU.get(lastI,lastJ,board);
        }

        // TODO - Очень долго!! Возможно, проблема в делении? - проверить
        if ((oldC1==0 || oldS1/oldC1 != 0) && placedSum[lastI]/placedCount[lastI] == 0)
        {
            notDrawCounter--;
        }
        if((oldC2 == 0 || oldS2/oldC2 != 0) && placedSum[N+lastJ]/placedCount[N+lastJ] == 0)
        {
            notDrawCounter--;
        }
        if((oldC3 == 0 || oldS3/oldC3 != 0) && lastI == lastJ && placedSum[2*N]/placedCount[2*N] == 0)
        {
            notDrawCounter--;
        }
        if((oldC4 == 0 || oldS4/oldC4 != 0) && lastI == N-1-lastJ && placedSum[2*N+1]/placedCount[2*N+1] == 0)
        {
            notDrawCounter--;
        }

        if (placedSum[lastI] == N || placedSum[N+lastJ] == N || lastI == lastJ && placedSum[2*N] == N || lastI == N-1-lastJ && placedSum[2*N+1] == N)
        {
            return X;
        }
        else if(placedSum[lastI] == -N || placedSum[N+lastJ] == -N || lastI == lastJ && placedSum[2*N] == -N || lastI == N-1-lastJ && placedSum[2*N+1] == -N)
        {
            return O;
        }
        else if(notDrawCounter == 0)
        {
            return DRAW;
        }

        return NONE;

        /**/
    }


    public static int checkFinishedFull(MutLong board)
    {
        int[] placedCountL = new int[placedCount.length];
        int[] placedSumL = new int[placedSum.length];

        boolean isDraw = true;
        for (int i = 0; i < N; i++)
        {
            for (int j = 0; j < N; j++)
            {
                // Проверяем горизонтали и вертикали

                if (BU.get(i,j,board) != EMPTY)
                {
                    placedCountL[i]++;
                    placedSumL[i] += BU.get(i,j,board);
                }

                if (BU.get(j,i,board) != EMPTY)
                {
                    placedCountL[N+i]++;
                    placedSumL[N+i] += BU.get(j,i,board);
                }
            }

            // Проверяем диагонали

            if (BU.get(i,i,board) != EMPTY)
            {
                placedCountL[2*N]++;
                placedSumL[2*N] += BU.get(i,i,board);
            }

            if (BU.get(i,N-1-i,board) != EMPTY)
            {
                placedCountL[2*N+1]++;
                placedSumL[2*N+1] += BU.get(i,N-1-i,board);
            }
        }

        for (int i = 0; i < placedCount.length; i++)
        {
            if (placedSumL[i] == N)
            {
                return X;
            }
            else if(placedSumL[i] == -N)
            {
                return O;
            }
            else if(placedCountL[i] == 0 || placedSumL[i] / placedCountL[i] != 0)
            {
                // X и O на одной линии не содержится
                isDraw = false;
            }
        }

        return isDraw ? DRAW : NONE;
    }


    public static void init(MutLong board)
    {
        notDrawCounter = 0;

        for (int i = 0; i < placedCount.length; i++)
        {
            placedCount[i] = 0;
            placedSum[i] = 0;
        }

        for (int i = 0; i < N; i++)
        {
            for (int j = 0; j < N; j++)
            {
                // Проверяем горизонтали и вертикали

                if (BU.get(i,j,board) != EMPTY)
                {
                    placedCount[i]++;
                    placedSum[i] += BU.get(i,j,board);
                }

                if (BU.get(j,i,board) != EMPTY)
                {
                    placedCount[N+i]++;
                    placedSum[N+i] += BU.get(j,i,board);
                }
            }

            // Проверяем диагонали

            if (BU.get(i,i,board) != EMPTY)
            {
                placedCount[2*N]++;
                placedSum[2*N] += BU.get(i,i,board);
            }

            if (BU.get(i,N-1-i,board) != EMPTY)
            {
                placedCount[2*N+1]++;
                placedSum[2*N+1] += BU.get(i,N-1-i,board);
            }
        }


        for (int i = 0; i < placedCount.length; i++)
        {
            if (placedCount[i] == 0 || placedSum[i]/placedCount[i] != 0)
            {
                notDrawCounter++;
            }
        }
    }


    /**
     * Проверяет симметричность доски по 7ми возможным симметриям (по гориз., верт., глав. диаг. и побоч. диаг. доски,
     * а также по трем поворотам).
     *
     * @param board проверяемая доска
     */
    public static void countSymmetry(MutLong board, Symmetry[] symmetries)
    {
        for (int i = 0 ; i < symmetries.length; i++)
        {
            symmetries[i] = SYM_FUNCTIONS[i];
        }

        for (int i = 0; i < N; i++)
        {
            for (int j = 0; j < N; j++)
            {
                if (symmetries[0] == null)
                {
                    // Симметрий нет
                    return;
                }

                for (int k = 0; k < symmetries.length; k++)
                {
                    // Возможные симметрии расположены только в начале
                    if (symmetries[k] == null)
                    {
                        break;
                    }

                    if (BU.get(i,j,board) != BU.get(symmetries[k].refI(i,j,N),symmetries[k].refJ(i,j,N),board))
                    {
                        // Симметрия не подтвержилать - сдвигаем

                        for (int t = k; t < symmetries.length-1; t++)
                        {
                            symmetries[t] = symmetries[t+1];
                        }
                        symmetries[symmetries.length-1] = null;

                        k--;
                    }
                }
            }
        }
    }


    /**
     * Интерфейс для функций отражений (для четырех осей и трех поворотов).
     */
    public interface Symmetry
    {
        int refI(int i, int j, int n);
        int refJ(int i, int j, int n);
    }


    public static class HorizRef implements Symmetry
    {
        @Override
        public int refI(int i, int j, int n) {
            return n-1-i;
        }

        @Override
        public int refJ(int i, int j, int n) {
            return j;
        }
    }


    public static class VertRef implements Symmetry
    {
        @Override
        public int refI(int i, int j, int n) {
            return i;
        }

        @Override
        public int refJ(int i, int j, int n) {
            return n-1-j;
        }
    }


    public static class MainDiagRef implements Symmetry
    {
        @Override
        public int refI(int i, int j, int n) {
            return j;
        }

        @Override
        public int refJ(int i, int j, int n) {
            return i;
        }
    }


    public static class SideDiagRef implements Symmetry
    {
        @Override
        public int refI(int i, int j, int n) {
            return n-1-j;
        }

        @Override
        public int refJ(int i, int j, int n) {
            return n-1-i;
        }
    }


    public static class Rot1Ref implements Symmetry
    {
        @Override
        public int refI(int i, int j, int n) {
            return j;
        }

        @Override
        public int refJ(int i, int j, int n) {
            return n-1-i;
        }
    }


    public static class Rot2Ref extends Rot1Ref
    {
        @Override
        public int refI(int i, int j, int n) {
            return super.refJ(i, j, n);
        }

        @Override
        public int refJ(int i, int j, int n) {
            return n-1-super.refI(i, j, n);
        }
    }


    public static class Rot3Ref extends Rot2Ref
    {
        @Override
        public int refI(int i, int j, int n) {
            return super.refJ(i, j, n);
        }

        @Override
        public int refJ(int i, int j, int n) {
            return n-1-super.refI(i, j, n);
        }
    }


    public static class MutLong
    {
        long num = 0;

        public void set(long num)
        {
            this.num = num;
        }

        public long get()
        {
            return num;
        }
    }


    public static class MutInteger
    {
        int num = 0;

        public void set(int num)
        {
            this.num = num;
        }

        public int get()
        {
            return num;
        }
    }


    public static class MutDouble
    {
        double num;

        public void set(double num)
        {
            this.num = num;
        }

        public double get()
        {
            return num;
        }
    }

    public static class BU
    {
        public static int get(int i, int j, MutLong board)
        {
            // 00 - EMPTY, 01 - X, 10 - O, 11 - SYM
            int v = (int)((board.get() << 62-(i*N+j)*2) >>> 62);
            return v == 2 ? O : v == 3 ? SYM : v;
        }


        public static void set(int i, int j, MutLong board, int val)
        {
            int pos = (i*N+j)*2;
            long v = val == O ? 2 : val == SYM ? 3 : val;
            board.set((board.get() & (~(3L << pos))) | (v << pos));
        }


        public static void print(MutLong board)
        {
            System.out.println();
            for (int i = 0; i < N; i++)
            {
                for (int j = 0; j < N; j++)
                {
                    System.out.print(get(i,j,board)+" ");
                }
                System.out.println();
            }
        }
    }


    public static class IterationResult {
        public byte result;
        public short recI;
        public short recJ;
        public float winXP;
        public float winOP;

        public IterationResult(byte result, short recI, short recJ, float winXP, float winOP)
        {
            this.result = result;
            this.recI = recI;
            this.recJ = recJ;
            this.winXP = winXP;
            this.winOP = winOP;
        }
    }
}
