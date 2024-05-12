package com.cdevworks.filescryptpro;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.os.Build.VERSION.SDK_INT;
import static androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK;
import static androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.preference.PreferenceManager;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.common.collect.ImmutableList;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    static boolean mainUIRunning;
    Intent foregroundService;
    static File internalFilesDir, cacheDir;
    static File externalFilesDir, externalAppDir;
    static File reportFile;
    protected static Handler handler;
    protected static Runnable runnable, runnable2;
    protected static String toastMessage, version;
    static boolean statusUpdateRunning = false;
    static boolean cardBackgroundDimmer = false;
    static boolean updateAppSetting = false;
    static boolean openCSFileChooserUponPermission = false;
    static boolean disableSameAsSourceBtn = false;
    static boolean operationCanceled = false;
    static boolean initiateInAppPurchase = false;
    static boolean nowPurchasing = false;
    static ArrayList<String> files;
    static int cipherMode, pSubmitMode = 0;
    static String pwd, saveLocation;
    static char[] auth = new char[0];
    static ArrayList<com.cdevworks.filescryptpro.CryptorEngineInput> cryptorEngineRetryInputs = new ArrayList<>();
    static ArrayList<com.cdevworks.filescryptpro.CryptorEngineInput> cryptorEngineDirRetryInputs = new ArrayList<>();
    private InterstitialAd mInterstitialAd;
    Context context;
    DrawerLayout drawerLayout;
    NavigationView navigationView;
    Toolbar toolbar;
    MaterialButtonToggleGroup buttonToggleGroup;
    Button chooseFilesBtn;
    CardView cardView_encryption_settings, cardView_decryption_settings, statusCard;
    CheckBox encryptFilenamesChkBox, encryptFolderNamesChkBox, deleteSourceFilesChkBox, deleteSourceFilesChkBox2, saveToPMChkBox;
    TextInputLayout IVTextBox;
    static String strIV;
    ConstraintLayout operationDetails, ongoingOperation, idleView;
    LinearLayout pDialogEncOp, pDialogDecOp;
    TextView filesProcessedStatus, processedSizeStatus, ongoingOperationStats, ongoingOperationStats2, completionStatus, fileProcessCompletionStat;
    ProgressBar progressBar;
    Button btnAppDir, pSubmitBtn, btnEncrypt, btnDecrypt;
    static AlertDialog pDialog, sDialog, msgDialog;
    EditText pwdField;
    LayoutInflater layoutInflater;
    View password_inputView, save_locationView, msg_box;
    View dimOverlay;
    SharedPreferences sharedPreferences;
    Animation fadeInAnimation, fadeInAnimation2, fadeInAnimationWithDelay;

    //getting return data from CSFileChooser
    ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            Intent intent = result.getData();
            if (result.getResultCode() == 36) {
                if (intent != null) {
                    //getting chosenFiles
                    files = intent.getStringArrayListExtra("result");
                    if (files != null && files.size() > 0) {
                        if (buttonToggleGroup.getCheckedButtonId() == R.id.btnDecrypt)
                            cipherMode = 2;
                        else cipherMode = 1;
                        showPasswordDialogBox();
                    }
                }
            } else if (result.getResultCode() == 63) {
                if (intent != null) {
                    try {
                        saveLocation = Objects.requireNonNull(intent.getStringArrayListExtra("result")).get(0);
                        startOperation(files);
                    } catch (Exception ignored) {
                    }
                }
            }

            if (getSharedPreferences("FilesCryptPro_Prefs", Context.MODE_PRIVATE).getBoolean("advis", true))
                if (mInterstitialAd != null) {
                    mInterstitialAd.show(MainActivity.this);
                } else {
                    Log.d("tag1", "The interstitial ad wasn't ready yet.");
                }

        }
    });

    ActivityResultLauncher<Intent> activityResultLauncher2 = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        Intent intent = result.getData();
        if (result.getResultCode() == 9) {
            if (intent != null) {
                if (intent.getBooleanExtra("result", false)) {
                    com.cdevworks.filescryptpro.OperationReport report;
                    try (FileInputStream fis = new FileInputStream(MainActivity.reportFile); ObjectInputStream ois = new ObjectInputStream(fis)) {
                        report = (com.cdevworks.filescryptpro.OperationReport) ois.readObject();
                        if (files == null) files = new ArrayList<>();
                        else files.clear();
                        cryptorEngineRetryInputs.clear();
                        cryptorEngineDirRetryInputs.clear();
                        for (String key : report.log.keySet()) {
                            if (!key.contains("Successfully")) {
                                if (key.contains("Folder"))
                                    cryptorEngineDirRetryInputs.addAll(Objects.requireNonNull(report.log.get(key)));
                                else
                                    cryptorEngineRetryInputs.addAll(Objects.requireNonNull(report.log.get(key)));
                            }
                        }
                        pSubmitMode = 1;
                        showPasswordDialogBox();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    });


    //For in App Purchase implementation.
    private BillingClient billingClient;
    static ProductDetails donateVersionProductDetails;
    private final PurchasesUpdatedListener purchasesUpdatedListener = (billingResult, purchases) -> {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (Purchase purchase : purchases) {
                handlePurchase(purchase);
            }
        } else //noinspection StatementWithEmptyBody
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
                // Handle an error caused by a user cancelling the purchase flow.
            } else {
                // Handle any other error codes.
            }
    };

    void establishGooglePlayBillingConnection() {
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    // The BillingClient is ready. You can query purchases here.
                    getProductDetails();
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
                establishGooglePlayBillingConnection();
            }
        });
    }

    void getProductDetails() {
        QueryProductDetailsParams queryProductDetailsParams = QueryProductDetailsParams.newBuilder().setProductList(ImmutableList.of(QueryProductDetailsParams.Product.newBuilder().setProductId("filescryptpro_noads_donateversion").setProductType(BillingClient.ProductType.INAPP).build())).build();
        billingClient.queryProductDetailsAsync(queryProductDetailsParams, (billingResult, productDetailsList) -> {
            // check billingResult
            // process returned productDetailsList
            //noinspection ConstantValue
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && productDetailsList != null) {
                donateVersionProductDetails = productDetailsList.get(0);
            }
        });
    }

    void handlePurchase(Purchase purchase) {
        if (!purchase.isAcknowledged()) {
            AcknowledgePurchaseParams acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.getPurchaseToken()).build();
            billingClient.acknowledgePurchase(acknowledgePurchaseParams, billingResult -> {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {

                    if (purchase.getProducts().get(0).equals("filescryptpro_noads_donateversion")) {
                        Log.i("tag1", "Acknowledgment success: " + billingResult.getResponseCode());
                        updateSharedPreference("advis", false);
                        runOnUiThread(() -> {
                            AdView mAdView = findViewById(R.id.adView);
                            mAdView.setVisibility(View.GONE);

                            Menu menu = navigationView.getMenu();
                            menu.findItem(R.id.nav_donate).setVisible(false);

                            View thankYou_View = this.layoutInflater.inflate(R.layout.thank_you_noads, drawerLayout, false);
                            AlertDialog tyDialog = new AlertDialog.Builder(this).create();
                            tyDialog.setView(thankYou_View);
                            Objects.requireNonNull(tyDialog.getWindow()).setBackgroundDrawableResource(R.drawable.dialog_border);
                            ImageButton tyDialogClose = thankYou_View.findViewById(R.id.tyDialogClose);
                            tyDialogClose.setOnClickListener(v -> tyDialog.dismiss());
                            tyDialog.show();
                        });
                    }
                } else {
                    // Handle acknowledgment failure, show an error message, or retry acknowledging.
                    // You should also log or handle other response codes appropriately.
                    Log.i("tag1", "Acknowledgment failed: " + billingResult.getResponseCode());
                }
            });
        }
        nowPurchasing = false;
    }

    // The following method activates no ads version on the second device of the user when the app is launching if he has purchased already purchased this feature on some other device,etc.
    void activatePurchasesEffects() {
        billingClient = BillingClient.newBuilder(this).enablePendingPurchases().setListener((billingResult, list) -> {
        }).build();
        final BillingClient finalBillingClient = billingClient;
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingServiceDisconnected() {
                establishGooglePlayBillingConnection();
            }

            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    finalBillingClient.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(), (billingResult1, list) -> {

                        if (billingResult1.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                            AdView mAdView = findViewById(R.id.adView);
                            if (list.size() > 0) {
                                for (Purchase purchase : list) {
                                    if (purchase.getProducts().get(0).equals("filescryptpro_noads_donateversion")) {
                                        updateSharedPreference("advis", false);
                                        runOnUiThread(() -> {
                                            mAdView.setVisibility(View.GONE);
                                            Menu menu = navigationView.getMenu();
                                            menu.findItem(R.id.nav_donate).setVisible(false);
                                        });
                                    } else {
                                        updateSharedPreference("advis", true);
                                        runOnUiThread(() -> mAdView.setVisibility(View.VISIBLE));
                                    }
                                }
                            } else {
                                updateSharedPreference("advis", true);
                                runOnUiThread(() -> mAdView.setVisibility(View.VISIBLE));
                            }
                        }
                    });
                }
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SplashScreen.installSplashScreen(this);
        setContentView(R.layout.activity_main);
        context = getApplicationContext();

        Thread cacheClearer = new Thread(MainActivity::deleteCache);
        cacheClearer.start();

        internalFilesDir = context.getFilesDir();
        cacheDir = context.getCacheDir();
        externalFilesDir = Environment.getExternalStorageDirectory();
        externalAppDir = new File(externalFilesDir.getPath() + File.separator + "FilesCrypt Pro");
        reportFile = new File(internalFilesDir + File.separator + "FileCipherReport");

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        toolbar = findViewById(R.id.toolbar);
        fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        fadeInAnimation2 = AnimationUtils.loadAnimation(this, R.anim.fade_in_2);
        fadeInAnimationWithDelay = AnimationUtils.loadAnimation(this, R.anim.fade_in_with_delay);
        buttonToggleGroup = findViewById(R.id.toggleButtonGroup);
        buttonToggleGroup.startAnimation(fadeInAnimation);
        cardView_encryption_settings = findViewById(R.id.cardView_encryption_settings);
        cardView_encryption_settings.startAnimation(fadeInAnimation);
        cardView_decryption_settings = findViewById(R.id.cardView_decryption_settings);
        statusCard = findViewById(R.id.cardView);
        statusCard.startAnimation(fadeInAnimation);
        chooseFilesBtn = findViewById(R.id.choose_files);
        chooseFilesBtn.startAnimation(fadeInAnimation);
        encryptFilenamesChkBox = findViewById(R.id.checkBox_encrypt_filenames);
        encryptFolderNamesChkBox = findViewById(R.id.checkBox_encrypt_foldernames);
        deleteSourceFilesChkBox = findViewById(R.id.checkBox_delete_source_files);
        deleteSourceFilesChkBox2 = findViewById(R.id.checkBox2_delete_source_files);
        IVTextBox = findViewById(R.id.textInputLayout);
        btnEncrypt = findViewById(R.id.btnEncrypt);
        btnDecrypt = findViewById(R.id.btnDecrypt);
        operationDetails = findViewById(R.id.operationDetails);
        ongoingOperation = findViewById(R.id.ongoingOperation);
        idleView = findViewById(R.id.idleView);
        filesProcessedStatus = findViewById(R.id.filesProcessedStatus);
        processedSizeStatus = findViewById(R.id.processedSizeStatus);
        ongoingOperationStats = findViewById(R.id.ongoingOperationStats);
        ongoingOperationStats2 = findViewById(R.id.ongoingOperationStats2);
        completionStatus = findViewById(R.id.completionStatus);
        fileProcessCompletionStat = findViewById(R.id.fileProcessCompletionStat);
        progressBar = findViewById(R.id.progressBar);
        pDialog = new AlertDialog.Builder(this).create();
        sDialog = new AlertDialog.Builder(this).create();
        msgDialog = new AlertDialog.Builder(this).create();
        layoutInflater = LayoutInflater.from(this);
        password_inputView = this.layoutInflater.inflate(R.layout.password_input, drawerLayout, false);
        save_locationView = this.layoutInflater.inflate(R.layout.choose_save_location, drawerLayout, false);
        msg_box = this.layoutInflater.inflate(R.layout.msg_box, drawerLayout, false);
        pDialogEncOp = password_inputView.findViewById(R.id.pDialogEncOp);
        pDialogDecOp = password_inputView.findViewById(R.id.pDialogDecOp);
        pSubmitBtn = password_inputView.findViewById(R.id.pSubmit);
        pwdField = password_inputView.findViewById(R.id.editTextPassword);
        saveToPMChkBox = password_inputView.findViewById(R.id.saveToPMChkBox);
        btnAppDir = save_locationView.findViewById(R.id.btnAppDir);
        dimOverlay = findViewById(R.id.dim_overlay);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        AdView mAdView = findViewById(R.id.adView);
        setSupportActionBar(toolbar);

        navigationView.bringToFront();
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setCheckedItem(R.id.nav_home);

        billingClient = BillingClient.newBuilder(context).setListener(purchasesUpdatedListener).enablePendingPurchases().build();
        establishGooglePlayBillingConnection();
        activatePurchasesEffects();

        //Update Check Implementation
        Thread updateCheck = new Thread(() -> {
            AppUpdateManager appUpdateManager = AppUpdateManagerFactory.create(context);
            // Returns an intent object that you use to check for an update.
            Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();
            // Checks whether the platform allows the specified type of update,
            // and checks the update priority.
            appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                    runOnUiThread(() -> {
                        CEUtils.showMessage("Please update the app to continue.", 1);
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.cdevworks.filescryptpro")));
                        finish();
                    });
                }
            });
        });
        updateCheck.start();

        if (getSharedPreferences("FilesCryptPro_Prefs", Context.MODE_PRIVATE).getBoolean("advis", true)) {

            MobileAds.initialize(this, initializationStatus -> {
            });
            mAdView.setVisibility(View.VISIBLE);
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);

            InterstitialAd.load(this, "ca-app-pub-2911045801719011/3582606142", adRequest, new InterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                    // The mInterstitialAd reference will be null until
                    // an ad is loaded.
                    mInterstitialAd = interstitialAd;
                    updateSharedPreference("adval", 0);
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    // Handle the error
                    Log.i("TAG", "onAdFailedToLoad: >>>>>>>>>>>>>>> NOT LOADED <<<<<<<<<<<<<<<<<<<<<<<<<");
                    mInterstitialAd = null;
                    if (getSharedPreferences("FilesCryptPro_Prefs", Context.MODE_PRIVATE).getInt("adval", 0) >= 1) {
                        showMsgBox("Please ensure your device has internet access and that any ad blockers are disabled to access all app features. Once you have done so, restart the app to activate all features. Ad support sustains the app. You can still decrypt files/folders normally. Explore an ad-free experience by purchasing the No Ads (donate version) in-app. Thank you.");
                        buttonToggleGroup.check(R.id.btnDecrypt);
                        btnEncrypt.setEnabled(false);
                        btnEncrypt.setTextColor(getColor(R.color.disabled_button_text_color));
                    } else
                        updateSharedPreference("adval", getSharedPreferences("FilesCryptPro_Prefs", Context.MODE_PRIVATE).getInt("adval", 0) + 1);
                }
            });
        } else {
            mAdView.setVisibility(View.GONE);
            Menu menu = navigationView.getMenu();
            menu.findItem(R.id.nav_donate).setVisible(false);
        }

        updateAppSettings();
        pSubmitBtn.setOnClickListener(v -> {
            if (pwdField.getText().toString().length() > 0) {
                pwd = pwdField.getText().toString();
                if (cipherMode == 1) {
                    if (saveToPMChkBox.isChecked()) auth = new char[3];
                    else auth = new char[1];
                } else auth = new char[0];
                pDialog.dismiss();
                if (pSubmitMode == 0) showSaveLocationDialogBox();
                else retryOperation(1);
            } else
                Toast.makeText(getApplicationContext(), "Password must not be empty!", Toast.LENGTH_SHORT).show();
        });

        try {
            PackageInfo pInfo = this.getPackageManager().getPackageInfo(this.getPackageName(), 0);
            version = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        mainUIRunning = true;
        handler = new Handler(Looper.getMainLooper());
        runnable = () -> Toast.makeText(getApplicationContext(), toastMessage, Toast.LENGTH_LONG).show();
        runnable2 = () -> showMsgBox(toastMessage);
        foregroundService = new Intent(this, com.cdevworks.filescryptpro.CryptorEngineService.class);
        context.startForegroundService(foregroundService);

        buttonToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnEncrypt) {
                    cardView_decryption_settings.setVisibility(View.GONE);
                    cardView_encryption_settings.setVisibility(View.VISIBLE);
                    buttonToggleGroup.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                } else {
                    cardView_encryption_settings.setVisibility(View.GONE);
                    cardView_decryption_settings.setVisibility(View.VISIBLE);
                    buttonToggleGroup.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                }
            }
        });

        ongoingOperationUpdate();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!nowPurchasing) {
            billingClient.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(), (billingResult, list) -> {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    for (Purchase purchase : list) {
                        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged()) {
                            handlePurchase(purchase);
                        }
                    }
                }
            });
        }

        if (initiateInAppPurchase) {
            initiateInAppPurchase = false;
            if (donateVersionProductDetails != null) {
                Activity activity = MainActivity.this;
                ImmutableList<BillingFlowParams.ProductDetailsParams> productDetailsParamsList = ImmutableList.of(BillingFlowParams.ProductDetailsParams.newBuilder()
                        // retrieve a value for "productDetails" by calling queryProductDetailsAsync()
                        .setProductDetails(donateVersionProductDetails)
                        // to get an offer token, call ProductDetails.getSubscriptionOfferDetails()
                        // for a list of offers that are available to the user
                        .build());
                BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder().setProductDetailsParamsList(productDetailsParamsList).build();
                // Launch the billing flow
                BillingResult billingResult = billingClient.launchBillingFlow(activity, billingFlowParams);
                if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                    CEUtils.showMessage("Loading, Try again shortly", 1);
                }
            } else {
                establishGooglePlayBillingConnection();
                findViewById(R.id.dim_overlay3).setVisibility(View.VISIBLE);
                Thread thread = new Thread(() -> {
                    while (donateVersionProductDetails == null) {
                        try {
                            //noinspection BusyWait
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    runOnUiThread(() -> {
                        findViewById(R.id.dim_overlay3).setVisibility(View.GONE);
                        Activity activity = MainActivity.this;
                        ImmutableList<BillingFlowParams.ProductDetailsParams> productDetailsParamsList = ImmutableList.of(BillingFlowParams.ProductDetailsParams.newBuilder()
                                // retrieve a value for "productDetails" by calling queryProductDetailsAsync()
                                .setProductDetails(donateVersionProductDetails)
                                // to get an offer token, call ProductDetails.getSubscriptionOfferDetails()
                                // for a list of offers that are available to the user
                                .build());
                        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder().setProductDetailsParamsList(productDetailsParamsList).build();
                        // Launch the billing flow
                        BillingResult billingResult = billingClient.launchBillingFlow(activity, billingFlowParams);
                        if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                            CEUtils.showMessage("Loading, Try again shortly", 1);
                        }
                    });
                });
                thread.start();
            }
        }

        if (updateAppSetting) {
            updateAppSetting = false;
            updateAppSettings();
        }

        if (openCSFileChooserUponPermission) {
            openCSFileChooserUponPermission = false;
            Thread thread = new Thread(() -> {
                try {
                    Thread.sleep(150);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                runOnUiThread(() -> findViewById(R.id.choose_files).callOnClick());
            });
            thread.start();
        }
    }

    /**
     * @noinspection unused
     */
    public static void deleteCache() {
        ArrayList<File> cacheDirs = new ArrayList<>();
        try {
            cacheDirs.add(MainActivity.cacheDir);
            File codeCacheFile = new File(Objects.requireNonNull(MainActivity.internalFilesDir.getParentFile()).getPath() + File.separator + "code_cache");
            if (codeCacheFile.exists()) cacheDirs.add(codeCacheFile);
            for (File cacheDir : cacheDirs)
                deleteDir(cacheDir);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                for (String child : children) {
                    boolean success = deleteDir(new File(dir, child));
                    if (!success) {
                        return false;
                    }
                }
            }
            return dir.delete();
        } else if (dir != null && dir.isFile()) {
            return dir.delete();
        } else {
            return false;
        }
    }

    @SuppressWarnings("BusyWait")
    public void ongoingOperationUpdate() {
        Thread statusUpdater = new Thread(() -> {
            if (statusUpdateRunning) return;
            statusUpdateRunning = true;
            try {
                if (!(com.cdevworks.filescryptpro.FileOperationLog.makingCryptorEngineInputs || com.cdevworks.filescryptpro.FileOperationLog.cryptorEnginesProcessing || com.cdevworks.filescryptpro.FileOperationLog.folderCryptionOnProgress)) {
                    runOnUiThread(() -> {
                        ongoingOperation.setVisibility(View.GONE);
                        if (MainActivity.reportFile.exists()) {
                            com.cdevworks.filescryptpro.OperationReport.loadReport();
                            if (com.cdevworks.filescryptpro.FileOperationLog.report.status) {
                                completionStatus.setText(getString(R.string.completion_status, "Successfully"));
                                completionStatus.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.checkbox_multiple_marked_circle_outline, 0);
                            } else {
                                completionStatus.setText(getString(R.string.completion_status, "With Error(s)"));
                                completionStatus.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.alert_circle_outline, 0);
                            }
                            if (com.cdevworks.filescryptpro.FileOperationLog.report.filesToEncrypt > 0 && com.cdevworks.filescryptpro.FileOperationLog.report.filesToDecrypt > 0)
                                fileProcessCompletionStat.setText(getString(R.string.processed_files_status, String.valueOf(com.cdevworks.filescryptpro.FileOperationLog.report.filesProcessed), String.valueOf(com.cdevworks.filescryptpro.FileOperationLog.report.totalFiles)));
                            else if (com.cdevworks.filescryptpro.FileOperationLog.report.filesToEncrypt > 0)
                                fileProcessCompletionStat.setText(getString(R.string.processed_files_status2, String.valueOf(com.cdevworks.filescryptpro.FileOperationLog.report.filesProcessed), String.valueOf(com.cdevworks.filescryptpro.FileOperationLog.report.totalFiles)));
                            else if (com.cdevworks.filescryptpro.FileOperationLog.report.filesToDecrypt > 0)
                                fileProcessCompletionStat.setText(getString(R.string.processed_files_status3, String.valueOf(com.cdevworks.filescryptpro.FileOperationLog.report.filesProcessed), String.valueOf(com.cdevworks.filescryptpro.FileOperationLog.report.totalFiles)));
                            idleView.setVisibility(View.GONE);
                            operationDetails.setVisibility(View.VISIBLE);
                        } else {
                            operationDetails.setVisibility(View.GONE);
                            ongoingOperation.setVisibility(View.GONE);
                            idleView.setVisibility(View.VISIBLE);
                        }
                    });
                    while (!(com.cdevworks.filescryptpro.FileOperationLog.makingCryptorEngineInputs || com.cdevworks.filescryptpro.FileOperationLog.cryptorEnginesProcessing || com.cdevworks.filescryptpro.FileOperationLog.folderCryptionOnProgress) && statusUpdateRunning)
                        Thread.sleep(1000);
                }
                if (com.cdevworks.filescryptpro.FileOperationLog.makingCryptorEngineInputs && statusUpdateRunning) {
                    runOnUiThread(() -> {
                        idleView.setVisibility(View.GONE);
                        operationDetails.setVisibility(View.GONE);
                        filesProcessedStatus.setText(getString(R.string.one_moment));
                        ongoingOperationStats.setText(getString(R.string.calc_input));
                        ongoingOperationStats2.setText(getString(R.string.ongoing_operation_stats2, String.valueOf(0)));
                        processedSizeStatus.setText(getString(R.string.ongoing_operation_stats3, "0", "0"));
                        progressBar.setIndeterminate(true);
                        ongoingOperation.setVisibility(View.VISIBLE);
                        showCardBackgroundDimmer();
                    });
                    while (com.cdevworks.filescryptpro.FileOperationLog.makingCryptorEngineInputs && statusUpdateRunning)
                        Thread.sleep(1000);
                }
                if (com.cdevworks.filescryptpro.FileOperationLog.cryptorEnginesProcessing && statusUpdateRunning) {
                    runOnUiThread(() -> {
                        progressBar.setIndeterminate(false);
                        idleView.setVisibility(View.GONE);
                        operationDetails.setVisibility(View.GONE);
                        filesProcessedStatus.setText(getString(R.string.ongoing_operation_status, String.valueOf(com.cdevworks.filescryptpro.FileOperationLog.filesProcessed), String.valueOf(com.cdevworks.filescryptpro.FileOperationLog.totalFiles)));
                        ongoingOperationStats.setText(getString(R.string.ongoing_operation_stats, String.valueOf(0)));
                        ongoingOperationStats2.setText(getString(R.string.ongoing_operation_stats2, String.valueOf(0)));
                        processedSizeStatus.setText(getString(R.string.ongoing_operation_stats3, String.valueOf(0), String.valueOf(com.cdevworks.filescryptpro.FileOperationLog.totalSizeInMB)));
                        ongoingOperation.setVisibility(View.VISIBLE);
                        showCardBackgroundDimmer();
                    });
                    while (com.cdevworks.filescryptpro.FileOperationLog.cryptorEnginesProcessing && statusUpdateRunning) {
                        long bytesProcessedCheckpoint = com.cdevworks.filescryptpro.FileOperationLog.processedBytes / 1048576L;
                        Thread.sleep(1000);
                        long bytesProcessedCheckpoint2 = com.cdevworks.filescryptpro.FileOperationLog.processedBytes / 1048576L;
                        int progress = (int) (((float) (com.cdevworks.filescryptpro.FileOperationLog.processedBytes / 1048576L) / (float) com.cdevworks.filescryptpro.FileOperationLog.totalSizeInMB) * 100);
                        runOnUiThread(() -> {
                            filesProcessedStatus.setText(getString(R.string.ongoing_operation_status, String.valueOf(com.cdevworks.filescryptpro.FileOperationLog.filesProcessed), String.valueOf(com.cdevworks.filescryptpro.FileOperationLog.totalFiles)));
                            ongoingOperationStats.setText(getString(R.string.ongoing_operation_stats, String.valueOf(progress)));
                            ongoingOperationStats2.setText(getString(R.string.ongoing_operation_stats2, String.valueOf(bytesProcessedCheckpoint2 - bytesProcessedCheckpoint)));
                            processedSizeStatus.setText(getString(R.string.ongoing_operation_stats3, String.valueOf(bytesProcessedCheckpoint2), String.valueOf(com.cdevworks.filescryptpro.FileOperationLog.totalSizeInMB)));
                            progressBar.setProgress(progress);
                        });
                        if (com.cdevworks.filescryptpro.FileOperationLog.folderCryptionOnProgress && statusUpdateRunning) {
                            runOnUiThread(() -> {
                                idleView.setVisibility(View.GONE);
                                operationDetails.setVisibility(View.GONE);
                                filesProcessedStatus.setText(getString(R.string.ongoing_operation_status, String.valueOf(com.cdevworks.filescryptpro.FileOperationLog.filesProcessed), String.valueOf(com.cdevworks.filescryptpro.FileOperationLog.totalFiles)));
                                ongoingOperationStats.setText(getString(R.string.one_moment));
                                processedSizeStatus.setText(getString(R.string.ongoing_operation_stats3, String.valueOf(com.cdevworks.filescryptpro.FileOperationLog.processedBytes / 1048576L), String.valueOf(com.cdevworks.filescryptpro.FileOperationLog.totalSizeInMB)));
                                progressBar.setIndeterminate(true);
                                ongoingOperation.setVisibility(View.VISIBLE);
                                showCardBackgroundDimmer();
                            });
                            while (com.cdevworks.filescryptpro.FileOperationLog.folderCryptionOnProgress && statusUpdateRunning)
                                Thread.sleep(1000);
                        }
                    }
                }
                runOnUiThread(() -> {
                    ongoingOperation.setVisibility(View.GONE);
                    idleView.setVisibility(View.GONE);
                    if (MainActivity.reportFile.exists()) {
                        com.cdevworks.filescryptpro.OperationReport.loadReport();
                        if (com.cdevworks.filescryptpro.FileOperationLog.report.status) {
                            completionStatus.setAnimation(fadeInAnimationWithDelay);
                            completionStatus.setText(getString(R.string.completion_status, "Successfully"));
                            completionStatus.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.checkbox_multiple_marked_circle_outline, 0);
                        } else {
                            completionStatus.setText(getString(R.string.completion_status, "With Error(s)"));
                            completionStatus.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.alert_circle_outline, 0);
                        }
                        if (com.cdevworks.filescryptpro.FileOperationLog.report.filesToEncrypt > 0 && com.cdevworks.filescryptpro.FileOperationLog.report.filesToDecrypt > 0)
                            fileProcessCompletionStat.setText(getString(R.string.processed_files_status, String.valueOf(com.cdevworks.filescryptpro.FileOperationLog.report.filesProcessed), String.valueOf(com.cdevworks.filescryptpro.FileOperationLog.report.totalFiles)));
                        else if (com.cdevworks.filescryptpro.FileOperationLog.report.filesToEncrypt > 0)
                            fileProcessCompletionStat.setText(getString(R.string.processed_files_status2, String.valueOf(com.cdevworks.filescryptpro.FileOperationLog.report.filesProcessed), String.valueOf(com.cdevworks.filescryptpro.FileOperationLog.report.totalFiles)));
                        else if (com.cdevworks.filescryptpro.FileOperationLog.report.filesToDecrypt > 0)
                            fileProcessCompletionStat.setText(getString(R.string.processed_files_status3, String.valueOf(com.cdevworks.filescryptpro.FileOperationLog.report.filesProcessed), String.valueOf(com.cdevworks.filescryptpro.FileOperationLog.report.totalFiles)));
                        operationDetails.startAnimation(fadeInAnimation2);
                        operationDetails.setVisibility(View.VISIBLE);
                        operationDetails.performHapticFeedback(HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
                        showCardBackgroundDimmer();
                    } else {
                        operationDetails.setVisibility(View.GONE);
                        ongoingOperation.setVisibility(View.GONE);
                        idleView.setVisibility(View.VISIBLE);
                    }
                });
                statusUpdateRunning = false;
            } catch (Exception ignored) {
                statusUpdateRunning = false;
            }
        });
        statusUpdater.start();
    }

    /**
     * @noinspection unused
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        if (itemId == R.id.nav_password_manager) {
            Intent intent = new Intent(MainActivity.this, PasswordManagerActivity.class);
            startActivity(intent);
        } else if (itemId == R.id.nav_settings) {
            Intent intent2 = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent2);
        } else if (itemId == R.id.nav_user_guide) {
            Intent intent3 = new Intent(MainActivity.this, UserGuideActivity.class);
            startActivity(intent3);
        } else if (itemId == R.id.nav_help) {
            Intent intent4 = new Intent(MainActivity.this, HelpAndSupportActivity.class);
            startActivity(intent4);
        } else if (itemId == R.id.nav_donate) {
            nowPurchasing = true;
            if (donateVersionProductDetails != null) {
                Activity activity = MainActivity.this;
                ImmutableList<BillingFlowParams.ProductDetailsParams> productDetailsParamsList = ImmutableList.of(BillingFlowParams.ProductDetailsParams.newBuilder()
                        // retrieve a value for "productDetails" by calling queryProductDetailsAsync()
                        .setProductDetails(donateVersionProductDetails)
                        // to get an offer token, call ProductDetails.getSubscriptionOfferDetails()
                        // for a list of offers that are available to the user
                        .build());
                BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder().setProductDetailsParamsList(productDetailsParamsList).build();
                // Launch the billing flow
                BillingResult billingResult = billingClient.launchBillingFlow(activity, billingFlowParams);
            } else {
                establishGooglePlayBillingConnection();
                findViewById(R.id.dim_overlay3).setVisibility(View.VISIBLE);
                Thread thread = new Thread(() -> {
                    while (donateVersionProductDetails == null) {
                        try {
                            //noinspection BusyWait
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    runOnUiThread(() -> {
                        findViewById(R.id.dim_overlay3).setVisibility(View.GONE);
                        Activity activity = MainActivity.this;
                        ImmutableList<BillingFlowParams.ProductDetailsParams> productDetailsParamsList = ImmutableList.of(BillingFlowParams.ProductDetailsParams.newBuilder()
                                // retrieve a value for "productDetails" by calling queryProductDetailsAsync()
                                .setProductDetails(donateVersionProductDetails)
                                // to get an offer token, call ProductDetails.getSubscriptionOfferDetails()
                                // for a list of offers that are available to the user
                                .build());
                        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder().setProductDetailsParamsList(productDetailsParamsList).build();
                        // Launch the billing flow
                        BillingResult billingResult = billingClient.launchBillingFlow(activity, billingFlowParams);
                        if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                            CEUtils.showMessage("Loading, Try again shortly", 1);
                        }
                    });
                });
                thread.start();
            }
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

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else if (dimOverlay.getVisibility() == View.VISIBLE) {
            hideCardBackgroundDimmer();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        statusUpdateRunning = false;
        mainUIRunning = false;
    }

    public void onFileChooseBtn(View view) {
        openCSFileChooserUponPermission = false;
        if (files != null) files.clear();
        //choosing files with CSFileChooser
        if (checkPermission(this)) {
            try {
                strIV = Objects.requireNonNull(IVTextBox.getEditText()).getText().toString().trim();
            } catch (Exception e) {
                strIV = null;
            }
            com.cdevworks.filescryptpro.CSFileChooser.itemPickMode = 0;
            com.cdevworks.filescryptpro.CSFileChooser.chosenFiles.clear();
            Intent intent;
            intent = new Intent(this, com.cdevworks.filescryptpro.CSFileChooser.class);
            activityResultLauncher.launch(intent);
        } else {
            getPermissions(this, this);
            Thread thread = new Thread(() -> {
                while (!checkPermission(this)) {
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


    public void startOperation(ArrayList<String> files) {
        boolean encryptFileNames, encryptFolderNames, deleteSourceFiles;
        operationCanceled = false;
        encryptFileNames = encryptFilenamesChkBox.isChecked();
        encryptFolderNames = encryptFolderNamesChkBox.isChecked();
        deleteSourceFiles = cipherMode == 1 && deleteSourceFilesChkBox.isChecked() || cipherMode == 2 && deleteSourceFilesChkBox2.isChecked();
        byte[] IVBytes, tmp;
        if (strIV == null || strIV.isEmpty()) IVBytes = null;
        else {
            IVBytes = new byte[16];
            tmp = strIV.getBytes(StandardCharsets.UTF_8);
            int paddingLen = 16 - tmp.length;
            if (paddingLen > 0) {
                byte[] paddingBytes = new byte[paddingLen];
                Arrays.fill(paddingBytes, (byte) '0');
                System.arraycopy(tmp, 0, IVBytes, 0, tmp.length);
                System.arraycopy(paddingBytes, 0, IVBytes, tmp.length, paddingBytes.length);
            } else System.arraycopy(tmp, 0, IVBytes, 0, tmp.length);
        }
        Thread t1 = new Thread(() -> {
            Log.i("", ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Mode: " + cipherMode + " <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
            byte[] pwdBytes = null;
            if (auth.length == 0 || cipherMode == 1) {
                pwdBytes = pwd.getBytes(StandardCharsets.UTF_8);
            }
            com.cdevworks.filescryptpro.CryptionConfigs configs = new com.cdevworks.filescryptpro.CryptionConfigs(cipherMode, pwdBytes, IVBytes, saveLocation, encryptFileNames, encryptFolderNames, deleteSourceFiles, auth);
            ongoingOperationUpdate();
            com.cdevworks.filescryptpro.CryptorEngineInputHandler.makeNewInputs(files, configs);
        });
        t1.start();
    }

    static void getPermissions(Context context, Activity activity) {
        if (SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", context.getPackageName(), null);
                intent.setData(uri);
                activity.startActivity(intent);
            }
        } else
            ActivityCompat.requestPermissions(activity, new String[]{READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE}, PackageManager.PERMISSION_GRANTED);
    }

    static boolean checkPermission(Context context) {
        if (SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            int result = ContextCompat.checkSelfPermission(context, READ_EXTERNAL_STORAGE);
            int result1 = ContextCompat.checkSelfPermission(context, WRITE_EXTERNAL_STORAGE);
            return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED;
        }
    }

    public void showIVToolTip(View view) {
        findViewById(R.id.textView_IV).performLongClick();
        try {
            testFunction();
        } catch (Exception e) {
            e.printStackTrace();
            Log.i("tag1", ">> Test Function Error <<");
        }
    }

    public void cancelOngoingOperation(View view) {
        MainActivity.operationCanceled = true;
        Thread thread = new Thread(() -> {
            com.cdevworks.filescryptpro.CryptorEngineHandler.processQueue.clear();
            com.cdevworks.filescryptpro.CryptorEngineHandler.folderNameCryptionQueue.clear();
            com.cdevworks.filescryptpro.CryptorEngineHandler.rootFolders.clear();
            com.cdevworks.filescryptpro.CryptorEngineHandler.cancelOperation();
            com.cdevworks.filescryptpro.BufferPool.createBuffers(com.cdevworks.filescryptpro.CryptorEngineHandler.bufferCount - com.cdevworks.filescryptpro.BufferPool.bufferPool.size());
        });
        thread.start();
    }

    public void retryOperation(int mode) {
        //noinspection ResultOfMethodCallIgnored
        reportFile.delete();
        com.cdevworks.filescryptpro.FileOperationLog.totalFiles = 0;
        com.cdevworks.filescryptpro.FileOperationLog.refresh();
        com.cdevworks.filescryptpro.FileOperationLog.newReport();
        pSubmitMode = 0;
        Thread thread = new Thread(() -> {
            com.cdevworks.filescryptpro.FileOperationLog.totalFiles = 0;
            com.cdevworks.filescryptpro.FileOperationLog.refresh();
            com.cdevworks.filescryptpro.FileOperationLog.totalFiles = cryptorEngineRetryInputs.size();
            long totalBytes = 0L;
            for (com.cdevworks.filescryptpro.CryptorEngineInput cryptorEngineInput : cryptorEngineRetryInputs) {
                totalBytes += cryptorEngineInput.fileSizeInBytes;

                // mode is 1 when password is from pSubmitBtn or 2 when Password manager is used.
                if (mode == 1) {
                    cryptorEngineInput.pwd = pwd.getBytes(StandardCharsets.UTF_8);
                    if (cryptorEngineInput.cipherMode == 1) {
                        cryptorEngineInput.usePasswordManagerToggle = auth;

                    } else {
                        cryptorEngineInput.usePasswordManagerToggle = new char[0];
                    }
                } else {
                    //this block only when cipherMode == 2;
                    cryptorEngineInput.usePasswordManagerToggle = auth;
                }
                if (cryptorEngineInput.cipherMode == 1)
                    com.cdevworks.filescryptpro.FileOperationLog.filesToEncrypt++;
                else com.cdevworks.filescryptpro.FileOperationLog.filesToDecrypt++;
            }

            for (com.cdevworks.filescryptpro.CryptorEngineInput cryptorEngineInput : cryptorEngineDirRetryInputs) {
                // mode is 1 when password is from pSubmitBtn or 2 when Password manager is used.
                if (mode == 1) {
                    cryptorEngineInput.pwd = pwd.getBytes(StandardCharsets.UTF_8);
                    if (cryptorEngineInput.cipherMode == 1) {
                        cryptorEngineInput.usePasswordManagerToggle = auth;

                    } else {
                        cryptorEngineInput.usePasswordManagerToggle = new char[0];
                    }
                } else {
                    //this block only when cipherMode == 2;
                    cryptorEngineInput.usePasswordManagerToggle = auth;

                }
            }
            com.cdevworks.filescryptpro.FileOperationLog.totalSizeInMB += totalBytes / 1048576L;
            ongoingOperationUpdate();
            //noinspection ResultOfMethodCallIgnored
            reportFile.delete();
            com.cdevworks.filescryptpro.CryptorEngineHandler.processQueue.addAll(cryptorEngineRetryInputs);
            com.cdevworks.filescryptpro.CryptorEngineHandler.folderNameCryptionQueue.addAll(cryptorEngineDirRetryInputs);
            com.cdevworks.filescryptpro.CryptorEngine.cryptorEngineHandler.notifyAddingInputs();
        });
        thread.start();
    }

    public void updateAppSettings() {
        encryptFilenamesChkBox.setChecked(sharedPreferences.getBoolean("FSControls2", false));
        encryptFolderNamesChkBox.setChecked(sharedPreferences.getBoolean("FSControls3", false));
        String editTextValue = sharedPreferences.getString("FSControls1", "None");
        switch (editTextValue) {
            case "Delete upon successful encryption":
                deleteSourceFilesChkBox.setChecked(true);
                deleteSourceFilesChkBox2.setChecked(false);
                break;
            case "Delete upon successful decryption":
                deleteSourceFilesChkBox2.setChecked(true);
                deleteSourceFilesChkBox.setChecked(false);
                break;
            case "Both":
                deleteSourceFilesChkBox.setChecked(true);
                deleteSourceFilesChkBox2.setChecked(true);
                break;
            default:
                deleteSourceFilesChkBox.setChecked(false);
                deleteSourceFilesChkBox2.setChecked(false);
                break;
        }
    }

    public void showPasswordDialogBox() {
        pDialog.setView(password_inputView);
        Objects.requireNonNull(pDialog.getWindow()).setBackgroundDrawableResource(R.drawable.dialog_border);
        TextView strInfo = password_inputView.findViewById(R.id.info_textView);
        ImageButton pDialogClose = password_inputView.findViewById(R.id.pDialogClose);
        pDialogClose.setOnClickListener(v -> pDialog.dismiss());
        saveToPMChkBox.setChecked(sharedPreferences.getBoolean("PasswordManager", false));
        pwdField.setText("");
        if (cipherMode == 1) {
            pDialogEncOp.setVisibility(View.VISIBLE);
            pDialogDecOp.setVisibility(View.GONE);
            strInfo.setText(getString(R.string.str_info, "encrypting"));
        } else {
            pDialogEncOp.setVisibility(View.GONE);
            pDialogDecOp.setVisibility(View.VISIBLE);
            strInfo.setText(getString(R.string.str_info, "decrypting"));
        }
        pDialog.show();
    }

    public void showSaveLocationDialogBox() {
        TextView saveLocInfoText = save_locationView.findViewById(R.id.textView9);
        if (cipherMode == 1) {
            btnAppDir.setText(getString(R.string.encrypted_files_directory));
            saveLocInfoText.setText(getString(R.string.select_destination));
        } else {
            btnAppDir.setText(getString(R.string.decrypted_files_directory));
            saveLocInfoText.setText(getString(R.string.select_destination2));
        }
        Button BtnSas = save_locationView.findViewById(R.id.btnSAS);
        if (MainActivity.disableSameAsSourceBtn) BtnSas.setVisibility(View.GONE);
        else BtnSas.setVisibility(View.VISIBLE);
        sDialog.setView(save_locationView);
        Objects.requireNonNull(sDialog.getWindow()).setBackgroundDrawableResource(R.drawable.dialog_border);
        sDialog.show();
    }

    public void showLog(View view) {
        hideCardBackgroundDimmer();
        Intent intent = new Intent(this, com.cdevworks.filescryptpro.logViewer.class);
        activityResultLauncher2.launch(intent);
    }

    public void onBtnSAS(View view) {
        saveLocation = "SameAsSource";
        sDialog.dismiss();
        startOperation(files);
    }

    public void onBtnAppDir(View view) {
        File file;
        try {
            if (cipherMode == 1) {
                file = new File(externalAppDir.getPath() + File.separator + "Encrypted Files");
            } else {
                file = new File(externalAppDir.getPath() + File.separator + "Decrypted Files");
            }
            if (!file.exists()) {
                Files.createDirectories(file.toPath());
            }
            saveLocation = file.getPath();
            sDialog.dismiss();
            startOperation(files);
        } catch (Exception ignored) {
        }
    }

    public void onBtnCAF(View view) {
        com.cdevworks.filescryptpro.CSFileChooser.itemPickMode = 1;
        com.cdevworks.filescryptpro.CSFileChooser.chosenFiles.clear();
        Intent intent;
        intent = new Intent(this, com.cdevworks.filescryptpro.CSFileChooser.class);
        sDialog.dismiss();
        activityResultLauncher.launch(intent);
    }

    public void onBtnRetrievePW(View view) {
        BiometricManager biometricManager = BiometricManager.from(this);
        if (biometricManager.canAuthenticate(DEVICE_CREDENTIAL | BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS) {
            Executor executor = ContextCompat.getMainExecutor(this);
            BiometricPrompt biometricPrompt = new BiometricPrompt(MainActivity.this, executor, new BiometricPrompt.AuthenticationCallback() {

                @Override
                public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                    super.onAuthenticationSucceeded(result);
                    Toast.makeText(getApplicationContext(), "Authentication succeeded!", Toast.LENGTH_SHORT).show();
                    auth = com.cdevworks.filescryptpro.PasswordManager.getAuthArray();
                    pDialog.dismiss();
                    if (pSubmitMode == 0) showSaveLocationDialogBox();
                    else retryOperation(2);
                }

                @Override
                public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                    super.onAuthenticationError(errorCode, errString);
                    Toast.makeText(getApplicationContext(), "Authentication error: " + errString, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onAuthenticationFailed() {
                    super.onAuthenticationFailed();
                    Toast.makeText(getApplicationContext(), "Authentication failed", Toast.LENGTH_SHORT).show();
                }
            });

            BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder().setTitle("Authenticate to Decrypt").setSubtitle("FilesCrypt Pro").setAllowedAuthenticators(DEVICE_CREDENTIAL | BIOMETRIC_WEAK).build();

            biometricPrompt.authenticate(promptInfo);
        } else {
            showMsgBox(getString(R.string.msg_info));
        }
    }

    public void showCardBackgroundDimmer() {
        if (cardBackgroundDimmer) return;
        cardBackgroundDimmer = true;
        Animation fadeInAnimation = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        dimOverlay.setVisibility(View.VISIBLE);
        dimOverlay.startAnimation(fadeInAnimation);
        disableClicksForViewsBelow(dimOverlay);
    }

    public void hideCardBackgroundDimmer() {
        if (!cardBackgroundDimmer) return;
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

    public void hideCardBackgroundDimmer(View view) {
        hideCardBackgroundDimmer();
    }

    public void hapticFeedback(View view) {
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
    }

    public void showMsgBox(String msg) {
        msgDialog.setView(msg_box);
        Objects.requireNonNull(msgDialog.getWindow()).setBackgroundDrawableResource(R.drawable.dialog_border);
        ((TextView) msg_box.findViewById(R.id.msg_box_textView)).setText(msg);
        msg_box.findViewById(R.id.msgBoxCloseBtn).setOnClickListener(v -> msgDialog.dismiss());
        msgDialog.show();
    }

    public void updateSharedPreference(String key, Object val) {
        SharedPreferences sharedPreferences = getSharedPreferences("FilesCryptPro_Prefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (key.equals("adval")) {
            editor.putInt(key, (Integer) val);
        } else if (key.equals("advis")) {
            editor.putBoolean(key, (Boolean) val);
        }
        editor.apply();
    }

    public void testFunction() {
    }
}