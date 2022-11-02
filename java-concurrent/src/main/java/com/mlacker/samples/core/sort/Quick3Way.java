package com.mlacker.samples.core.sort;

public class Quick3Way implements Sort {

    // if hi - lo <= M, should be use InsertionSort, M between 5 to 15.
    private static final int INSERTION_SORT_THRESHOLD = 47;

    @Override
    public void sort(int[] numbers) {
        sort(numbers, 0, numbers.length - 1);
    }

    public void sort(int[] numbers, int lo, int hi) {
        if (hi <= lo + INSERTION_SORT_THRESHOLD) {
            Insertion.sort(numbers, lo, hi);
            return;
        }

        int eq = lo + 1;
        int lt = lo;
        int gt = hi;
        int key = numbers[lo];

        while (eq <= gt) {
            int cmp = Integer.compare(numbers[eq], key);
            if (cmp < 0) exch(numbers, lt++, eq++);
            else if (cmp> 0)  exch(numbers, eq, gt--);
            else eq++;
        }
        sort(numbers, lo, lt - 1);
        sort(numbers, gt + 1, hi);
    }

    private void exch(int[] numbers, int x, int y) {
        int t = numbers[x];
        numbers[x] = numbers[y];
        numbers[y] = t;
    }
}
