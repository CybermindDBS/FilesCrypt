package com.cdevworks.filescryptpro;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.material.navigation.NavigationView;

public class SettingsActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    Toolbar toolbar;
    NavigationView navigationView;
    DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.settings, new SettingsFragment()).commit();
        }

        drawerLayout = findViewById(R.id.drawer_layout);
        toolbar = findViewById(R.id.toolbar);
        navigationView = findViewById(R.id.nav_view);
        setSupportActionBar(toolbar);
        findViewById(R.id.imageButtonSettingsClose).setOnClickListener(v -> finish());
        navigationView.bringToFront();
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);
        MainActivity.updateAppSetting = true;

        if (!getSharedPreferences("FilesCryptPro_Prefs", Context.MODE_PRIVATE).getBoolean("advis", true)) {
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
            Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        } else if (itemId == R.id.nav_password_manager) {
            Intent intent = new Intent(SettingsActivity.this, PasswordManagerActivity.class);
            startActivity(intent);
            finish();
        } else if (itemId == R.id.nav_user_guide) {
            Intent intent3 = new Intent(SettingsActivity.this, UserGuideActivity.class);
            startActivity(intent3);
            finish();
        } else if (itemId == R.id.nav_help) {
            Intent intent4 = new Intent(SettingsActivity.this, HelpAndSupportActivity.class);
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

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
        }
    }
}
