package com.getsixtyfour.openvpnmgmt.android;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.getsixtyfour.openvpnmgmt.net.UsernamePasswordHandler;

class OpenVpnHandler implements UsernamePasswordHandler {

    private final CharSequence mUser;

    private final CharSequence mPassword;

    OpenVpnHandler(@NonNull CharSequence user, @NonNull CharSequence password) {
        mUser = user;
        mPassword = password;
    }

    @Override
    @Nullable
    public CharSequence getUser() {
        return mUser;
    }

    @Override
    @Nullable
    public CharSequence getPassword() {
        return mPassword;
    }
}
