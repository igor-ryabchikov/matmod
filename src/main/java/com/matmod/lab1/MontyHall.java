package com.matmod.lab1;


import java.util.concurrent.ThreadLocalRandom;


public class MontyHall
{
    public static void main(String[] args)
    {
        int roomsCount = 5;
        int carsCount = 2;
        int iterationsCount = 1_000_000;

        int firstCount = 0;
        int secondCount = 0;

        int[] carsAndFirstChoice = new int[carsCount + 1];
        int[] firstChoiceAndOpend = new int[2];
        for (int i = 0; i < iterationsCount; i++)
        {
            clear(carsAndFirstChoice);

            for (int j = 0; j < carsCount; j++)
            {
                carsAndFirstChoice[j] = randNum(roomsCount, carsAndFirstChoice, j);
            }

            carsAndFirstChoice[carsCount] = randNum(roomsCount, null, 0);
            firstChoiceAndOpend[0] = carsAndFirstChoice[carsCount];

            firstChoiceAndOpend[1] = randNum(roomsCount, carsAndFirstChoice, carsAndFirstChoice.length);

            int secondChoice = randNum(roomsCount, firstChoiceAndOpend, 2);

            for (int j = 0; j < carsCount; j++)
            {
                if (firstChoiceAndOpend[0] == carsAndFirstChoice[j])
                {
                    firstCount++;
                }

                if(secondChoice == carsAndFirstChoice[j])
                {
                    secondCount++;
                }
            }
        }

        System.out.println("First: " + ((double)firstCount / iterationsCount) + " Expected: " + ((double)carsCount/roomsCount));

        double s = (double)carsCount/(roomsCount-1);
        double secP = s + (s-(double)carsCount/roomsCount)/(roomsCount-2);

        System.out.println("Second: " + ((double)secondCount / iterationsCount) + " Expected: " + secP);
    }


    public static void clear(int[] arr)
    {
        for (int i = 0; i < arr.length; i++)
        {
            arr[i] = -1;
        }
    }


    public static int randNum(int tillNum, int[] except, int exceptCount)
    {
        int num = ThreadLocalRandom.current().nextInt(tillNum-exceptCount);

        if (except == null) return num;

        label: for (int i = 0; i < tillNum; i++)
        {
            for (int j = 0; j < except.length; j++)
            {
                if (i == except[j])
                {
                    continue label;
                }
            }

            if (num == 0)
            {
                return i;
            }

            num--;
        }

        throw new RuntimeException("Should not happen");
    }
}