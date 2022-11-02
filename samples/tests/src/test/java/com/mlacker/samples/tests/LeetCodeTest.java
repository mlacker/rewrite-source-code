package com.mlacker.samples.tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

public class LeetCodeTest {

    /**
     * 最长回文子串
     * 给你一个字符串 s，找到 s 中最长的回文子串。
     */
    @Test
    public void L5() {
        // 动态规划， O(n^2) - O(n^2)
        Function<String, String> longestPalindrome = (String s) -> {
            int resLo = 0, resLen = 0, n = s.length();

            // dp[i..j] 表示从 i 到 j 的子串是否为回文字符串
            boolean[] dp = new boolean[n * n];
            // 初始化：所有长度为 1 的子串都是回文串
            for (int i = 0; i < n; i++) {
                dp[i * n + i] = true;
            }

            // 状态转移方程：
            //  P(i, j) = P(i + 1, j - 1) & S[i] == S[j]
            var chars = s.toCharArray();
            for (int len = 1; len < n; len++) {
                for (int lo = 0; lo + len < n; lo++) {
                    int hi = lo + len;

                    if (chars[lo] == chars[hi]) {
                        int pos = lo * n + hi;
                        dp[pos] = len == 1 || dp[pos + n - 1];

                        if (dp[pos] && len > resLen) {
                            resLo = lo;
                            resLen = len;
                        }
                    }
                }
            }

            return s.substring(resLo, resLo + resLen + 1);
        };

        assertEquals("bab", longestPalindrome.apply("babad"));
        assertEquals("bb", longestPalindrome.apply("cbbd"));
    }

    /**
     * 给你一个包含 n 个整数的数组 nums，判断 nums 中是否存在三个元素 a，b，c ，
     * 使得 a + b + c = 0 ？请你找出所有和为 0 且不重复的三元组。
     */
    @Test
    public void L15() {
        // 双指针, O(n^2)
        Function<int[], List<List<Integer>>> threeSum = (int[] nums) -> {
            var res = new ArrayList<List<Integer>>();
            int n = nums.length;

            // 形成有序数列，O(nlogn)
            Arrays.sort(nums);
            for (int i = 0; i < n - 2; i++) {
                // 跳过相同的数字，避免返回相同的数列
                if (i > 0 && nums[i - 1] == nums[i]) continue;

                // 因 a = b + c，且 b < c，假设 b < b` 则 b` < c` < c；
                //  设双指针 lo 和 hi，lo 向右推进，hi 向左收缩。内循环时间复杂度 O(n)
                for (int lo = i + 1, hi = n - 1; lo < hi; lo++) {
                    if (lo > i + 1 && nums[lo - 1] == nums[lo]) continue;

                    int target = -(nums[i] + nums[lo]);
                    while (lo < hi - 1 && nums[hi] > target) {
                        hi--;
                    }

                    if (nums[hi] == target) {
                        res.add(List.of(nums[i], nums[lo], nums[hi]));
                    }
                }
            }

            return res;
        };

        assertIterableEquals(List.of(-1, -1, 2, -1, 0, 1),
                threeSum.apply(new int[]{-1, 0, 1, 2, -1, -4}).stream()
                        .flatMap(Collection::stream).collect(Collectors.toList())
        );

        assertIterableEquals(List.of(-4, 0, 4, -4, 1, 3, -3, -1, 4, -3, 0, 3, -3, 1, 2, -2, -1, 3, -2, 0, 2, -1, -1, 2, -1, 0, 1),
                threeSum.apply(new int[]{-1, 0, 1, 2, -1, -4, -2, -3, 3, 0, 4}).stream()
                        .flatMap(Collection::stream).collect(Collectors.toList())
        );
    }

    /**
     * 数字 n 代表生成括号的对数，请你设计一个函数，用于能够生成所有可能的并且 有效的 括号组合。
     */
    @Test
    public void L22() {
        class Solution {
            public List<String> generateParenthesis(int n) {
                if (n == 1) return List.of("()");

                Set<String> res = new TreeSet<>();
                for (String prts : generateParenthesis(n - 1)) {
                    for (int i = 0; i < prts.length() + 1; i++) {
                        res.add(prts.substring(0, i) + "()" + prts.substring(i));
                    }
                }
                return new ArrayList<>(res);
            }
        }

        var solution = new Solution();

        assertEquals(List.of("()"), solution.generateParenthesis(1));
        assertEquals(List.of("((()))", "(()())", "(())()", "()(())", "()()()"), solution.generateParenthesis(3));
    }

    @Test
    public void L1561() {
        Function<int[], Integer> maxCoins = (int[] piles) -> {
            Arrays.sort(piles);

            int sum = 0;
            for (int i = piles.length / 3; i < piles.length; i += 2) {
                sum += piles[i];
            }
            return sum;
        };

        assertEquals(18, maxCoins.apply(new int[]{9, 8, 7, 6, 5, 1, 2, 3, 4}));
    }
}
