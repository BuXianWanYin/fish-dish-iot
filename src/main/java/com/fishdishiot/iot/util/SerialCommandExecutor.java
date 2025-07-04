// src/main/java/com/fishdishiot/iot/util/SerialCommandExecutor.java
package com.fishdishiot.iot.util;

import org.springframework.stereotype.Component;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

@Component
public class SerialCommandExecutor {
    private final BlockingQueue<Runnable> commandQueue = new LinkedBlockingQueue<>();
    private final Thread workerThread;

    private static final long MIN_INTERVAL_MS = 500; // 500ms安全间隔
    private long lastTaskTime = 0;

    public SerialCommandExecutor() {
        workerThread = new Thread(() -> {
            while (true) {
                try {
                    Runnable task = commandQueue.take();
                    long now = System.currentTimeMillis();
                    long wait = lastTaskTime + MIN_INTERVAL_MS - now;
                    if (wait > 0) {
                        Thread.sleep(wait);
                    }
                    lastTaskTime = System.currentTimeMillis();
                    try {
                        System.out.println("[SerialCommandExecutor] 执行任务: " + task + ", 线程: " + Thread.currentThread().getName() + ", 时间: " + java.time.LocalDateTime.now());
                        task.run();
                        System.out.println("[SerialCommandExecutor] 任务完成: " + task + ", 线程: " + Thread.currentThread().getName() + ", 时间: " + java.time.LocalDateTime.now());
                    } catch (Throwable t) {
                        t.printStackTrace();
                        System.err.println("[SerialCommandExecutor] 任务异常: " + t.getMessage());
                    }
                } catch (InterruptedException ignored) {}
            }
        }, "Serial-Command-Executor");
        workerThread.setDaemon(true);
        workerThread.start();
    }

    public void submit(Runnable task) {
        commandQueue.offer(task);
    }

    public <T> Future<T> submit(Callable<T> task) {
        FutureTask<T> futureTask = new FutureTask<>(task);
        commandQueue.offer(futureTask);
        return futureTask;
    }
}