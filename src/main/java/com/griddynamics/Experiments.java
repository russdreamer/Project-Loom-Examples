package com.griddynamics;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.stream.IntStream;

public class Experiments {
    private static final Object MUTEX = new Object();
    private static final Queue<Integer> queue = new ArrayDeque<>();

    public static void createPlatformThreads() {
        IntStream.range(0, 1000).forEach(i -> {
            new Thread(() -> {
                sleep(60_000);
            }).start();
        });
        sleep(60_000);
    }

    public static void createVirtualThreads() {
        IntStream.range(0, 1_000_000).forEach(i -> {
            Thread.startVirtualThread(() -> {
                sleep(5_000);
            });
        });
        sleep(60_000);
    }

    public static void findPrimaryNumber() {
        try (var ex = Executors.newVirtualThreadPerTaskExecutor()) {
            IntStream.range(0, 1000).forEach( i -> {
                Runnable runnable = () -> {
                    for (int n = 2; n <= 10000000; n++) {
                        boolean isPrime = true;
                        double squareRoot = Math.sqrt(n) + 2;
                        int r = 2;
                        while (r <= squareRoot && isPrime) {
                            if (n % r == 0) isPrime = false;
                            r++;
                        }
                        if (n % 5000000 == 0) {
                            System.out.println(i);
                        }
                    }
                };
                ex.submit(runnable);
            });
        }
    }
    public static void fastAndSlowTasks() throws InterruptedException {
        var threads = new ArrayList<Thread>();
        var cores = Runtime.getRuntime().availableProcessors(); // 8 cores

        // slow tasks
        IntStream.range(0, cores).forEach( i -> {
            threads.add(createSlowCPUTask());
        });

        // fast tasks
        IntStream.range(0, cores).forEach( i -> {
            threads.add(createFastCPUTask());
        });

        for (Thread t : threads) {
            t.join();
        }
    }

    public static void runSleepThreadPools() {
        runSleepThreadPoolsExample(Executors.newWorkStealingPool());
    }

    public static void runSleepVirtualPool() {
        runSleepThreadPoolsExample(Executors.newVirtualThreadPerTaskExecutor());
    }

    public static void blockingQueue() {
        runBlockingQueueExample(Executors.newFixedThreadPool(5));
    }

    public static void blockingQueueWithVirtual() {
        runBlockingQueueExample(Executors.newVirtualThreadPerTaskExecutor());
    }

    public static void syncronizedExample() {
        try (var exec = Executors.newVirtualThreadPerTaskExecutor()) {
            IntStream.range(0, 1_000_000).forEach(i -> exec.submit(() -> syncMethod(i)));
        }
    }

    public static void runCompletableFuture() throws ExecutionException, InterruptedException {
        ArrayBlockingQueue<String> queue = new ArrayBlockingQueue<>(1);
        int threadNum = 1000;

        CompletableFuture[] arr = new CompletableFuture[threadNum];
        IntStream.range(0, threadNum).forEach(i -> {
            Supplier s = () -> {
                try {
                    System.out.println(Thread.currentThread().getName() + " entered");
                    queue.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return null;
            };
            CompletableFuture future = CompletableFuture.supplyAsync(s);
            arr[i] = future;
        });
        CompletableFuture.allOf(arr).get();
    }

    private static void syncMethod(int num) {
        synchronized (MUTEX) {
            System.out.println(num + " started");
            sleep(3_000);
            System.out.println(num + " finished");
        }
    }

    private static void runBlockingQueueExample(ExecutorService executorService) {
        LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>(1);
        try (executorService) {
            IntStream.range(0, 3).forEach(k -> {
                sleep(5000);
                IntStream.range(0, 1000).forEach(i -> {
                    Runnable r = () -> {
                        var currentThread = Thread.currentThread();
                        System.out.println(currentThread.getName() + " entered. isDaemon = " + currentThread.isDaemon());
                        try {
                            String res = queue.take();
                            System.out.println("Got: " + res);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    };
                    executorService.submit(r);
                });
            });
        }
    }

    private static void runSleepThreadPoolsExample(ExecutorService executorService) {
        try (executorService) {
            IntStream.range(0, 20).forEach(i -> {
                Runnable r = () -> {
                    sleep(5_000);
                    System.out.println(Thread.currentThread().getName() + ": finished. Was virtual: " + Thread.currentThread().isVirtual());
                };
                executorService.submit(r);
            });
        }
    }

    private static Thread createSlowCPUTask() {
        return Thread.startVirtualThread(() -> {
            var bestUUID = "";
            for (var j = 0; j < 1_000_000; j++) {
                var currentUUID = UUID.randomUUID().toString();
                if (currentUUID.compareTo(bestUUID) > 0) {
                    bestUUID = currentUUID;
                }
            }
            System.out.println("Best slow UUID is " + bestUUID);
        });
    }

    private static Thread createFastCPUTask() {
        return Thread.startVirtualThread(() -> {
            var bestUUID = UUID.randomUUID().toString();
            System.out.println("Best fast UUID is " + bestUUID);
        });
    }

    public static void sleep(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
