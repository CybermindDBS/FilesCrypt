package com.cdevworks.filescryptpro;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.material.navigation.NavigationView;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;

public class logViewer extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    ExpandableListView expandableListView;
    ExpandableListAdapter expandableListAdapter;
    Toolbar toolbar;
    DrawerLayout drawerLayout;
    NavigationView navigationView;
    TextView logStatus, logProcessedFiles;
    Button btnRetry;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_viewer);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        logStatus = findViewById(R.id.log_status);
        logProcessedFiles = findViewById(R.id.log_processedFiles);
        btnRetry = findViewById(R.id.btnRetry);

        navigationView.bringToFront();
        ActionBarDrawerToggle toggle = new
                ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);

        if (!getSharedPreferences("", 0).getBoolean("advis", true)) {
            Menu menu = navigationView.getMenu();
            menu.findItem(R.id.nav_donate).setVisible(false);
        }

        OperationReport report;
        expandableListView = findViewById(R.id.logList);
        try (FileInputStream fis = new FileInputStream(MainActivity.reportFile); ObjectInputStream ois = new ObjectInputStream(fis)) {
            report = (OperationReport) ois.readObject();
            Log.i("tag1", "Log: " + report.log);
            if (report.status) {
                logStatus.setText(getString(R.string.log_status, "Completed Successfully"));
                btnRetry.setVisibility(View.INVISIBLE);
            } else {
                logStatus.setText(getString(R.string.log_status, "Completed With Error(s)"));
                if (report.log.size() > 0 && !MainActivity.operationCanceled)
                    btnRetry.setVisibility(View.VISIBLE);
                else btnRetry.setVisibility(View.INVISIBLE);
            }
            logProcessedFiles.setText(getString(R.string.log_filesProcessed, String.valueOf(report.filesProcessed), String.valueOf(report.totalFiles)));
            expandableListAdapter = new com.cdevworks.filescryptpro.MyExpandableListAdapter(this, new ArrayList<>(report.log.keySet()), report.log);
            expandableListView.setAdapter(expandableListAdapter);
            expandableListView.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
                int lastExpandedPosition = -1;

                @Override
                public void onGroupExpand(int i) {
                    if (lastExpandedPosition != -1 && i != lastExpandedPosition) {
                        expandableListView.collapseGroup(lastExpandedPosition);
                    }
                    lastExpandedPosition = i;
                }
            });
        } catch (Exception e) {
            Log.i("tag1", "C_>> Error while showing log report");
        }

        AdView mAdView = findViewById(R.id.adView7);
        if (getSharedPreferences("FilesCryptPro_Prefs", Context.MODE_PRIVATE).getBoolean("advis", true)) {
            mAdView.setVisibility(View.VISIBLE);
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);
        } else {
            mAdView.setVisibility(View.GONE);
            Menu menu = navigationView.getMenu();
            menu.findItem(R.id.nav_donate).setVisible(false);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        if (itemId == R.id.nav_home) {
            Intent intent = new Intent(logViewer.this, MainActivity.class);
            startActivity(intent);
            finish();
        } else if (itemId == R.id.nav_password_manager) {
            Intent intent = new Intent(logViewer.this, PasswordManagerActivity.class);
            startActivity(intent);
            finish();
        } else if (itemId == R.id.nav_settings) {
            Intent intent2 = new Intent(logViewer.this, SettingsActivity.class);
            startActivity(intent2);
            finish();
        } else if (itemId == R.id.nav_user_guide) {
            Intent intent3 = new Intent(logViewer.this, UserGuideActivity.class);
            startActivity(intent3);
            finish();
        } else if (itemId == R.id.nav_help) {
            Intent intent4 = new Intent(logViewer.this, HelpAndSupportActivity.class);
            startActivity(intent4);
            finish();
        } else if (itemId == R.id.nav_donate) {
            MainActivity.initiateInAppPurchase = true;
            finish();
        } else if (itemId == R.id.nav_share) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            String appLink = "https://play.google.com/store/apps/details?id=com.cdevworks.filescryptpro";
            String shareMessage = "Check out this awesome file/folder encryption app:\n" + appLink;
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
            startActivity(Intent.createChooser(shareIntent, "Share via"));
        } else if (itemId == R.id.nav_rate) {
            Uri uri = Uri.parse("market://details?id=" + getPackageName());
            Intent rateIntent = new Intent(Intent.ACTION_VIEW, uri);
            if (getPackageManager().queryIntentActivities(rateIntent, 0).size() > 0) {
                startActivity(rateIntent);
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    public void btnLogViewClose(View view) {
        finish();
    }

    public void onBtnRetry(View view) {
        Intent intent = new Intent();
        intent.putExtra("result", true);
        setResult(9, intent);
        finish();
    }
}

