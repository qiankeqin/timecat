package com.time.cat.ui.modules.intro;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.time.cat.R;
import com.time.cat.TimeCatApp;
import com.time.cat.data.Constants;
import com.time.cat.test.DefaultDataGenerator;
import com.time.cat.ui.base.BaseActivity;
import com.time.cat.ui.modules.main.MainActivity;
import com.time.cat.util.UrlCountUtil;
import com.timecat.commonjar.contentProvider.SPHelper;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;

public class SplashActivity extends BaseActivity {

    public static final String KEY = "key" + getVersion(TimeCatApp.getInstance());
    private static final java.lang.String GOTO_HOME = "go_home";
    private static final java.lang.String GOTO_INTRO = "go_intro";

    public static String getVersion(Context context) {
        try {
            PackageInfo pi = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return pi.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return "1";
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            Intent intent = getIntent();
            if (intent.getAction().equals(Constants.NOTIFY_SCREEN_CAPTURE_OVER_BROADCAST)) {
                UrlCountUtil.onEvent(UrlCountUtil.CLICK_NOFITY_SCREEN);
                sendBroadcast(new Intent(Constants.SCREEN_CAPTURE_OVER_BROADCAST));
                finish();
                return;
            } else if (intent.getAction().equals(Constants.NOTIFY_UNIVERSAL_COPY_BROADCAST)) {
                UrlCountUtil.onEvent(UrlCountUtil.CLICK_NOFITY_COPY);
                sendBroadcast(new Intent(Constants.UNIVERSAL_COPY_BROADCAST));
                finish();
                return;
            }
        } catch (Throwable e) {
        }
        if (!PreferenceManager.getDefaultSharedPreferences(TimeCatApp.getInstance())
                .getBoolean("DEFAULT_DATA_INSERTED", false)) {
            DefaultDataGenerator.fillDBWithDummyData(getApplicationContext());
            PreferenceManager.getDefaultSharedPreferences(TimeCatApp.getInstance())
                    .edit().putBoolean("DEFAULT_DATA_INSERTED", true).apply();
        }
        setContentView(R.layout.activity_splash);
        setUpSplash();
    }

    private void setUpSplash() {
        Observable.timer(2, TimeUnit.SECONDS).compose(bindToLifecycle()).observeOn(AndroidSchedulers.mainThread()).flatMap(new Func1<Long, Observable<String>>() {
            @Override
            public Observable<String> call(Long aLong) {
                boolean isShowIntro = SPHelper.getBoolean(KEY, false);
                if (isShowIntro) {
                    return Observable.just(GOTO_HOME);
                } else {
                    return Observable.just(GOTO_INTRO);
                }
            }
        }).subscribe(s -> {
            if (s.equals(GOTO_HOME)) {
                if (SPHelper.getBoolean(PreSettingActivity.SHOW, true)) {
                    startActivity(new Intent(SplashActivity.this, PreSettingActivity.class));
                } else {
                    startActivity(new Intent(SplashActivity.this, MainActivity.class));
                }

                finish();
            } else {
                startActivity(new Intent(SplashActivity.this, IntroActivity.class));
                finish();
            }
        });
    }
}
