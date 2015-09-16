package gq.baijie.androidracecondition;

import android.os.Bundle;
import android.support.annotation.MainThread;
import android.support.annotation.WorkerThread;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.Spanned;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.lang.ref.WeakReference;

import butterknife.Bind;
import butterknife.ButterKnife;
import gq.baijie.androidracecondition.utils.Engine;

public class MainActivity extends AppCompatActivity {

    private final Engine mEngine = new Engine();

    @Bind(R.id.main_text)
    TextView mMainTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        for (int counter = 1; counter <= 100; counter++) {
            mEngine.execute(new DummyTask(mMainTextView, counter));
        }

        new Thread() {
            @Override
            public void run() {
                try {
                    sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mMainTextView.post(new Runnable() {
                    @Override
                    public void run() {
                        mEngine.closeTap();
                        mMainTextView.append(stopMessage());
                        mMainTextView.append(" " + System.nanoTime() + "\n");
                    }

                    private Spanned stopMessage() {
                        return Html.fromHtml("<font color=#cc0029>**Tap is closed**</font>");
                    }
                });
            }
        }.start();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private static class DummyTask implements Engine.Task<String> {

        private static final String TIME_FORMAT = "%1$tk:%1$tM:%1$tS %1$tL %1$tN";

        final WeakReference<TextView> mTextViewWeakReference;

        final int mTaskNum;

        private DummyTask(TextView mainTextView, int taskNum) {
            mTextViewWeakReference = new WeakReference<>(mainTextView);
            mTaskNum = taskNum;
        }

        @WorkerThread
        @Override
        public String process() {
            try {
                Thread.sleep(5000 + (int) (Math.random() * 500 - 250));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return mTaskNum + " Nanosecond: " + String.valueOf(System.nanoTime());
//            return String.format(TIME_FORMAT, new Date());
//            return String.format(TIME_FORMAT, LocalTime.now());
        }

        @MainThread
        @Override
        public void onSuccess(String result) {
            tryToLogMessage(result);
        }

        @MainThread
        @Override
        public void onFailure(Exception e) {
            tryToLogMessage("on Failure: " + e);
        }

        @MainThread
        private void tryToLogMessage(String message) {
            final TextView mainTextView = mTextViewWeakReference.get();
            if (mainTextView != null) {
                mainTextView.append(message + "\n");
            }
        }
    }

}
