package io.errorlab.widget;

import android.content.Context;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.widget.Checkable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class CheckableFloatingActionButton extends FloatingActionButton implements Checkable {

    private static final int[] CheckedStateSet = { android.R.attr.state_checked, };

    private boolean checked;

    public CheckableFloatingActionButton(@NonNull Context ctx) {
        this(ctx, null);
    }

    public CheckableFloatingActionButton(@NonNull Context ctx, @Nullable AttributeSet attrs) {
        this(ctx, attrs, 0);
    }

    public CheckableFloatingActionButton(@NonNull Context ctx, @Nullable AttributeSet attrs, int defStyle) {
        super(ctx, attrs, defStyle);
    }

    @Override
    protected void onRestoreInstanceState(@Nullable Parcelable state) {
        if (!(state instanceof CheckedSavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }
        CheckedSavedState ss = (CheckedSavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        setChecked(ss.checked);
    }

    @NonNull
    @Override
    protected Parcelable onSaveInstanceState() {
        CheckedSavedState result = new CheckedSavedState(super.onSaveInstanceState());
        result.checked = checked;
        return result;
    }

    @Override
    public boolean isChecked() {
        return checked;
    }

    @Override
    public void setChecked(boolean checked) {
        if (checked != this.checked) {
            this.checked = checked;
        }
    }

    @NonNull
    @Override
    public int[] onCreateDrawableState(int extraSpace) {
        int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
        if (checked) {
            mergeDrawableStates(drawableState, CheckedStateSet);
        }
        return drawableState;
    }

    @Override
    public boolean performClick() {
        toggle();
        return super.performClick();
    }

    @Override
    public void toggle() {
        setChecked(!checked);
    }
}
