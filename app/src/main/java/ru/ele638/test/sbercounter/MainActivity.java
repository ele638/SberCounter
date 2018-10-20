package ru.ele638.test.sbercounter;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private final String TAG = "SMSMainActivity";

    SQLHelper dbHelper;
    static RVAdapter rvAdapter;
    RecyclerView recyclerView;
    static ArrayList<Message> messages;
    static LoadTask loadTask;
    static ProgressDialog dialog;

    public static RVAdapter getRvAdapter() {
        return rvAdapter;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.mainRV);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        rvAdapter = new RVAdapter();
        messages = new ArrayList<>();

        dbHelper = new SQLHelper(this);

        rvAdapter.setMessages(messages);
        recyclerView.setAdapter(rvAdapter);
        dialog = new ProgressDialog(this);
        dialog.setMessage("Loading");
        dialog.setCancelable(false);
        dialog.setTitle("Loading");

        if (!hasReadSmsPermission()) {
            showRequestPermissionsInfoAlertDialog();
        }
        SMSHelper.getAllMessages(dbHelper, rvAdapter);

        Log.d(TAG, "Started activity");
    }

    private boolean hasReadSmsPermission() {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED;
    }


    private void requestReadAndSendSmsPermission() {
        String Recieve_permission = Manifest.permission.RECEIVE_SMS;
        String Read_permission = Manifest.permission.READ_SMS;
        int recieve_grant = ContextCompat.checkSelfPermission(this, Recieve_permission);
        int read_grant = ContextCompat.checkSelfPermission(this, Read_permission);
        if (recieve_grant != PackageManager.PERMISSION_GRANTED ||
                read_grant != PackageManager.PERMISSION_GRANTED) {
            String[] permission_list = new String[2];
            permission_list[0] = Recieve_permission;
            permission_list[1] = Read_permission;
            ActivityCompat.requestPermissions(this, permission_list, 1);
        }
    }

    private void showRequestPermissionsInfoAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Запрос прав для приложения");
        builder.setMessage("Данное приложение работает исключительно с СМС от номера 900, в связи с " +
                "чем необходим доступ к сообщениям. Без этого доступа приложение работать не будет!");
        builder.setPositiveButton("Понятно", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                requestReadAndSendSmsPermission();
            }
        });
        builder.show();
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
        if (id == R.id.action_resetdb) {
            SMSHelper.dropDatabase(dbHelper);
            reloadMessages();
        }
        if (id == R.id.action_readSMS) {
            loadTask = new LoadTask();
            loadTask.execute();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            SMSHelper.getAllMessages(dbHelper, rvAdapter);
        } else {
            finish();
        }
    }

    public void reloadMessages() {
        messages.clear();
        SMSHelper.getAllMessages(dbHelper, rvAdapter);
        rvAdapter.notifyDataSetChanged();
    }

    private class LoadTask extends AsyncTask<Void, Integer, Void> {

        Integer size, current;
        String[] projection = {"address", "body"};
        String selection = "address = \'900\'";

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Cursor cursor = getContentResolver().query(
                    Uri.parse("content://sms/inbox"), projection, selection, null, null);
            assert cursor != null;
            size = cursor.getCount();
            cursor.close();
            dialog.setMax(size);
            dialog.setProgress(0);
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog.show();
            SMSHelper.dropDatabase(dbHelper);
            RVAdapter.getMessages().clear();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Cursor cursor = getContentResolver().query(
                    Uri.parse("content://sms/inbox"), projection, selection, null, null);
            if (cursor.moveToFirst()) {
                current = 0;
                do {
                    SMSHelper.processIncomeSMS(dbHelper, cursor.getString(cursor.getColumnIndex("body")));
                    current++;
                    publishProgress(current);
                } while (cursor.moveToNext());
            }
            cursor.close();
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer[] values) {
            super.onProgressUpdate(values);
            dialog.setMessage("Loaded: " + values[0] + "/" + size);
            dialog.setProgress(values[0]);
        }

        @Override
        protected void onPostExecute(Void voids) {
            super.onPostExecute(voids);
            SMSHelper.getAllMessages(dbHelper, rvAdapter);
            dialog.dismiss();
        }
    }

}
