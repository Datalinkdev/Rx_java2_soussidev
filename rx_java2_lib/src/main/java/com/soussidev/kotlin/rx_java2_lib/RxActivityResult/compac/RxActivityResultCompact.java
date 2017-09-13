package com.soussidev.kotlin.rx_java2_lib.RxActivityResult.compac;

/**
 * Created by Soussi on 13/09/2017.
 */

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

import com.soussidev.kotlin.rx_java2_lib.RxActivityResult.ActivityResult;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;

public class RxActivityResultCompact {
private static final String FRAGMENT_TAG = "_RESULT_HANDLE_FRAGMENT_";

public static Observable<ActivityResult> startActivityForResult(
        @NonNull AppCompatActivity activity, @NonNull Intent intent, int requestCode) {

        return startActivityForResult(activity.getSupportFragmentManager(), intent, requestCode, null);
        }

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
public static Observable<ActivityResult> startActivityForResult(
@NonNull AppCompatActivity activity, @NonNull Intent intent, int requestCode, @Nullable Bundle options) {

        return startActivityForResult(activity.getSupportFragmentManager(), intent, requestCode, options);
        }

@RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
public static Observable<ActivityResult> startActivityForResult(
        @NonNull Fragment fragment, @NonNull Intent intent, int requestCode) {

        return startActivityForResult(fragment.getFragmentManager(), intent, requestCode, null);
        }

public static Observable<ActivityResult> startActivityForResult(
@NonNull Fragment fragment, @NonNull Intent intent, int requestCode, @NonNull Bundle options) {

        return startActivityForResult(fragment.getFragmentManager(), intent, requestCode, options);
        }

private static Observable<ActivityResult> startActivityForResult(
        @NonNull FragmentManager fragmentManager, @NonNull final Intent intent, final int requestCode, @Nullable final Bundle options) {

        ResultHandleV4Fragment _fragment = (ResultHandleV4Fragment) fragmentManager.findFragmentByTag(FRAGMENT_TAG);
        if (_fragment == null) {
        _fragment = new ResultHandleV4Fragment();

final FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.add(_fragment, FRAGMENT_TAG);
        transaction.commit();

        } else if (_fragment.isDetached()) {
final FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.attach(_fragment);
        transaction.commit();
        }

final ResultHandleV4Fragment fragment = _fragment;
        return fragment.getIsAttachedBehavior()
        .filter(new Predicate<Boolean>() {
@Override
public boolean test(@io.reactivex.annotations.NonNull Boolean isAttached) throws Exception {
        return isAttached;
        }
        })
        .flatMap(new Function<Boolean, ObservableSource<ActivityResult>>() {
@Override
public ObservableSource<ActivityResult> apply(@io.reactivex.annotations.NonNull Boolean aBoolean) throws Exception {
        fragment.startActivityForResult(intent, requestCode, options);
        return fragment.getResultPublisher();
        }
        })
        .filter(new Predicate<ActivityResult>() {
@Override
public boolean test(@io.reactivex.annotations.NonNull ActivityResult result) throws Exception {
        return result.getRequestCode() == requestCode;
        }
        });
        }
        }
