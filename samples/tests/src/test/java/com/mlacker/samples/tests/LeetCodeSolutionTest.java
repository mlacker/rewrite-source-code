package com.mlacker.samples.tests;

import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LeetCodeSolutionTest  {

    @Test
    public void test() {
        // assertEquals(1, 1);
    }


    static class UnionFind {
        private final int[] parents;
        private final double[] weights;

        public UnionFind(int count) {
            this.parents = new int[count];
            this.weights = new double[count];
            for (int i = 0; i < count; i++) {
                this.parents[i] = i;
                this.weights[i] = 1.0d;
            }
        }

        public void union(int x, int y, double value) {
            int rootX = find(x);
            int rootY = find(y);
            if (rootX == rootY) {
                return;
            }
            parents[rootX] = rootY;
            weights[rootX] = weights[y] * value / weights[x];
        }

        public int find(int id) {
            int ptr = parents[id];
            if (ptr != id) {
                parents[id] = find(ptr);
                weights[id] *= weights[ptr];
            }
            return parents[id];
        }

        public double isConnect(int x, int y) {
            int rootX = find(x);
            int rootY = find(y);

            if (rootX == rootY) {
                return weights[x] / weights[y];
            } else {
                return -1.0d;
            }
        }
    }

    private void swap(int[] nums, int x, int y) {
        int tmp = nums[x];
        nums[x] = nums[y];
        nums[y] = tmp;
    }

    public static class ListNode {
        int val;
        ListNode next;

        ListNode() {
        }

        ListNode(int val) {
            this.val = val;
        }

        ListNode(int val, ListNode next) {
            this.val = val;
            this.next = next;
        }

        static ListNode ofArray(int[] nums) {
            var dummy = new ListNode();
            var cur = dummy;
            for (int num : nums) {
                cur.next = new ListNode(num);
                cur = cur.next;
            }
            return dummy.next;
        }
    }

    public static class TreeNode {
        int val;
        TreeNode left;
        TreeNode right;

        TreeNode() {
        }

        TreeNode(int val) {
            this.val = val;
        }

        TreeNode(int val, TreeNode left, TreeNode right) {
            this.val = val;
            this.left = left;
            this.right = right;
        }

        public static TreeNode ofArray(Integer[] nums) {
            TreeNode head = null, parent = null, node;
            Queue<TreeNode> queue = new LinkedList<>();
            boolean isLeft = true;
            for (Integer num : nums) {
                node = (num != null) ? new TreeNode(num) : null;
                if (head == null) {
                    head = node;
                    queue.offer(node);
                    continue;
                }
                if (isLeft) {
                    parent = queue.poll();
                    parent.left = node;
                    isLeft = false;
                } else {
                    parent.right = node;
                    isLeft = true;
                }
                if (node != null) {
                    queue.offer(node);
                }
            }
            return head;
        }
    }
}
