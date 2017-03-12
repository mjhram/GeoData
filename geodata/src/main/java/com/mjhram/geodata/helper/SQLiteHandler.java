package com.mjhram.geodata.helper;

/**
* Author: Ravi Tamada
* URL: www.androidhive.info
* twitter: http://twitter.com/ravitamada
* */

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.HashMap;

public class SQLiteHandler extends SQLiteOpenHelper {
    private static final String TAG = SQLiteHandler.class.getSimpleName();
    // All Static variables
    // Database Version
    private static final int DATABASE_VERSION = 2;
    // Database Name
    private static final String DATABASE_NAME = "geodb.db";
    // Login table name
    private static final String TABLE_GEODATA = "geodata";
    // Login Table Columns names
    private static final String KEY_ID = "id";
    private static final String KEY_TIME = "time";
    private static final String KEY_UID = "userid";
    private static final String KEY_LAT = "lat";
    private static final String KEY_LONG = "long";
    private static final String KEY_SPEED = "speed";
    private static final String KEY_BEARING = "bearing";
    private static final String KEY_ACC = "accuracy";
    private static final String KEY_FIXTIME = "fixtime";
    private static final String KEY_INFO = "hasinfo";
    private static final String KEY_RMV = "shouldRmv";
    private static final String KEY_CALC_SPEED = "calcSpeed";
    private static final String KEY_CALC_BEARING = "calcBearing";

    public SQLiteHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    // Creating Tables
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_LOGIN_TABLE = "CREATE TABLE " + TABLE_GEODATA + "("
            + KEY_ID + " INTEGER PRIMARY KEY," + KEY_TIME + " TEXT,"
            + KEY_UID + " INTEGER ," + KEY_LAT + " TEXT," + KEY_LONG + " TEXT,"
            + KEY_SPEED + " REAL ," + KEY_BEARING + " REAL," + KEY_ACC + " REAL,"
            + KEY_FIXTIME + " INTEGER , " + KEY_INFO + " TEXT , "
            + KEY_RMV + " INTEGER , "
            + KEY_CALC_SPEED + " REAL , " + KEY_CALC_BEARING + " REAL" + ")";
        db.execSQL(CREATE_LOGIN_TABLE);

