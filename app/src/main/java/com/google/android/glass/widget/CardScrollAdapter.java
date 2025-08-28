package com.google.android.glass.widget;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public abstract class CardScrollAdapter extends BaseAdapter {
    public abstract int getCount();

    public abstract Object getItem(int i);

    public abstract int getPosition(Object obj);

    public abstract View getView(int i, View view, ViewGroup viewGroup);

    public int getItemViewType(int position) {
        return -1;
    }

    public int getViewTypeCount() {
        return 0;
    }

    public long getItemId(int position) {
        return (long) position;
    }

    public int getHomePosition() {
        return 0;
    }

    @Deprecated
    public void recycleView(View view) {
    }
}
