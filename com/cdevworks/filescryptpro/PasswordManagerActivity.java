package com.cdevworks.filescryptpro;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.material.navigation.NavigationView;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class PasswordManagerActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    DrawerLayout drawerLayout;
    NavigationView navigationView;
    Toolbar toolbar;
    TextView infoText2;
    static AlertDialog ynDialog, pDialog, msgDialog;
    View ynView, password_inputView, msg_box;
    LayoutInflater layoutInflater;
    Button pSubmitBtn;
    static int pSubmitMode = 1;
    static ArrayList<String> importFiles = new ArrayList<>();
    static ArrayList<String> defaultImportFiles = new ArrayList<>();
    EditText pwdField;
    static boolean cardBackgroundDimmer = false, openCSFileChooserUponPermission = false;
    View dimOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_manager);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        navigationView.bringToFront();
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);
        layoutInflater = LayoutInflater.from(this);
        ynDialog = new AlertDialog.Builder(this).create();
        msgDialog = new AlertDialog.Builder(this).create();
        ynView = this.layoutInflater.inflate(R.layout.yes_no_box, drawerLayout, false);
        msg_box = this.layoutInflater.inflate(R.layout.msg_box, drawerLayout, false);
        pDialog = new AlertDialog.Builder(this).create();
        password_inputView = this.layoutInflater.inflate(R.layout.password_input, drawerLayout, false);
        pSubmitBtn = password_inputView.findViewById(R.id.pSubmit);
        pwdField = password_inputView.findViewById(R.id.editTextPassword);
        infoText2 = findViewById(R.id.infoTextView2);
        dimOverlay = findViewById(R.id.dim_overlay2);
        dimOverlay.setVisibility(View.GONE);
        keyInfoViewUpdate();
        pSubmitBtn.setOnClickListener(v -> {
            if (pwdField.getText().toString().length() > 0) {
                if (pSubmitMode == 1) {
                    importPM(importFiles, defaultImportFiles, pwdField.getText().toString().toCharArray());
                } else {
                    Thread thread = new Thread(() -> {
                        if (!MainActivity.checkPermission(this)) {
                            MainActivity.getPermissions(this, this);
                            try {
                                while (!MainActivity.checkPermission(this)) {
                                    //noinspection BusyWait
                                    Thread.sleep(100);
                                }
                                Thread.sleep(250);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        runOnUiThread(this::showCardBackgroundDimmerWithLoader);
                        ExportResult exportResult = PasswordManager.exportPM(pwdField.getText().toString().toCharArray());
                        runOnUiThread(() -> {
                            hideCardBackgroundDimmerWithLoader();
                            if (exportResult.result) {
                                showMsgBox("File has been saved at " + exportResult.filePath.substring(exportResult.filePath.indexOf(MainActivity.externalFilesDir.getPath()) + MainActivity.externalFilesDir.getPath().length()));
                            } else {
                                showMsgBox("An error occurred while attempting to export the file.");
                            }
                        });
                    });
                    thread.start();
                }
                pDialog.dismiss();
            } else
                Toast.makeText(getApplicationContext(), "Password must not be empty!", Toast.LENGTH_SHORT).show();
        });

        AdView mAdView = findViewById(R.id.adView2);
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
    public void onResume() {
        super.onResume();
        if (openCSFileChooserUponPermission) {
            openCSFileChooserUponPermission = false;
            Thread thread = new Thread(() -> {
                try {
                    Thread.sleep(150);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                runOnUiThread(() -> findViewById(R.id.importButton).callOnClick());
            });
            thread.start();
        }

        keyInfoViewUpdate();
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
            Intent intent = new Intent(PasswordManagerActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        } else if (itemId == R.id.nav_settings) {
            Intent intent2 = new Intent(PasswordManagerActivity.this, SettingsActivity.class);
            startActivity(intent2);
            finish();
        } else if (itemId == R.id.nav_user_guide) {
            Intent intent3 = new Intent(PasswordManagerActivity.this, UserGuideActivity.class);
            startActivity(intent3);
            finish();
        } else if (itemId == R.id.nav_help) {
            Intent intent4 = new Intent(PasswordManagerActivity.this, HelpAndSupportActivity.class);
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

    public void keyInfoViewUpdate() {
        try {
            int noOfKeys = PasswordManager.ks.size();
            if (noOfKeys == 1)
                infoText2.setText(getString(R.string.passwords_are_securely_saved, String.valueOf(noOfKeys), "password is"));
            else if (noOfKeys == 0) {
                infoText2.setText(getString(R.string.passwords_are_securely_saved, "", " passwords are"));
            } else
                infoText2.setText(getString(R.string.passwords_are_securely_saved, String.valueOf(noOfKeys), "passwords are"));
        } catch (Exception ignored) {
            infoText2.setText(getString(R.string.passwords_are_securely_saved, "", " passwords are"));
        }
    }

    public void showMsgBox(String msg) {
        try {
            msgDialog.setView(msg_box);
            Objects.requireNonNull(msgDialog.getWindow()).setBackgroundDrawableResource(R.drawable.dialog_border);
            ((TextView) msg_box.findViewById(R.id.msg_box_textView)).setText(msg);
            msg_box.findViewById(R.id.msgBoxCloseBtn).setOnClickListener(v -> msgDialog.dismiss());
            msgDialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onBtnExport(View view) {
        ynDialog.setView(ynView);
        Objects.requireNonNull(ynDialog.getWindow()).setBackgroundDrawableResource(R.drawable.dialog_border);
        ynDialog.show();
    }

    public void onYesDialogBtn(View view) {
        ynDialog.dismiss();
        pSubmitMode = 2;
        showPasswordDialogBox();
    }

    public void showPasswordDialogBox() {
        pDialog.setView(password_inputView);
        Objects.requireNonNull(pDialog.getWindow()).setBackgroundDrawableResource(R.drawable.dialog_border);
        TextView strInfo = password_inputView.findViewById(R.id.info_textView);
        LinearLayout layout = password_inputView.findViewById(R.id.pDialogPMLayout);
        EditText pwdField = password_inputView.findViewById(R.id.editTextPassword);
        ImageButton pDialogClose = password_inputView.findViewById(R.id.pDialogClose);
        strInfo.setVisibility(View.GONE);
        layout.setVisibility(View.GONE);
        pwdField.setText("");
        pDialogClose.setOnClickListener(v -> pDialog.dismiss());
        pDialog.show();
    }

    public void onNoDialogBtn(View view) {
        ynDialog.dismiss();
        Thread thread = new Thread(() -> {
            if (!MainActivity.checkPermission(this)) {
                MainActivity.getPermissions(this, this);
                try {
                    while (!MainActivity.checkPermission(this)) {
                        //noinspection BusyWait
                        Thread.sleep(100);
                    }
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            runOnUiThread(this::showCardBackgroundDimmerWithLoader);
            ExportResult exportResult = PasswordManager.exportPM(null);
            runOnUiThread(() -> {
                hideCardBackgroundDimmerWithLoader();
                if (exportResult.result) {
                    showMsgBox("File has been saved at \n" + exportResult.filePath.substring(exportResult.filePath.indexOf(MainActivity.externalFilesDir.getPath()) + MainActivity.externalFilesDir.getPath().length()));
                } else {
                    showMsgBox("An error occurred while attempting to export the file.");
                }
            });
        });
        thread.start();
    }

    public void onBtnImport(View view) {
        if (MainActivity.checkPermission(this)) {
            CSFileChooser.itemPickMode = 2;
            CSFileChooser.chosenFiles.clear();
            Intent intent;
            intent = new Intent(this, CSFileChooser.class);
            activityResultLauncher.launch(intent);
        } else {
            MainActivity.getPermissions(this, this);
            Thread thread = new Thread(() -> {
                while (!MainActivity.checkPermission(this)) {
                    try {
                        //noinspection BusyWait
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                openCSFileChooserUponPermission = true;
            });
            thread.start();
        }
    }

    public void importPM(ArrayList<String> importFiles, ArrayList<String> defaultImportFiles, char[] pwd) {
        Thread thread = new Thread(() -> {
            runOnUiThread(this::showCardBackgroundDimmerWithLoader);
            ArrayList<String> successfulImports = new ArrayList<>();
            ArrayList<String> errorImports = new ArrayList<>();
            if (importFiles != null && (importFiles.size() - defaultImportFiles.size()) > 0)
                for (String filePath : importFiles) {
                    if (defaultImportFiles.contains(filePath)) continue;
                    if (PasswordManager.importPM(filePath, pwd)) {
                        successfulImports.add(new File(filePath).getName());
                    } else {
                        errorImports.add(new File(filePath).getName());
                    }
                }
            if (defaultImportFiles != null && defaultImportFiles.size() > 0)
                for (String filePath : defaultImportFiles) {
                    if (PasswordManager.importPM(filePath, PasswordManager.defaultExportPassword)) {
                        successfulImports.add(new File(filePath).getName());
                    } else {
                        errorImports.add(new File(filePath).getName());
                    }
                }
            StringBuilder sb = new StringBuilder();
            if (successfulImports.size() > 0)
                sb.append("Successfully imported the following file(s): \n").append(Arrays.toString(successfulImports.toArray(new String[0])));
            if (errorImports.size() > 0) {
                if (sb.length() > 0) sb.append("\n\n");
                sb.append("Error while importing the following file(s) (check password): \n").append(Arrays.toString(errorImports.toArray(new String[0])));
            }
            runOnUiThread(() -> {
                hideCardBackgroundDimmerWithLoader();
                keyInfoViewUpdate();
                showMsgBox(sb.toString());
            });
        });
        thread.start();
    }

    public void showCardBackgroundDimmerWithLoader() {
        cardBackgroundDimmer = true;
        Animation fadeInAnimation = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        dimOverlay.setVisibility(View.VISIBLE);
        dimOverlay.startAnimation(fadeInAnimation);
        disableClicksForViewsBelow(dimOverlay);
    }

    public void hideCardBackgroundDimmerWithLoader() {
        cardBackgroundDimmer = false;
        Animation fadeOutAnimation = AnimationUtils.loadAnimation(this, android.R.anim.fade_out);
        dimOverlay.startAnimation(fadeOutAnimation);
        dimOverlay.setVisibility(View.GONE);
        enableClicksForViewsBelow(dimOverlay);
    }

    private void disableClicksForViewsBelow(View view) {
        // Disable clicks on the views below the given view
        view.setClickable(true);
        view.setFocusable(true);
    }

    private void enableClicksForViewsBelow(View view) {
        // Enable clicks on the views below the given view
        view.setClickable(false);
        view.setFocusable(false);
    }

    ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        Intent intent = result.getData();
        if (result.getResultCode() == 93) {
            if (intent != null) {
                //getting chosenFiles
                importFiles = intent.getStringArrayListExtra("result");
                defaultImportFiles.clear();
                if (importFiles.size() > 0) {
                    for (String filePath : importFiles) {
                        try (FileInputStream fis = new FileInputStream(filePath)) {
                            KeyStore ks1 = KeyStore.getInstance("BKS");
                            ks1.load(fis, PasswordManager.defaultExportPassword);
                            defaultImportFiles.add(filePath);
                        } catch (CertificateException | NoSuchAlgorithmException |
                                 KeyStoreException e) {
                            throw new RuntimeException(e);
                        } catch (IOException ignored) {
                        }
                    }
                    if ((importFiles.size() - defaultImportFiles.size()) > 0) {
                        pSubmitMode = 1;
                        showPasswordDialogBox();
                    } else if (defaultImportFiles.size() > 0) {
                        importPM(null, defaultImportFiles, null);
                    }
                }
            }
        }
    });
}
