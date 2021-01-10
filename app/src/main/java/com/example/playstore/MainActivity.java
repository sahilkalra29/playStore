package com.example.playstore;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    ListView listview;
    List<MainData> dataList = new ArrayList<>();
    public int newVersionCode;
    public String newVersionName = "";
    public String ApkName;
    public String ReleaseServer;
    public String BuildVersionPath = "";
    public String urlpath;
    //public String PackageName;
    public int errorCode=0;

    // Progress Dialog
    private ProgressDialog pDialog;

    // Progress dialog type (0 - for Horizontal progress bar)
    public static final int progress_bar_type = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setIcon(getDrawable(R.drawable.cdot));
        actionBar.setTitle((Html.fromHtml("<font color=\"#000080\">" + "\t\tPlayStore" + "</font>")));
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#FFFFFF")));

        listview = findViewById(R.id.list_view);
        ReleaseServer = "http://192.168.4.244/";

        //Initialize Package Manager
        PackageManager manager = getPackageManager();
        //Initialize Installed Apps List
        //List<ApplicationInfo> infoList = manager.getInstalledPackages(PackageManager.GET_META_DATA);
        List<PackageInfo> infoList = manager.getInstalledPackages(PackageManager.GET_META_DATA);

        for(PackageInfo info: infoList){

            //Check only for installed apps and not system apps
            //if((info.flags & ApplicationInfo.FLAG_SYSTEM)) == 0){
            //if ((!getSysPackages) && (info.versionName == null)) {
            if(manager.getLaunchIntentForPackage(info.packageName) == null){
                continue;
            }

            MainData data = new MainData();
            data.setName(info.applicationInfo.loadLabel(getPackageManager()).toString());
            data.setPackageName(info.packageName);
            data.setLogo(info.applicationInfo.loadIcon(manager));
            data.setCurrentVersionCode(info.versionCode);
            data.setCurrentVersionName(info.versionName);

            dataList.add(data);
        }

        //Set Adapter
        listview.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return dataList.size();
            }

            @Override
            public Object getItem(int position) {
                return position;
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {

                //Initialize View
                View view = getLayoutInflater().inflate(R.layout.list_row_item,null);

                //Initialize main data
                MainData data = dataList.get(position);

                //Initialize  and assign variable
                ImageView ivAppLogo = view.findViewById(R.id.iv_app_logo);
                TextView tvAppName = view.findViewById(R.id.tv_app_name);
                TextView curreVersionDisplay = view.findViewById(R.id.curr_ver_display);
                Button checkUpdate = view.findViewById(R.id.btn_check_update);

                //Set Logo on ImageView
                ivAppLogo.setImageDrawable(data.getLogo());
                tvAppName.setText(data.getName());
                curreVersionDisplay.setText("Current Version : " + data.getCurrentVersionCode());
                checkUpdate.setOnClickListener(new View.OnClickListener(){

                    @Override
                    public void onClick(View v) {

                        MainData data = dataList.get(position);

                        Log.i("info","Hello the App selected is :: " + data.getName());
                        Log.i("info", "\nApp current version Code is  :: " + data.getCurrentVersionCode());

                        BuildVersionPath = ReleaseServer + data.getName() + "_version.txt";
                        urlpath = ReleaseServer + data.getName() + ".apk";
                        ApkName = data.getName() + ".apk";

                        Log.i("info","\nHello BuildVersionPath is :: " + BuildVersionPath);
                        Log.i("info","\nHello URL PATH is :: " + urlpath);

                        checkForUpdate(data);
                    }
                });
                return view;
            }
        });
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case progress_bar_type:
                pDialog = new ProgressDialog(this);
                pDialog.setMessage("Downloading file. Please wait...");
                pDialog.setIndeterminate(false);
                pDialog.setMax(100);
                pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                pDialog.setCancelable(true);
                pDialog.show();
                return pDialog;
            default:
                return null;
        }
    }

    private void checkForUpdate(MainData data){

        Log.i("info", " \n Inside check for Update, the build version path is  :: " + BuildVersionPath);
        Log.i("info", "\nInside checkFor Update App name is :: " + data.getName());

        /* 1. Download Version.text File from Server */
        new DownloadFileFromURL().execute(data);

        /* 2. Provide Dialog to Ask user permission to install */
        //compareVersionCode(data);
    }

    private void compareVersionCode(MainData data){

        Log.i("info", "Inside compare Version Code" + data.getName());
        Log.i("info", "Inside compare Version Code" + newVersionCode);

        if (newVersionCode <= data.getCurrentVersionCode()) {
            Toast.makeText(getApplicationContext(),"App is up to date !!", Toast.LENGTH_SHORT).show();
        }

        if (newVersionCode > data.getCurrentVersionCode()) {

            Log.i("info", " \n Inside onClick Dialog Interface - 00 new :: " + newVersionCode);
            Log.i("info", " \n Inside onClick Dialog Interface - 00 old :: " + data.getCurrentVersionCode());

            DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {

                Log.i("info", " \n Inside onClick Dialog Interface - 1 ");
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:

                        Log.i("debug", " Inside onClick Dialog Interface -2 Button Positive ");
                        new DownloadFileForSdCard().execute(data);

                        Log.i("debug", " Inside onClick Dialog Interface -3");
                        InstallApplication();
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        break;
                }
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("New Update is Available.").setPositiveButton("Install", dialogClickListener)
                    .setNegativeButton("Cancel.", dialogClickListener).show();
        }
    }

    public void InstallApplication() {

        Log.i("info","\n Entered in Installed Application !!");

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        //intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);


        Log.i("inof","\n 2222222222222222222 :: Entered in Installed Application");

        Uri apkURI = FileProvider.getUriForFile(getApplicationContext(), getPackageName() + ".provider", new File(Environment.getExternalStorageDirectory() + "/" + ApkName.toString()));
        Log.i("inof","\n 333333333333333333333 :: Entered in Installed Application");

        intent.setDataAndType(apkURI, "application/vnd.android.package-archive");
        Log.i("inof","\n 44444444444444444444444444 :: Entered in Installed Application" + Environment.getExternalStorageDirectory() + ApkName.toString());
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        Log.i("inof","\n 5555555555555555555555555555555555 :: Entered in Installed Application");

        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(intent);

        Log.i("inof","\n 6666666666666666666666666666666 :: Entered in Installed Application");
    }



    class DownloadFileFromURL extends AsyncTask<MainData, String, MainData> {

        //public void GetVersionFromServer(String BuildVersionPath) {
        @Override
        protected MainData doInBackground(MainData ...data) {

            MainData datay = data[0];
            Log.i("info", " PRint the dataaaaaaaaaa ::: " + datay.getName());
            /*  This is the file which contains version number to be downloaded from the remote server
                Path ="http://<RELEASE-SERVER-IP>/<apk_name_version.txt>";
                Name of the local file created : <apk_name_version.txt>
                <apk_name_version.txt> contains Version Code : < >; \n Version Name = <>
             */

            Log.i("DEBUG", " !!!!!!!!!!!!!!!!!!!!! Entered in Get Version form Server !!!!!!!!!!!!!!!!");

            URL u;
            try {
                u = new URL(BuildVersionPath);

                Log.i("info", "URL is u::" + u);

                HttpURLConnection c = (HttpURLConnection) u.openConnection();
                c.setRequestMethod("GET");
                c.setDoOutput(true);
                c.setConnectTimeout(2000);
                c.connect();

                InputStream in = c.getInputStream();

                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                byte[] buffer = new byte[1024]; //that stops the reading after 1024 chars..
                //in.read(buffer); //  Read from Buffer.
                //baos.write(buffer); // Write Into Buffer.

                int len1 = 0;
                while ((len1 = in.read(buffer)) != -1) {
                    baos.write(buffer, 0, len1); // Write Into ByteArrayOutputStream Buffer.
                }

                String temp = "";
                String s = baos.toString();// baos.toString(); contain Version Code = 2; \n Version name = 2.1;

                for (int i = 0; i < s.length(); i++) {
                    i = s.indexOf("=") + 1;
                    while (s.charAt(i) == ' ') // Skip Spaces
                    {
                        i++; // Move to Next.
                    }
                    while (s.charAt(i) != ';' && (s.charAt(i) >= '0' && s.charAt(i) <= '9' || s.charAt(i) == '.')) {
                        temp = temp.toString().concat(Character.toString(s.charAt(i)));
                        i++;
                    }
                    //
                    s = s.substring(i); // Move to Next to Process.!
                    temp = temp + " "; // Separate w.r.t Space Version Code and Version Name.
                }
                String[] fields = temp.split(" ");// Make Array for Version Code and Version Name.

                newVersionCode = Integer.parseInt(fields[0].toString());// .ToString() Return String Value.
                newVersionName = fields[1].toString();

                Log.i("info", "\nInside Download From URL :: " + newVersionName.toString());
                Log.i("info", "\n Inside Download From URL :: " + String.valueOf(newVersionCode));

                baos.close();
            } catch (MalformedURLException e) {
                //Toast.makeText(getApplicationContext(), "Error." + e.getMessage(), Toast.LENGTH_SHORT).show();
                errorCode=1;
                //Log.i("info", " 11111111111111111111111111111111111111111111111111111111111111111111111");
                //e.printStackTrace();
            } catch (IOException e) {
                //Log.i("info", " 22222222222222222222222222222222222222222222222222222222222222222222222");
                errorCode=2;
                //e.printStackTrace();
                //Toast.makeText(getApplicationContext(), "Error." + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
            return datay;
        }// doInBackground End.

        protected void onPostExecute(MainData data) {

            if(errorCode == 1){
                Toast.makeText(getApplicationContext(), "Error = " + errorCode, Toast.LENGTH_SHORT).show();
                return;
            }
            else if(errorCode == 2){
                Toast.makeText(getApplicationContext(), "Error : Release Server Not Available !! ", Toast.LENGTH_SHORT).show();
                return;
            }
            /*if (newVersionCode <= data.getCurrentVersionCode()) {
                Toast.makeText(getApplicationContext(),"App is up to date !!", Toast.LENGTH_SHORT).show();
            }*/
            compareVersionCode(data);
        }
    } // Class DownloadFromURL End


    class DownloadFileForSdCard extends AsyncTask<MainData, String, MainData> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // take CPU lock to prevent CPU from going off if the user
            // presses the power button during download
            /*PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    getClass().getName());
            mWakeLock.acquire();*/
            showDialog(progress_bar_type);
        }

        /**
         * Updating progress bar
         * */
        protected void onProgressUpdate(String... progress) {
            // setting progress percentage
            pDialog.setProgress(Integer.parseInt(progress[0]));
        }

        @Override
        protected MainData doInBackground(MainData ...data) {

            //public void DownloadOnSDcard()
            try {
                Log.i("info", "I am in Download on SDCARD and url PAth is  :: " + urlpath);
                URL url = new URL(urlpath); // Your given URL.

                HttpURLConnection c = (HttpURLConnection) url.openConnection();
                c.setRequestMethod("GET");
                c.setDoOutput(true);
                c.connect(); // Connection Complete here.!

                //Toast.makeText(getApplicationContext(), "HttpURLConnection complete.", Toast.LENGTH_SHORT).show();

                String PATH = Environment.getExternalStorageDirectory().toString();


                Log.i("info", " HEllo -->>>>>>>>> My directory PATH is" + PATH);
                File file = new File(PATH); // PATH = /mnt/sdcard/download/
                if (!file.exists()) {
                    Log.i("info", " HEllo in if check-->>>>>>>>> My directory PATH is" + PATH);
                    file.mkdirs();
                }


                Log.i("info", " HEllo past if check-->>>>>>>>>");

                File outputFile = new File(file, ApkName.toString());
                FileOutputStream fos = new FileOutputStream(outputFile);

                //      Toast.makeText(getApplicationContext(), "SD Card Path: " + outputFile.toString(), Toast.LENGTH_SHORT).show();

                Log.i("info", " HEllo before input streammmmmmmmmm -->>>>>>>>> My directory PATH is" + PATH);
                InputStream is = c.getInputStream(); // Get from Server and Catch In Input Stream Object.

                byte[] buffer = new byte[1024];
                int len1 = 0;

                Log.i("info", " HEllo starting while loop-->>>>>>>>> My directory PATH is" + PATH);

                while ((len1 = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, len1); // Write In FileOutputStream.
                }
                fos.close();
                is.close();//till here, it works fine - .apk is download to my sdcard in download file.

                //download the APK to sdcard then fire the Intent.
            } catch (IOException e) {
                errorCode=3;
                Log.i("debug", " EXCEPTION ::: " + e.getMessage());
            }
            Log.i("info", "I am out of  Download on SDCARD");
            return null;
        }


        protected void onPostExecute(MainData data) {
            // dismiss the dialog after the file was downloaded
            Log.i("info","Entered hereeeee on post execute");
            dismissDialog(progress_bar_type);
            if(errorCode == 3){
                Toast.makeText(getApplicationContext(), "Error :: Download Failed", Toast.LENGTH_SHORT).show();
            }
        }
    }
}