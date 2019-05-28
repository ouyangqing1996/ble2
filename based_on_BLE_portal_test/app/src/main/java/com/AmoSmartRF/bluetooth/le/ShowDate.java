package com.AmoSmartRF.bluetooth.le;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.lin.bluetooth.le.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;

public class ShowDate extends Activity {

    private SQLiteDatabase db;
    private Cursor cursor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_date);
//        MyView2 myView2 = (MyView2) findViewById(R.id.myview2);
        ListView lv = (ListView) findViewById(R.id.lv_data_show);
        MyOpenHelper myhelper = new MyOpenHelper(this);
        db = myhelper.getReadableDatabase();
        cursor = db.query("data1", null, null, null, null, null, null);
//        myView2.beginThread(cursor);

        lv.setAdapter(new MyAdapter(cursor));
    }



    public class MyAdapter extends BaseAdapter {
        Cursor mcursor;

        public MyAdapter(Cursor cursor) {
            mcursor = cursor;

        }

        @Override
        public int getCount() {
            return mcursor.getCount();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v;
            TextView date;
            TextView volt;
            if (convertView == null) {
                v = View.inflate(ShowDate.this, R.layout.item, null);
            } else {
                v = convertView;
            }
            mcursor.moveToPosition(mcursor.getCount() - position - 1);
            date = (TextView) v.findViewById(R.id.date);
            volt = (TextView) v.findViewById(R.id.volt);
            date.setText(mcursor.getString(1));
            volt.setText(mcursor.getString(2));

            return v;
        }
    }
}