        Log.d(TAG, "Database tables created");
    }
    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_GEODATA);

        // Create tables again
        onCreate(db);
    }
    /**
     * Storing user details in database
     * */
    public void addData(long idx, String time, int uid, String lat, String lng,
                        double speed, double bearing, double acc, long fixtime, String info) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_ID, idx);
        values.put(KEY_TIME, time);
        values.put(KEY_UID, uid);
        values.put(KEY_LAT, lat);
        values.put(KEY_LONG, lng);
        values.put(KEY_SPEED, speed);
        values.put(KEY_BEARING, bearing);
        values.put(KEY_ACC, acc);
        values.put(KEY_FIXTIME, fixtime);
        values.put(KEY_INFO, info);

        // Inserting Row
        long id = db.insert(TABLE_GEODATA, null, values);
        db.close(); // Closing database connection

        //Log.d(TAG, "New user inserted into sqlite: " + id);
    }

    public void calc() {
        String selectQuery = "SELECT * FROM " + TABLE_GEODATA;
            selectQuery+=" ORDER BY "+SQLiteHandler.KEY_ID+","+SQLiteHandler.KEY_FIXTIME;
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.query(TABLE_GEODATA, null, null, null, null, null, SQLiteHandler.KEY_UID+","+SQLiteHandler.KEY_FIXTIME, null);

        //Cursor cursor = db.rawQuery(selectQuery, null);
        cursor.moveToFirst();
        int idx;
        idx = cursor.getColumnIndex(SQLiteHandler.KEY_LAT);
        double prevLat = cursor.getDouble(idx);
        idx = cursor.getColumnIndex(SQLiteHandler.KEY_LONG);
        double prevLng = cursor.getDouble(idx);
        idx = cursor.getColumnIndex(SQLiteHandler.KEY_FIXTIME);
        long prevFix = cursor.getLong(idx);

        float[] res = new float[2];
        ContentValues cv = new ContentValues();
        while(cursor.moveToNext()) {
            idx = cursor.getColumnIndex(SQLiteHandler.KEY_ID);
            long id = cursor.getLong(idx);
            idx = cursor.getColumnIndex(SQLiteHandler.KEY_LAT);
            double lat = cursor.getDouble(idx);
            idx = cursor.getColumnIndex(SQLiteHandler.KEY_LONG);
            double lng = cursor.getDouble(idx);
            idx = cursor.getColumnIndex(SQLiteHandler.KEY_FIXTIME);
            long fix2 = cursor.getLong(idx);
            idx = cursor.getColumnIndex(SQLiteHandler.KEY_INFO);
            String hasInfo = cursor.getString(idx);
            Location.distanceBetween(prevLat, prevLng, lat, lng, res);
            if(res[0] > 250) {
                prevLat = lat;
                prevLng = lng;
                prevFix = fix2;
                continue;
            }
            cv.clear();
            boolean shouldUpdate = false;
            if(hasInfo.charAt(2)=='0') {//no bearing
                cv.put(SQLiteHandler.KEY_CALC_BEARING, res[1]);
                shouldUpdate = true;
            }
            if(hasInfo.charAt(3)=='0' && fix2 != prevFix) {//no speed
                //calc speed:
                double spd = res[0] / ((fix2 - prevFix)/1000.0) ;
                cv.put(SQLiteHandler.KEY_CALC_SPEED, spd);
                shouldUpdate = true;
                if(spd <0) {
                    shouldUpdate = true;
                }
            }
            if(shouldUpdate){
                int a= db.update(TABLE_GEODATA, cv, SQLiteHandler.KEY_ID + "=" + id, null);
                Log.d(TAG, "data updated: #" + a);
            }
            prevLat = lat;
            prevLng = lng;
            prevFix = fix2;
        }
        cursor.close();
        db.close();
    }
    /**
    * Getting user data from database
    * */
    public HashMap<String, String> getUserDetails() {
        HashMap<String, String> user = new HashMap<String, String>();
        String selectQuery = "SELECT * FROM " + TABLE_GEODATA;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        // Move to first row
        cursor.moveToFirst();
        if (cursor.getCount() > 0) {
            user.put("name", cursor.getString(1));
            user.put("email", cursor.getString(2));
            user.put("uid", cursor.getString(3));
            user.put("created_at", cursor.getString(4));
        }
        cursor.close();
        db.close();
        // return user
        Log.d(TAG, "Fetching user from Sqlite: " + user.toString());

        return user;
    }
    /**
    * Getting user login status return true if rows are there in table
    * */
    public int getRowCount() {
        String countQuery = "SELECT * FROM " + TABLE_GEODATA;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        int rowCount = cursor.getCount();
        db.close();
        cursor.close();

        // return row count
        return rowCount;
    }
    /**
    * Re crate database Delete all tables and create them again
    * */
        public void deleteUsers() {
        SQLiteDatabase db = this.getWritableDatabase();
        // Delete All Rows
        db.delete(TABLE_GEODATA, null, null);
        db.close();

        Log.d(TAG, "Deleted all user info from sqlite");
    }

    public static void exportDB(Context ac) {
        File sd = Environment.getExternalStorageDirectory();
        File data = Environment.getDataDirectory();
        FileChannel source = null;
        FileChannel destination = null;
        String tmp = ac.getPackageName();
        String currentDBPath = "/data/" + tmp + "/databases/" + SQLiteHandler.DATABASE_NAME;
        String backupDBPath = SQLiteHandler.DATABASE_NAME;
        File currentDB = new File(data, currentDBPath);
        File backupDB = new File(sd, backupDBPath);
        try {
            source = new FileInputStream(currentDB).getChannel();
            destination = new FileOutputStream(backupDB).getChannel();
            destination.transferFrom(source, 0, source.size());
            source.close();
            destination.close();
            Toast.makeText(ac, "DB Exported!" + backupDB.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void importDB(Context cx){
        File sd = Environment.getExternalStorageDirectory();
        File data = Environment.getDataDirectory();
        FileChannel source=null;
        FileChannel destination=null;
        String tmp = cx.getPackageName();
        String currentDBPath = "/data/"+ tmp +"/databases/"+SQLiteHandler.DATABASE_NAME;
        String backupDBPath = SQLiteHandler.DATABASE_NAME;
        File currentDB = new File(data, currentDBPath);
        File backupDB = new File(sd, backupDBPath);
        try {
            source = new FileInputStream(backupDB).getChannel();
            destination = new FileOutputStream(currentDB).getChannel();
            long s = source.size();
            destination.transferFrom(source, 0, s);
            source.close();
            destination.close();
            Toast.makeText(cx, "DB Imported!", Toast.LENGTH_LONG).show();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
}
