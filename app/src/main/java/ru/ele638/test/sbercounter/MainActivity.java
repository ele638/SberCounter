package ru.ele638.test.sbercounter;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
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

    static SMSService service;
    static RVAdapter rvAdapter;
    RecyclerView recyclerView;
    static ArrayList<Message> messages;
    static LoadTask loadTask;
    static ProgressDialog dialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.mainRV);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        rvAdapter = new RVAdapter();
        if (messages == null)
            messages = new ArrayList<>();

        loadTask = new LoadTask();

        dialog = new ProgressDialog(this);
        dialog.setMessage("Loading");
        dialog.setCancelable(false);
        dialog.setTitle("Loading");

        if (!hasReadSmsPermission()) {
            showRequestPermissionsInfoAlertDialog();
        } else if (SMSService.isServiceDown()) {
            startService(new Intent(this, SMSService.class));
        } else {
            service = SMSService.getInstance();
        }

        if (service != null)
            messages = service.getAllMessages();
        rvAdapter.setMessages(messages);
        if (messages.size() == 0)
            Log.d(TAG, "Empty messages");
        recyclerView.setAdapter(rvAdapter);
        Log.d(TAG, "Started activity");

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
        if (id == R.id.action_resetdb) {
            service.dropDatabase();
            reloadMessages();
        }
        if (id == R.id.action_readSMS) {
            loadTask.execute();
        }
        return super.onOptionsItemSelected(item);
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
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (SMSService.isServiceDown()) {
                startService(new Intent(this, SMSService.class));
                service = SMSService.getInstance();
            }
        } else {
            finish();
        }
    }

    public void reloadMessages() {
        messages.clear();
        messages = SMSService.getInstance().getAllMessages();
        rvAdapter.notifyDataSetChanged();
    }

    private class LoadTask extends AsyncTask {

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
            dialog.show();
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            Cursor cursor = getContentResolver().query(
                    Uri.parse("content://sms/inbox"), projection, selection, null, null);
            if (cursor.moveToFirst()) {
                current = 0;
                do {
                    service.processIncomeSMS(cursor.getString(cursor.getColumnIndex("body")));
                    current++;
                    dialog.setProgress(current);
                } while (cursor.moveToNext());
            }
            cursor.close();
            return null;
        }


        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
            messages = service.getAllMessages();
            dialog.dismiss();
        }
    }
}
