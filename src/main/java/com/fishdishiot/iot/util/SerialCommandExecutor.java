// src/main/java/com/fishdishiot/iot/util/SerialCommandExecutor.java
package com.fishdishiot.iot.util;

import org.springframework.stereotype.Component;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
public class SerialCommandExecutor {
    private final BlockingQueue<Runnable> commandQueue = new LinkedBlockingQueue<>();
    private final Thread workerThread;

    public SerialCommandExecutor() {
        workerThread = new Thread(() -> {
            while (true) {
                try {
                    Runnable task = commandQueue.take();
                    task.run();
                } catch (InterruptedException ignored) {}
            }
        }, "Serial-Command-Executor");
        workerThread.setDaemon(true);
        workerThread.start();
    }

    public void submit(Runnable task) {
        commandQueue.offer(task);
    }
}