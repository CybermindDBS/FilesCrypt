package com.cdevworks.filescryptpro;

import static com.cdevworks.filescryptpro.CSFileChooser.chosenFiles;
import static com.cdevworks.filescryptpro.CSFileChooser.itemPickMode;
import static com.cdevworks.filescryptpro.CSFileChooser.path;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Size;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;

public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ViewHolder> {
    Context context;
    ImageView selectorBtn;
    SearchView searchView;
    TextView info1, info2;
    Button pickSelected;
    String[] filenames;
    CSFileChooser csFileChooser;
    static boolean flagVal = true;
    static int flagValRecords = 0;
    static HashMap<String, Bitmap> bitmapThumbnails = new HashMap<>();
    static String itemPickMode3FileExtension = ".bks";
    Activity activity;

    public RecyclerAdapter(Context context, String[] filenames, CSFileChooser csFileChooser) {
        this.filenames = filenames;
        this.context = context;
        this.csFileChooser = csFileChooser;
        activity = (Activity) context;
        selectorBtn = activity.findViewById(R.id.selectAllBtn);
        searchView = activity.findViewById(R.id.searchView);
        pickSelected = activity.findViewById(R.id.pickSelected);
        info1 = activity.findViewById(R.id.infoText1);
        info2 = activity.findViewById(R.id.infoText2);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater.inflate(R.layout.item_design, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint({"SetTextI18n"})
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        //sets Filename and icon for each items
        File fileItem = new File(path + File.separator + filenames[position]);
        if (flagVal) {
            flagValRecords = filenames.length;
            holder.fileSelector.setVisibility(View.GONE);
            holder.fileSelector.setEnabled(false);
            pickSelected.setVisibility(View.INVISIBLE);
            pickSelected.setEnabled(false);
            if ((File.separator + filenames[position]).equals(Environment.getExternalStorageDirectory().getPath())) {
                holder.filename.setVisibility(View.GONE);
                holder.itemDetails.setVisibility(View.GONE);
                holder.itemDetails.setVisibility(View.GONE);
                holder.filename2.setVisibility(View.VISIBLE);
                holder.filename2.setText(context.getString(R.string.str_filename2));
                holder.fileThumbnail.setImageResource(R.drawable.folder_home);
            } else {
                holder.fileThumbnail.setImageResource(R.drawable.micro_sd);
                holder.itemDetails.setText(context.getString(R.string.str_itemDetails, "External Storage"));
            }
            holder.filename.setText("/" + filenames[position]);
        } else {
            if (flagValRecords > 0) {
                flagValRecords--;
                holder.filename2.setVisibility(View.GONE);
                holder.filename.setVisibility(View.VISIBLE);
                holder.fileSelector.setVisibility(View.VISIBLE);
                holder.fileSelector.setEnabled(true);
                holder.itemDetails.setVisibility(View.VISIBLE);
            }
            pickSelected.setVisibility(View.VISIBLE);
            pickSelected.setEnabled(true);
            holder.filename.setText(filenames[position]);
            if (fileItem.isDirectory()) {
                try {
                    holder.itemDetails.setText(context.getString(R.string.str_itemDetails, Objects.requireNonNull(fileItem.list()).length + " items"));
                } catch (Exception ignored) {
                }
            } else
                holder.itemDetails.setText(context.getString(R.string.str_itemDetails, com.cdevworks.filescryptpro.Utils.convertFileSize(fileItem.length())));
        }
        if (fileItem.isFile()) {
            if (CSFileChooser.itemPickMode == 1) {
                holder.fileSelector.setEnabled(false);
                holder.fileSelector.setVisibility(View.INVISIBLE);
            } else if (CSFileChooser.itemPickMode == 0) {
                holder.fileSelector.setEnabled(true);
                holder.fileSelector.setVisibility(View.VISIBLE);
            } else if (CSFileChooser.itemPickMode == 2) {
                String fileName = fileItem.getName();
                if (fileName.contains(".") && fileName.substring(fileName.lastIndexOf(".")).equals(itemPickMode3FileExtension)) {
                    holder.fileSelector.setEnabled(true);
                    holder.fileSelector.setVisibility(View.VISIBLE);
                } else {
                    holder.fileSelector.setEnabled(false);
                    holder.fileSelector.setVisibility(View.INVISIBLE);
                }
            }
            String extension = getFileExtension(fileItem.getName());
            setFileThumbnail(holder.fileThumbnail, Objects.requireNonNullElse(extension, ""), fileItem);
        } else {
            if (CSFileChooser.itemPickMode == 1) {
                if (!flagVal) {
                    holder.fileSelector.setEnabled(true);
                    holder.fileSelector.setVisibility(View.VISIBLE);
                }
            } else if (itemPickMode == 2) {
                holder.fileSelector.setEnabled(false);
                holder.fileSelector.setVisibility(View.INVISIBLE);
            }
            if (fileItem.getName().contains("Encrypted-FEP") || fileItem.getName().contains(CryptorEngine.dirExtension))
                holder.fileThumbnail.setImageResource(R.drawable.folder_lock);
            else {
                if (!flagVal) holder.fileThumbnail.setImageResource(R.drawable.folder);
            }
        }

        holder.filename.setOnClickListener(v -> {
            //Logic for folder traversal by clicking on it
            String tmp;
            tmp = path + File.separator + filenames[position];
            if (flagVal) CSFileChooser.selectedStorage = tmp;
            File file = new File(tmp);
            if (file.isDirectory()) {
                if (!holder.fileSelector.isChecked()) {
                    if (checkRWPermission(tmp)) {
                        path = tmp;
                        traverseView(file.list());
                    }
                } else {
                    holder.fileSelector.setChecked(false);
                    CSFileChooser.chosenFiles.remove(path + File.separator + holder.filename.getText());
                    holder.fileSelector.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                    holder.card.setCardBackgroundColor(context.getColor(R.color.background_dark));
                    selectorBtn.setImageResource(R.drawable.select_all);
                    CSFileChooser.itemSelectMode = 0;
                    updatePickButtonInfo();
                }
            } else {
                holder.filename.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                if (holder.fileSelector.isChecked()) {
                    holder.fileSelector.setChecked(false);
                    CSFileChooser.chosenFiles.remove(path + File.separator + holder.filename.getText());
                    holder.card.setCardBackgroundColor(context.getColor(R.color.background_dark));
                    selectorBtn.setImageResource(R.drawable.select_all);
                    CSFileChooser.itemSelectMode = 0;
                    updatePickButtonInfo();
                } else {
                    if (!flagVal && (CSFileChooser.itemPickMode == 0 || itemPickMode == 2)) {
                        if (itemPickMode == 2) {
                            if (getFileExtension(filenames[position]).equals(itemPickMode3FileExtension)) {
                                holder.fileSelector.setChecked(true);
                                CSFileChooser.chosenFiles.add(path + File.separator + holder.filename.getText());
                                holder.card.setCardBackgroundColor(context.getColor(R.color.background_light));
                                updatePickButtonInfo();
                            }
                        } else {
                            holder.fileSelector.setChecked(true);
                            CSFileChooser.chosenFiles.add(path + File.separator + holder.filename.getText());
                            holder.card.setCardBackgroundColor(context.getColor(R.color.background_light));
                            updatePickButtonInfo();
                        }
                    }
                }
            }
        });

        holder.filename.setOnLongClickListener(v -> {
            //Logic for folder traversal by clicking on it
            String tmp;
            tmp = path + File.separator + filenames[position];
            if (flagVal) CSFileChooser.selectedStorage = tmp;
            File file = new File(tmp);
            if (file.isDirectory()) {
                if (!holder.fileSelector.isChecked()) {
                    if (CSFileChooser.itemPickMode == 1) {
                        chosenFiles.clear();
                        updateView();
                    }
                    if (CSFileChooser.itemPickMode != 2) {
                        holder.fileSelector.setChecked(true);
                        CSFileChooser.chosenFiles.add(path + File.separator + holder.filename.getText());
                        holder.fileSelector.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                        holder.card.setCardBackgroundColor(context.getColor(R.color.background_light));
                        updatePickButtonInfo();
                    }
                } else {
                    holder.fileSelector.setChecked(false);
                    CSFileChooser.chosenFiles.remove(path + File.separator + holder.filename.getText());
                    holder.fileSelector.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                    holder.card.setCardBackgroundColor(context.getColor(R.color.background_dark));
                    selectorBtn.setImageResource(R.drawable.select_all);
                    CSFileChooser.itemSelectMode = 0;
                    updatePickButtonInfo();
                }
            } else {
                holder.filename.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                if (holder.fileSelector.isChecked()) {
                    holder.fileSelector.setChecked(false);
                    CSFileChooser.chosenFiles.remove(path + File.separator + holder.filename.getText());
                    holder.card.setCardBackgroundColor(context.getColor(R.color.background_dark));
                    selectorBtn.setImageResource(R.drawable.select_all);
                    CSFileChooser.itemSelectMode = 0;
                    updatePickButtonInfo();
                } else {
                    if (!flagVal && (CSFileChooser.itemPickMode == 0 || itemPickMode == 2)) {
                        if (itemPickMode == 2) {
                            if (getFileExtension(filenames[position]).equals(itemPickMode3FileExtension)) {
                                holder.fileSelector.setChecked(true);
                                CSFileChooser.chosenFiles.add(path + File.separator + holder.filename.getText());
                                holder.card.setCardBackgroundColor(context.getColor(R.color.background_light));
                                updatePickButtonInfo();
                            }
                        } else {
                            holder.fileSelector.setChecked(true);
                            CSFileChooser.chosenFiles.add(path + File.separator + holder.filename.getText());
                            holder.card.setCardBackgroundColor(context.getColor(R.color.background_light));
                            updatePickButtonInfo();
                        }
                    }
                }
            }
            return true;
        });
        holder.itemDetails.setOnLongClickListener(v -> holder.filename.performLongClick());
        holder.fileThumbnail.setOnLongClickListener(v -> holder.filename.performLongClick());

        holder.filename2.setOnClickListener(v -> holder.filename.callOnClick());
        holder.itemDetails.setOnClickListener(v -> holder.filename.callOnClick());
        holder.fileThumbnail.setOnClickListener(v -> holder.filename.callOnClick());

        holder.fileSelector.setOnClickListener(v -> {
            holder.fileSelector.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            if (holder.fileSelector.isChecked()) {
                if (checkRWPermission(path + File.separator + holder.filename.getText())) {
                    if (CSFileChooser.itemPickMode == 1) {
                        chosenFiles.clear();
                        updateView();
                    }
                    CSFileChooser.chosenFiles.add(path + File.separator + holder.filename.getText());
                    holder.card.setCardBackgroundColor(context.getColor(R.color.background_light));
                    updatePickButtonInfo();
                } else holder.fileSelector.setChecked(false);
            } else {
                CSFileChooser.chosenFiles.remove(path + File.separator + holder.filename.getText());
                holder.card.setCardBackgroundColor(context.getColor(R.color.background_dark));
                selectorBtn.setImageResource(R.drawable.select_all);
                CSFileChooser.itemSelectMode = 0;
                updatePickButtonInfo();
            }
        });

        //Keeps the checkbox ticked or un-ticked in previous folder at the time of clicking Back button
        if (CSFileChooser.chosenFiles.contains(path + File.separator + filenames[position])) {
            holder.fileSelector.setChecked(true);
            holder.card.setCardBackgroundColor(context.getColor(R.color.background_light));
        } else {
            holder.fileSelector.setChecked(false);
            holder.card.setCardBackgroundColor(context.getColor(R.color.background_dark));
        }

    }

    @Override
    public int getItemCount() {
        return filenames.length;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateView() {
        if (CSFileChooser.filenames.length == 0) info2.setVisibility(View.VISIBLE);
        else info2.setVisibility(View.GONE);
        notifyDataSetChanged();
    }

    public void itemSelector(boolean selectAll) {
        int count = getItemCount();
        for (int i = 0; i < count; i++) {
            if (selectAll) chosenFiles.add(path + File.separator + filenames[i]);
            else chosenFiles.remove(path + File.separator + filenames[i]);
        }
        updateView();
    }

    protected void traverseView(String[] updatedFiles) {
        CSFileChooser.backButtonCounter = 0;
        CSFileChooser.itemSelectMode = 0;
        if (flagVal) {
            flagVal = false;
            selectorBtn.setVisibility(View.VISIBLE);
            selectorBtn.setEnabled(true);
            searchView.setVisibility(View.VISIBLE);
            info1.setVisibility(View.GONE);
        }
        if (itemPickMode == 1 || itemPickMode == 2) selectorBtn.setVisibility(View.GONE);
        else selectorBtn.setVisibility(View.VISIBLE);
        CSFileChooser.filenames = filenames = CSFileChooser.getSortedFilesList(updatedFiles);
        TextView currentPath = activity.findViewById(R.id.current_path);
        currentPath.setText(context.getString(R.string.str_path, path));
        RecyclerView recyclerView = activity.findViewById(R.id.listView);
        recyclerView.setItemViewCacheSize(filenames.length);
        updateView();
    }

    protected boolean checkRWPermission(String path) {
        File tmpFile = new File(path);
        if (!(tmpFile.canRead() && (CSFileChooser.itemPickMode == 0 || CSFileChooser.itemPickMode == 2) || tmpFile.canWrite())) {
            if (itemPickMode == 0 || CSFileChooser.itemPickMode == 2)
                CEUtils.showMessage("Read access denied.", 1);
            else
                CEUtils.showMessage("This application does not currently support the ability to write to internal SD cards.", 1);
            return false;
        }
        return true;
    }

    public String getFileExtension(String name) {
        StringBuilder sb = new StringBuilder(name);
        String str;
        try {
            str = sb.substring(sb.lastIndexOf("."), sb.length());
        } catch (Exception ignored) {
            return "null";
        }
        return str.toLowerCase();
    }

    private void setFileThumbnail(ImageView fileThumbnail, String extension, File file) {
        Integer val = CSFileChooser.fileIcons.get(extension);
        if (val != null) {
            fileThumbnail.setImageResource(val);
            if (val == R.drawable.file_image) {
                createImageThumbNail(fileThumbnail, file.getPath(), file);
            } else if (val == R.drawable.file_video) {
                createVideoThumbNail(fileThumbnail, file.getPath());
            }
        } else {
            fileThumbnail.setImageResource(R.drawable.file);

        }
    }

    public void createVideoThumbNail(ImageView fileThumbnail, String path) {
        CSFileChooser.executorService.execute(() -> {
            Bitmap bitmap;
            if (!bitmapThumbnails.containsKey(path)) {
                bitmapThumbnails.put(path, null);
                bitmap = ThumbnailUtils.createVideoThumbnail(path, MediaStore.Video.Thumbnails.MICRO_KIND);
                bitmapThumbnails.put(path, bitmap);
            } else {
                if (bitmapThumbnails.get(path) == null) return;
            }
            if (Objects.requireNonNull(new File(path).getParentFile()).getPath().equals(CSFileChooser.path))
                activity.runOnUiThread(() -> fileThumbnail.setImageBitmap(bitmapThumbnails.get(path)));
        });
    }

    public void createImageThumbNail(ImageView fileThumbnail, String path, File filePath) {
        Activity activity = (Activity) context;
        CSFileChooser.executorService.execute(() -> {
            Bitmap bitmap;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    bitmap = ThumbnailUtils.createImageThumbnail(filePath, new Size(150, 150), null);
                } catch (IOException e) {
                    bitmap = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(path), 150, 150);
                }
            } else
                bitmap = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(path), 150, 150);
            Bitmap finalBitmap = bitmap;
            if (finalBitmap != null && Objects.requireNonNull(filePath.getParentFile()).getPath().equals(CSFileChooser.path))
                activity.runOnUiThread(() -> fileThumbnail.setImageBitmap(finalBitmap));
        });
    }

    public void updatePickButtonInfo() {
        if (chosenFiles.isEmpty()) pickSelected.setText(context.getString(R.string.pickSelected));
        else
            pickSelected.setText(context.getString(R.string.pickSelectionInfo, chosenFiles.size() + ""));
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView filename, filename2, itemDetails;
        ImageView fileThumbnail;
        CheckBox fileSelector;
        CardView card;
        ConstraintLayout constraintLayout;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            filename = itemView.findViewById(R.id.filename);
            filename2 = itemView.findViewById(R.id.filename2);
            itemDetails = itemView.findViewById(R.id.itemDetails);
            fileThumbnail = itemView.findViewById(R.id.fileThumbnail);
            fileSelector = itemView.findViewById(R.id.fileSelector);
            constraintLayout = itemView.findViewById(R.id.constraintLayout);
            card = itemView.findViewById(R.id.card);
        }
    }
}