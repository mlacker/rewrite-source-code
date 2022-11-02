package com.mlacker.samples.tests;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

/**
 * 用有限内存（比如256MB），处理一个大文件（比如4GB），该文件每行都只有一个全局不重复的乱序数字（正整数），排序好输出到一个新的文件。
 * 为了节约时间，定义大文件是：100w行，内存能处理1w行（排序+输出）
 * 第一步：按要求生成一个文件input.txt
 * 第二步：按题目要求处理该文件，最终输出成output.txt
 * 第三步：统计排序算法的耗时，通过并发编程，在确保结果争取的基础上，如何减少耗时。
 */
public class FileSortInLimitedSpace {

    private static final String INPUT_FILE = "input.txt";
    private static final String OUTPUT_FILE = "output.txt";
    private static final Integer ROWS_IN_INPUT = 1_000_000;
    private static final Integer ROWS_LIMITED = 10_000;
    private final int[] buffers = new int[ROWS_LIMITED];

    /**
     * 生成 100w 行唯一的随机正整数的 input 文件
     */
    public void generateInput() throws IOException {
        // 用于快速判断生成的数字是否重复，已使用的数字在位图中相应位置的 bit 位设置为 1
        long[] bitmap = new long[(Integer.MAX_VALUE - 1 >>> 6) + 1];

        var random = new Random();
        var path = Paths.get(INPUT_FILE);
        Files.deleteIfExists(path);

        // 使用缓冲提升写入性能
        try (var bufferedWriter = Files.newBufferedWriter(path)) {
            int i = 0;
            while (i < ROWS_IN_INPUT) {
                // 生成正整数
                int value = random.nextInt(Integer.MAX_VALUE);

                // 计算值在位图中的槽的索引，和槽中的掩码。判断值是否重复
                //  因为是索引访问，性能非常高，时间复杂度 O(1)
                int bitSlot = value >> 6;
                long bitMask = 1L << value;
                if ((bitmap[bitSlot] & bitMask) == 0) {
                    bitmap[bitSlot] |= bitMask;

                    bufferedWriter.write(String.valueOf(value));
                    bufferedWriter.newLine();

                    // 生成唯一数字
                    i++;
                }
            }

            bufferedWriter.flush();
        }
    }

    /**
     * 排序 input 文件，输出到 output 文件中。限制内存同时能最多处理 1W 行记录
     */
    public void sortFile() throws IOException {
        int stagingNumber = 0;
        int[] numbers = this.buffers;

        try (var bufferedReader = Files.newBufferedReader(Paths.get(INPUT_FILE))) {
            boolean eof = false;
            while (!eof) {
                int i = 0, maxRead;
                while (i < numbers.length) {
                    // 单文件句柄的并发访问性能不理想，
                    //  1. readLine 通过 synchronized 关键字实现线程安全，
                    //      多线程访问不能提高读取速度，反而因为阻塞调度降低吞吐量
                    //  2. 如果将文件拆成多段读取，随机 IO 效率低于顺序 IO
                    var line = bufferedReader.readLine();

                    if (line == null) {
                        eof = true;
                        break;
                    }

                    if (line.isEmpty()) {
                        continue;
                    }

                    numbers[i++] = Integer.parseInt(line);
                }

                maxRead = i;
                while (i < numbers.length) {
                    // 重置未填充的空间，虽然不会访问到这些空间但是为了安全
                    numbers[i++] = -1;
                }

                // 快速排序内存中的记录
                //  这里使用 maxRead 而非 numbers.length 是因为最后一段可能未填充满 numbers
                quickSort(numbers, 0, maxRead - 1);

                // 将 ROWS_LIMITED 行记录分割到两组临时文件存储
                staging(stagingNumber++, numbers, 0, numbers.length / 2);
                staging(stagingNumber++, numbers, numbers.length / 2, numbers.length);
            }
        }

        var mergeSort = new MergeSort(this.buffers, stagingNumber);
        mergeSort.mergeSort(true, 0, stagingNumber - 1);

        // 写入 output 文件
        flushOutput(stagingNumber);
    }

