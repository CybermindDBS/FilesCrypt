package com.cdevworks.filescryptpro;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MyExpandableListAdapter extends BaseExpandableListAdapter {

    private final Context context;
    private final Map<String, ArrayList<String>> collection;
    private final List<String> groupList;

    public MyExpandableListAdapter(Context context, List<String> groupList, Map<String, List<CryptorEngineInput>> collection) {
        HashMap<String, ArrayList<String>> map = new HashMap<>();
        for (String key : collection.keySet()) {
            if (key.contains("Folder")) {
                groupList.remove(key);
                continue;
            }
            List<CryptorEngineInput> inputs = collection.get(key);
            if (inputs != null) {
                for (CryptorEngineInput cryptorEngineInput : inputs) {
                    if (map.containsKey(key)) {
                        Objects.requireNonNull(map.get(key)).add(cryptorEngineInput.sourceFilePath);
                    } else {
                        map.put(key, new ArrayList<>(Collections.singletonList(cryptorEngineInput.sourceFilePath)));
                    }
                }
            }
        }
        Log.i("tag1", map.toString());
        this.context = context;
        this.collection = map;
        this.groupList = groupList;
    }

    @Override
    public int getGroupCount() {
        return collection.size();
    }

    @Override
    public int getChildrenCount(int i) {
        return Objects.requireNonNull(collection.get(groupList.get(i))).size();
    }

    @Override
    public Object getGroup(int i) {
        return groupList.get(i);
    }

    @Override
    public Object getChild(int i, int i1) {
        return Objects.requireNonNull(collection.get(groupList.get(i))).get(i1);
    }

    @Override
    public long getGroupId(int i) {
        return i;
    }

    @Override
    public long getChildId(int i, int i1) {
        return i1;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getGroupView(int i, boolean b, View view, ViewGroup viewGroup) {
        String mobileName = getGroup(i).toString();
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.group_item, null);
        }
        TextView item = view.findViewById(R.id.group_name);
        item.setTypeface(null, Typeface.BOLD);
        String str = context.getString(R.string.groupName, mobileName, String.valueOf(getChildrenCount(i)));
        if (str.contains("successfully")) item.setTextColor(context.getColor(R.color.foreground));
        else item.setTextColor(context.getColor(R.color.red));
        item.setText(str);
        return view;
    }

    @Override
    public View getChildView(final int i, final int i1, boolean b, View view, ViewGroup viewGroup) {
        String model = getChild(i, i1).toString();
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.child_item, null);
        }
        TextView item = view.findViewById(R.id.model);
        TextView item2 = view.findViewById(R.id.model2);
        File file = new File(model);
        item.setText(context.getString(R.string.fileName, file.getName()));
        item2.setText(context.getString(R.string.pathName, file.getPath()));
        return view;
    }

    @Override
    public boolean isChildSelectable(int i, int i1) {
        return true;
    }
}