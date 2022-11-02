package com.mlacker.samples.core.sort;

public class Quick implements Sort {

    @Override
    public void sort(int[] numbers) {
        sort(numbers, 0, numbers.length - 1);
    }

    public void sort(int[] numbers, int lo, int hi) {
        if (hi <= lo) return;
        int key = numbers[lo];
        int lt = lo;
        int gt = hi;

        while (lt < gt) {
            while (key < numbers[gt]) {
                gt--;
            }
            numbers[lt] = numbers[gt];
            while (lt < gt && key >= numbers[lt]) {
                lt++;
            }
            numbers[gt] = numbers[lt];
        }
        numbers[lt] = key;

        sort(numbers, lo, lt - 1);
        sort(numbers, lt + 1, hi);
    }
}