    private void quickSort(int[] numbers, int min, int max) {
        if (min >= max) {
            return;
        }

        int i = min;
        int j = max;
        final int key = i;

        while (i <= j) {
            while (i < j && numbers[j] > key) {
                j--;
            }
            numbers[i] = numbers[j];

            while (i < j && numbers[i] >= key) {
                i++;
            }
            numbers[j] = numbers[i];
        }
        numbers[i] = key;

        quickSort(numbers, min, i - 1);
        quickSort(numbers, i + 1, max);
    }

    private void staging(int fileNumber, int[] numbers, int from, int to) throws IOException {
        var stagingPath = getStagingPath(0, fileNumber);
        try (var outputStream = Files.newOutputStream(stagingPath)) {
            for (int i = from; i < to; i++) {
                outputStream.write(numbers[i] >> 24);
                outputStream.write(numbers[i] >> 16);
                outputStream.write(numbers[i] >> 8);
                outputStream.write(numbers[i]);
            }
            outputStream.flush();
        }
    }

    private static Path getStagingPath(int group, int fileNumber) {
        return Paths.get(".staging", String.format("sorted%d-%4d.tmp", group, fileNumber));
    }

    public static class MergeSort {

        private final int[] buffers;
        private final Path[] stagingPath0;
        private final Path[] stagingPath1;

        public MergeSort(int[] buffers, int stagingNumber) {
            this.buffers = buffers;
            this.stagingPath0 = new Path[stagingNumber];
            this.stagingPath1 = new Path[stagingNumber];
            for (int i = 0; i < stagingNumber; i++) {
                this.stagingPath0[i] = getStagingPath(0, i);
                this.stagingPath1[i] = getStagingPath(1, i);
            }
        }

        /**
         * 归并排序，递归分治
         */
        private void mergeSort(boolean staging0, int min, int max) throws IOException {
            var size = max - min;
            if (size < 2) {
                return;
            }

            int middle = size / 2 + 1;

            // 分而治之
            mergeSort(!staging0, min, middle - 1);
            mergeSort(!staging0, middle, max);

            merge(staging0, min, max, middle);
        }

        private void merge(boolean staging0, int min, int max, int middle) throws IOException {
            // 生成两组暂存文件交替运行，归并到另一组。
            var fromStagings = (staging0) ? this.stagingPath0 : this.stagingPath1;
            var toStagings = (!staging0) ? this.stagingPath0 : this.stagingPath1;

            final int bufferStep = this.buffers.length / 4;
            // 利用内存缓冲加速，将缓冲区分为 4个区域，
            //  左、右归并区各执 1个区域，归并结果占用 2个区域
            //  并且将文件分为左右两组，批量顺序读入缓冲区，运算排序后写入 resultGroup 文件组。
            //  当缓冲区执行完毕后，继续加载下一批数据
            //  resultGroup 的写入原理相同。
            var leftGroup = new MergeSortFileGroup(this.buffers, bufferStep * 2, bufferStep * 3, fromStagings, min, middle - 1);
            var rightGroup = new MergeSortFileGroup(this.buffers, bufferStep * 3, bufferStep * 4, fromStagings, middle, max);
            var resultGroup = new MergeSortFileGroup(this.buffers, 0, bufferStep * 2, toStagings, min, max);

            while (leftGroup.hasNext() && rightGroup.hasNext()) {
                if (leftGroup.get() < rightGroup.get()) {
                    resultGroup.write(leftGroup.getAndNext());
                } else {
                    resultGroup.write(rightGroup.getAndNext());
                }
            }

            while (leftGroup.hasNext()) {
                resultGroup.write(leftGroup.getAndNext());
            }

            while (rightGroup.hasNext()) {
                resultGroup.write(rightGroup.getAndNext());
            }
        }
    }

    public static class MergeSortFileGroup {
        private final int[] buffer;
        private final int bufferStart;
        private int bufferEnd;
        private int currentBufferPos;

        private final Path[] stagingFiles;
        private final int stagingLength;
        private int currentStagingIndex;
        private long lastByteOfStagingRead;

