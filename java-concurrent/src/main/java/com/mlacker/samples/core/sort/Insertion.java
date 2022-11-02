package com.mlacker.samples.core.sort;

import java.util.Arrays;

public class Insertion {
    public static void sort(int[] numbers, int lo, int hi) {
        Arrays.sort(numbers, lo, hi + 1);
    }
}
