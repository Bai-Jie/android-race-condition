package gq.baijie.androidracecondition.utils;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Engine {

    private static final int STATE_SUCCESS = 0;

    private static final int STATE_FAILURE = 1;

    private ExecutorService mExecutorService = Executors.newFixedThreadPool(110);

    private Handler mHandler = new TaskHandler<>(Looper.getMainLooper());

    private Tap mTap = new Tap();


    public static interface Task<T> {

        @WorkerThread
        public T process();

        @MainThread
        public void onSuccess(T result);

        @MainThread
        public void onFailure(Exception e);
    }

    @MainThread
    public <T> void execute(Task<T> task) {
        mExecutorService.execute(new RunnableTask<>(task, mHandler, mTap));
    }

    /**
     * Thread Safe
     */
    public void closeTap() {
        mTap.isClose = true;
    }

    private static class Tap {

        /** Thread Safe */
        volatile boolean isClose = false;
    }

    private static class RunnableTask<T> implements Runnable {

        private Task<T> mTask;

        private Handler mHandler;

        private Tap mTap;

        public RunnableTask(@NonNull Task<T> task, @NonNull Handler handler, @NonNull Tap tap) {
            mTask = task;
            mHandler = handler;
            mTap = tap;
        }

        @WorkerThread
        @Override
        public void run() {
            TaskWrapper<T> wrapper = new TaskWrapper<>();
            wrapper.task = mTask;
            try {
                wrapper.result = mTask.process();
                sendMessageIfTapIsOpen(Message.obtain(mHandler, STATE_SUCCESS, wrapper));
            } catch (Exception e) {
                wrapper.exception = e;
                sendMessageIfTapIsOpen(Message.obtain(mHandler, STATE_FAILURE, wrapper));
            }
        }

        @WorkerThread
        private void sendMessageIfTapIsOpen(Message message) {
            if (!mTap.isClose) {
                mHandler.sendMessage(message);
            } else {
                Log.d("ENGINE", "Didn't send message because tap is close.");
            }
        }

    }

    private static class TaskWrapper<T> {

        Task<T> task;

        T result;

        Exception exception;
    }

    private static class TaskHandler<T> extends Handler {

        public TaskHandler() {
        }

        public TaskHandler(Callback callback) {
            super(callback);
        }

        public TaskHandler(Looper looper) {
            super(looper);
        }

        public TaskHandler(Looper looper, Callback callback) {
            super(looper, callback);
        }

        @MainThread
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            TaskWrapper<T> wrapper = cast(msg.obj);
            if (msg.what == STATE_SUCCESS) {
                wrapper.task.onSuccess(wrapper.result);
            } else {
                wrapper.task.onFailure(wrapper.exception);
            }
        }

        @SuppressWarnings("unchecked")
        private <C> C cast(Object obj) {
            return (C) obj;
        }

    }

}
