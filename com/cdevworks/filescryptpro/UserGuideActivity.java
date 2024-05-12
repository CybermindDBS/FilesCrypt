package com.cdevworks.filescryptpro;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.material.navigation.NavigationView;

public class UserGuideActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    DrawerLayout drawerLayout;
    NavigationView navigationView;
    Toolbar toolbar;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_guide);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        navigationView.bringToFront();
        ActionBarDrawerToggle toggle = new
                ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);


        WebView webView = findViewById(R.id.webView1);
        webView.getSettings().setJavaScriptEnabled(true); // Enable JavaScript if needed
        webView.setWebViewClient(new WebViewClient()); // Optional: Handle navigation within WebView

        // Load local HTML file from assets folder
        webView.loadUrl("file:///android_asset/user_guide.html");

        AdView mAdView = findViewById(R.id.adView4);
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
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        if (itemId == R.id.nav_home) {
            Intent intent = new Intent(UserGuideActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        } else if (itemId == R.id.nav_password_manager) {
            Intent intent = new Intent(UserGuideActivity.this, PasswordManagerActivity.class);
            startActivity(intent);
            finish();
        } else if (itemId == R.id.nav_settings) {
            Intent intent2 = new Intent(UserGuideActivity.this, SettingsActivity.class);
            startActivity(intent2);
            finish();
        } else if (itemId == R.id.nav_help) {
            Intent intent4 = new Intent(UserGuideActivity.this, HelpAndSupportActivity.class);
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
}