        private boolean eof = false;

        public MergeSortFileGroup(int[] buffer, int bufferStart, int bufferEnd, Path[] stagingFiles, int stagingStart, int stagingLength) {
            this.buffer = buffer;
            this.bufferStart = bufferStart;
            this.bufferEnd = bufferEnd;
            this.stagingFiles = stagingFiles;
            this.stagingLength = stagingLength;

            this.currentBufferPos = this.bufferStart;
            this.currentStagingIndex = stagingStart;
            this.lastByteOfStagingRead = 0;
        }

        public boolean hasNext() {
            return !eof && currentBufferPos < bufferEnd;
        }

        public int get() {
            return buffer[currentBufferPos];
        }

        public int getAndNext() throws IOException {
            int number = buffer[currentBufferPos++];

            if (currentBufferPos >= bufferEnd) {
                paddingToBuffer();
            }

            return number;
        }

        public void write(int number) throws IOException {
            buffer[currentBufferPos++] = number;

            if (currentBufferPos >= bufferEnd) {
                flushBuffer();
            }
        }

        private void paddingToBuffer() throws IOException {
            int i = bufferStart;
            loopStaging:
            while (this.currentStagingIndex < this.stagingLength) {
                var path = this.stagingFiles[this.currentStagingIndex];
                try (var inputStream = Files.newInputStream(path)) {
                    inputStream.skip(lastByteOfStagingRead);

                    while (i < bufferEnd) {
                        int number = inputStream.read() << 24
                                | inputStream.read() << 16
                                | inputStream.read() << 8
                                | inputStream.read();
                        if (number == -1) {
                            this.currentStagingIndex++;
                            this.lastByteOfStagingRead = 0;

                            if (this.currentStagingIndex < this.stagingLength) {
                                continue loopStaging;
                            } else {
                                eof = true;
                                bufferEnd = i;
                                break loopStaging;
                            }
                        }
                        buffer[i++] = number;
                    }

                    lastByteOfStagingRead += (i - bufferStart) * 4;
                    break;
                }
            }

            currentBufferPos = this.bufferStart;
        }

        private void flushBuffer() throws IOException {
            int i = bufferStart;
            loopStaging:
            while (this.currentStagingIndex < this.stagingLength) {
                var path = this.stagingFiles[this.currentStagingIndex];
                try (var outputStream = Files.newOutputStream(path)) {
                    while (i < bufferEnd) {
                        outputStream.write(buffer[i] >> 24);
                        outputStream.write(buffer[i] >> 16);
                        outputStream.write(buffer[i] >> 8);
                        outputStream.write(buffer[i]);

                        lastByteOfStagingRead += 4;
                        if (lastByteOfStagingRead >= ROWS_LIMITED / 2) {
                            this.currentStagingIndex++;
                            this.lastByteOfStagingRead = 0;

                            if (this.currentStagingIndex < this.stagingLength) {
                                continue loopStaging;
                            } else {
                                eof = true;
                                bufferEnd = i;
                                break loopStaging;
                            }
                        }
                    }

                    break;
                }
            }

            currentBufferPos = this.bufferStart;
        }
    }

    private void flushOutput(int stagingNumber) throws IOException {
        var buffer = this.buffers;
        var bufferPos = 0;

        try (var bufferedWriter = Files.newBufferedWriter(Paths.get(OUTPUT_FILE))) {
            for (int i = 0; i < stagingNumber; i++) {
                var path = getStagingPath(1, i);
                try (var inputStream = Files.newInputStream(path)) {
                    while (bufferPos < buffer.length) {
                        int number = inputStream.read() << 24
                                | inputStream.read() << 16
                                | inputStream.read() << 8
                                | inputStream.read();
                        if (number == -1) {
                            break;
                        }
                        buffer[bufferPos++] = number;
                    }

                    if (bufferPos >= buffer.length) {
                        for (int value : buffer) {
                            bufferedWriter.write(value);
                            bufferedWriter.newLine();
                        }
                        bufferedWriter.flush();
                        bufferPos = 0;
                    }
                }
            }
        }
    }
}
