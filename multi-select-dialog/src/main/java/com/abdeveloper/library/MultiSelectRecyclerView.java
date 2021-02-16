package com.abdeveloper.library;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

public class MultiSelectRecyclerView extends RecyclerView {

    @SuppressWarnings("ConstantConditions")
    private final AdapterDataObserver mAdapterDataObserver = new AdapterDataObserver() {
        @Override
        public void onChanged() {
            updateEmptyStatus(getAdapter().getItemCount() == 0);
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            updateEmptyStatus(getAdapter().getItemCount() == 0);
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            updateEmptyStatus(getAdapter().getItemCount() == 0);
        }
    };

    @Nullable
    private View mEmptyView;

    public MultiSelectRecyclerView(@NonNull Context context) {
        super(context);
    }

    public MultiSelectRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public MultiSelectRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void setAdapter(@Nullable Adapter adapter) {
        Adapter oldAdapter = getAdapter();
        if (oldAdapter != null) {
            oldAdapter.unregisterAdapterDataObserver(mAdapterDataObserver);
        }
        super.setAdapter(adapter);
        if (adapter != null) {
            adapter.registerAdapterDataObserver(mAdapterDataObserver);
        }
    }

    @Nullable
    public View getEmptyView() {
        return mEmptyView;
    }

    public void setEmptyView(@Nullable View view) {
        mEmptyView = view;
        Adapter adapter = getAdapter();
        boolean empty = (adapter == null) || (adapter.getItemCount() == 0);
        updateEmptyStatus(empty);
    }

    public void updateEmptyStatus(boolean empty) {
        if (empty) {
            if (mEmptyView != null) {
                mEmptyView.setVisibility(View.VISIBLE);
                setVisibility(View.INVISIBLE);
            } else {
                setVisibility(View.VISIBLE);
            }
        } else {
            if (mEmptyView != null) {
                mEmptyView.setVisibility(View.GONE);
            }
            setVisibility(View.VISIBLE);
        }
    }
}
