package org.telegram.messenger.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public abstract class AsyncTaskRunner<T> {

    private ExecutorService executorService = null;
    private final Set<Callable<T>> tasks = new HashSet<>();

    public AsyncTaskRunner() {
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public AsyncTaskRunner(int threadNum) {
        this.executorService = Executors.newFixedThreadPool(threadNum);
    }


    public void addTask(Callable<T> task) {
        tasks.add(task);
    }

    public void execute() {
        try {
            List<Future<T>> features = executorService.invokeAll(tasks);

            List<T> results = new ArrayList<>();
            for (Future<T> feature : features) {
                results.add(feature.get());
            }
            this.onPostExecute(results);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            this.onCancelled();
        } finally {
            executorService.shutdown();
        }

    }

    protected abstract void onPostExecute(List<T> results);

    protected void onCancelled() {
        // stub
    }
}