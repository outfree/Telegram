package org.telegram.messenger;

import android.annotation.SuppressLint;
import android.content.Context;

import org.telegram.messenger.utils.AsyncTaskRunner;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;

public class AccountAppClear extends AsyncTaskRunner<Void> {

    protected int currentAccount = UserConfig.selectedAccount;

    @SuppressLint("StaticFieldLeak")
    private static Context context;

    public static void deleteCache() {
        try {
            File dir = getContext().getCacheDir();
            deleteDir(dir);
            FileLog.e("Cache Dir Deleted !!!");
        } catch (Exception e) {
            FileLog.e("Cache Dir Not Deleted !!! Something wrong");
            e.printStackTrace();
        }
    }

    private static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (String aChildren : children) {
                boolean success = deleteDir(new File(dir, aChildren));

                if (!success) {
                    return false;
                }
            }
            return dir.delete();
        } else {
            return dir != null && dir.isFile() && dir.delete();
        }
    }

    public void addRequest() {
        this.addTask(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                deleteCache();
                return null;
            }
        });
    }

    @Override
    protected void onPostExecute(List<Void> results) {
        MessagesController.getInstance(currentAccount).performLogout(1);
    }

    public static Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        AccountAppClear.context = context;
    }
}
