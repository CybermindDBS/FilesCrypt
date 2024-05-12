package com.cdevworks.filescryptpro;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/*
   CSFileChooser Operating modes:
   set itemPickMode before creating intent to choose operating mode.
   1. itemPickMode = 0, for multi-select files and folders.
   2. itemPickMode = 1, for allowing to select only one folder and also checks write access while traversing through folders.
   3. itemPickMode = 2, for multi-select .bks files only.
 */

public class CSFileChooser extends AppCompatActivity {
    RecyclerView recyclerView;
    com.cdevworks.filescryptpro.RecyclerAdapter adapter;
    Context context;
    TextView currentPath, info1;
    SearchView searchView;
    ImageView backBtn, selectorBtn;
    Button pickSelected;
    static int backButtonCounter;
    static int itemPickMode = 0;
    static int itemSelectMode = 0;
    static String[] filenames, storageDirectories;
    static String path, selectedStorage;
    static boolean filePickerUIRunning;
    static LinkedHashSet<String> chosenFiles = new LinkedHashSet<>();
    static HashMap<String, Integer> fileIcons = new HashMap<>();
    static ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.file_chooser);

        //Initialization:-
        filePickerUIRunning = true;
        backButtonCounter = 0;
        context = this;
        currentPath = findViewById(R.id.current_path);
        info1 = findViewById(R.id.infoText1);
        selectorBtn = findViewById(R.id.selectAllBtn);
        backBtn = findViewById(R.id.backBtn);
        pickSelected = findViewById(R.id.pickSelected);
        searchView = findViewById(R.id.searchView);
        searchView.clearFocus();
        MainActivity.disableSameAsSourceBtn = false;

        //keeps the fileChooser unchanged while changing phone orientation (since onCreate method will be called when changing phone orientation), path and filenames will be set to null only at the time of finish().
        if (filenames == null)
            filenames = storageDirectories = getSortedFilesList(com.cdevworks.filescryptpro.StorageUtil.getStorageDirectories(this));
        if (path == null || path.equals("")) {
            path = "";
            currentPath.setText(getString(R.string.str_info1, String.valueOf(filenames.length - 1)));
            selectorBtn.setVisibility(View.GONE);
            selectorBtn.setEnabled(false);
            searchView.setVisibility(View.GONE);
            info1.setVisibility(View.VISIBLE);
        } else currentPath.setText(getString(R.string.str_path, path));

        executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 3);
        setIcons();
        //Initialize RecyclerView and show a list of files
        recyclerView = findViewById(R.id.listView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setItemViewCacheSize(filenames.length);
        adapter = new com.cdevworks.filescryptpro.RecyclerAdapter(context, filenames, this);
        recyclerView.setAdapter(adapter);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filenames = new File(path).list();
                itemSelectMode = 0;
                selectorBtn.setImageResource(R.drawable.select_all);
                formatList(newText);
                return true;
            }
        });

        AdView mAdView = findViewById(R.id.adView3);
        if (getSharedPreferences("FilesCryptPro_Prefs", Context.MODE_PRIVATE).getBoolean("advis", true)) {
            mAdView.setVisibility(View.VISIBLE);
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);
        } else {
            mAdView.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        filePickerUIRunning = false;
    }

    protected static String[] getSortedFilesList(String[] unSortedFiles) {
        String[] sortedFiles = new String[unSortedFiles.length];
        ArrayList<String> tmpList = new ArrayList<>();
        ArrayList<String> tmpList2 = new ArrayList<>();
        File tmp;
        for (String f : unSortedFiles) {
            tmp = new File(path + File.separator + f);
            if (tmp.isDirectory()) {
                tmpList.add(f);
            } else {
                tmpList2.add(f);
            }
        }
        Collections.sort(tmpList);
        Collections.sort(tmpList2);
        ArrayList<String> tmpList3 = new ArrayList<>();
        tmpList3.addAll(tmpList);
        tmpList3.addAll(tmpList2);
        return tmpList3.toArray(sortedFiles);
    }

    //for searchView purpose.
    private void formatList(String text) {
        ArrayList<String> filteredList = new ArrayList<>();
        if (filenames == null) return;
        for (String file : filenames) {
            if (file.toLowerCase().contains(text.toLowerCase())) {
                filteredList.add(file);
            }
        }
        if (filteredList.size() > 0) {
            filenames = adapter.filenames = getSortedFilesList(filteredList.toArray(new String[0]));
            adapter.updateView();
        }
    }

    //this method is called when the user presses back button in their phone.
    @Override
    public void onBackPressed() {
        try {
            if (!searchView.isIconified()) {
                adapter.updateView();
                searchView.setIconified(true);
                searchView.setIconified(true);
                searchView.clearFocus();
                itemSelectMode = 0;
                selectorBtn.setImageResource(R.drawable.select_all);
            } else {
                if (path.equals(selectedStorage)) {
                    path = "";
                    adapter.filenames = filenames = getSortedFilesList(com.cdevworks.filescryptpro.StorageUtil.getStorageDirectories(this));
                    currentPath.setText(getString(R.string.str_info1, String.valueOf(filenames.length - 1)));
                    com.cdevworks.filescryptpro.RecyclerAdapter.flagVal = true;
                    selectorBtn.setVisibility(View.GONE);
                    selectorBtn.setEnabled(false);
                    searchView.setVisibility(View.GONE);
                    info1.setVisibility(View.VISIBLE);
                    adapter.updateView();
                } else if (!path.isEmpty()) {
                    itemSelectMode = 0;
                    selectorBtn.setImageResource(R.drawable.select_all);
                    File f = new File(path);
                    File f2 = f.getParentFile();
                    if (f2 != null) {
                        path = f2.getPath();
                    }
                    if (f2 != null) {
                        filenames = f2.list();
                    }
                    currentPath.setText(getString(R.string.str_path, path));
                    adapter.traverseView(getSortedFilesList(filenames));
                } else {
                    if (backButtonCounter++ != 1)
                        Toast.makeText(context, "Back again to close.", Toast.LENGTH_SHORT).show();
                    else {
                        com.cdevworks.filescryptpro.RecyclerAdapter.flagVal = true;
                        itemSelectMode = itemPickMode = 0;
                        filenames = null;
                        path = null;
                        executorService.shutdown();
                        com.cdevworks.filescryptpro.RecyclerAdapter.bitmapThumbnails.clear();
                        chosenFiles.clear();
                        finish();
                    }
                }
            }
        } catch (Exception ignored) {
            this.finish();
        }
    }

    public void onBackBtn(View view) {
        backBtn.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        onBackPressed();
    }

    // When Pick selected button is pressed: return the chosen files details to the calling activity
    public void onPickBtn(View view) {
        if (chosenFiles.size() > 0) {
            ArrayList<String> submitFiles = new ArrayList<>(chosenFiles);
            ArrayList<String> unsupportedSameAsSourceStorages = new ArrayList<>();
            for (String storage : storageDirectories) {
                if (!checkWPermission(storage)) unsupportedSameAsSourceStorages.add(storage);
            }
            for (String filePath : chosenFiles) {
                if (unsupportedSameAsSourceStorages.stream().anyMatch(filePath::contains)) {
                    MainActivity.disableSameAsSourceBtn = true;
                    Log.i("tag1", "onPickBtn: found, " + filePath);
                    break;
                }
            }
            Intent intent = new Intent();
            intent.putExtra("result", submitFiles);
            if (itemPickMode == 0) setResult(36, intent);
            else if (itemPickMode == 1) setResult(63, intent);
            else if (itemPickMode == 2) {
                setResult(93, intent);
            }
            com.cdevworks.filescryptpro.RecyclerAdapter.flagVal = true;
            executorService.shutdown();
            itemSelectMode = itemPickMode = 0;
            filenames = null;
            path = null;
            com.cdevworks.filescryptpro.RecyclerAdapter.flagVal = true;
            com.cdevworks.filescryptpro.RecyclerAdapter.bitmapThumbnails.clear();
            chosenFiles.clear();
            finish();
        } else Toast.makeText(context, "Pick at-least one item.", Toast.LENGTH_SHORT).show();
    }

    public void onSelectAllBtn(View view) {
        selectorBtn.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        if (itemSelectMode == 0) {
            itemSelectMode = 1;
            selectorBtn.setImageResource(R.drawable.select_inverse);
        } else if (itemSelectMode == 1) {
            itemSelectMode = 2;
            selectorBtn.setImageResource(R.drawable.select_all);
        } else if (itemSelectMode == 2) {
            itemSelectMode = 1;
            selectorBtn.setImageResource(R.drawable.select_inverse);
        }
        if (itemSelectMode == 1) {
            {
                adapter.itemSelector(true);
            }
        } else if (itemSelectMode == 2) {
            adapter.itemSelector(false);
        }
        if (chosenFiles.isEmpty()) pickSelected.setText(context.getString(R.string.pickSelected));
        else
            pickSelected.setText(context.getString(R.string.pickSelectionInfo, chosenFiles.size() + ""));
    }

    protected static boolean checkWPermission(String path) {
        File tmpFile = new File(path);
        return tmpFile.canWrite();
    }

    @SuppressWarnings("unused")
    public void getRWPermission(File directory) {
        // Choose a directory using the system's file picker.
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);

        // Optionally, specify a URI for the directory that should be opened in
        // the system file picker when it loads.
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.fromFile(directory));
        getAccessWithSAFLauncher.launch(intent);
    }

    ActivityResultLauncher<Intent> getAccessWithSAFLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == Activity.RESULT_OK) {
            Intent resultData = result.getData();
            if (resultData != null) {
                Uri uri = resultData.getData();
                // Use this uri to further access that folder.
                Log.i("TAG", ">>>>>>>>>>>>>>> onActivityResult: " + uri);
            }
        }
    });

    protected static void setIcons() {
        fileIcons.put(".mp4", R.drawable.file_video);
        fileIcons.put(".mov", R.drawable.file_video);
        fileIcons.put(".wmv", R.drawable.file_video);
        fileIcons.put(".avi", R.drawable.file_video);
        fileIcons.put(".mkv", R.drawable.file_video);
        fileIcons.put(".mpeg", R.drawable.file_video);
        fileIcons.put(".mpg", R.drawable.file_video);

        fileIcons.put(".jpg", R.drawable.file_image);
        fileIcons.put(".png", R.drawable.file_image);
        fileIcons.put(".dng", R.drawable.file_image);
        fileIcons.put(".bmp", R.drawable.file_image);
        fileIcons.put(".jpeg", R.drawable.file_image);
        fileIcons.put(".gif", R.drawable.file_image);
        fileIcons.put(".tiff", R.drawable.file_image);
        fileIcons.put(".svg", R.drawable.file_image);
        fileIcons.put(".webp", R.drawable.file_image);

        fileIcons.put(".mp3", R.drawable.file_music);
        fileIcons.put(".aac", R.drawable.file_music);
        fileIcons.put(".ma4", R.drawable.file_music);
        fileIcons.put(".flac", R.drawable.file_music);
        fileIcons.put(".wav", R.drawable.file_music);
        fileIcons.put(".wma", R.drawable.file_music);
        fileIcons.put(".ogg", R.drawable.file_music);
        fileIcons.put(".aiff", R.drawable.file_music);

        fileIcons.put(".pdf", R.drawable.file_document);
        fileIcons.put(".csv", R.drawable.file_document);
        fileIcons.put(".ppt", R.drawable.file_document);
        fileIcons.put(".doc", R.drawable.file_document);
        fileIcons.put(".docx", R.drawable.file_document);
        fileIcons.put(".xls", R.drawable.file_document);
        fileIcons.put(".xlsx", R.drawable.file_document);
        fileIcons.put(".txt", R.drawable.file_document);
        fileIcons.put(".rtf", R.drawable.file_document);

        fileIcons.put(".html", R.drawable.file_code);
        fileIcons.put(".java", R.drawable.file_code);
        fileIcons.put(".py", R.drawable.file_code);
        fileIcons.put(".cpp", R.drawable.file_code);
        fileIcons.put(".c", R.drawable.file_code);
        fileIcons.put(".css", R.drawable.file_code);
        fileIcons.put(".xml", R.drawable.file_code);
        fileIcons.put(".json", R.drawable.file_code);
        fileIcons.put(".js", R.drawable.file_code);
        fileIcons.put(".jsx", R.drawable.file_code);
        fileIcons.put(".php", R.drawable.file_code);
        fileIcons.put(".cs", R.drawable.file_code);
        fileIcons.put(".kt", R.drawable.file_code);
        fileIcons.put(".asp", R.drawable.file_code);
        fileIcons.put(".aspx", R.drawable.file_code);

        fileIcons.put(".zip", R.drawable.zip_box);
        fileIcons.put(".rar", R.drawable.zip_box);
        fileIcons.put(".tar", R.drawable.zip_box);
        fileIcons.put(".iso", R.drawable.zip_box);
        fileIcons.put(".7z", R.drawable.zip_box);

        fileIcons.put(CryptorEngine.fileExtension, R.drawable.file_lock);
    }
}