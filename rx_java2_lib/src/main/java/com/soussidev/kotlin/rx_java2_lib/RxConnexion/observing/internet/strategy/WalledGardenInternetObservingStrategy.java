package com.soussidev.kotlin.rx_java2_lib.RxConnexion.observing.internet.strategy;

import com.soussidev.kotlin.rx_java2_lib.RxConnexion.Preconditions;
import com.soussidev.kotlin.rx_java2_lib.RxConnexion.observing.internet.InternetObservingStrategy;
import com.soussidev.kotlin.rx_java2_lib.RxConnexion.observing.internet.error.ErrorHandler;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Soussi on 06/09/2017.
 */

public class WalledGardenInternetObservingStrategy implements InternetObservingStrategy {
    private static final String DEFAULT_HOST = "http://clients3.google.com/generate_204";
    private static final String HTTP_PROTOCOL = "http://";
    private static final String HTTPS_PROTOCOL = "https://";

    @Override public String getDefaultPingHost() {
        return DEFAULT_HOST;
    }

    @Override public Observable<Boolean> observeInternetConnectivity(final int initialIntervalInMs,
                                                                     final int intervalInMs, final String host, final int port, final int timeoutInMs,
                                                                     final ErrorHandler errorHandler) {

        Preconditions.checkGreaterOrEqualToZero(initialIntervalInMs,
                "initialIntervalInMs is not a positive number");
        Preconditions.checkGreaterThanZero(intervalInMs, "intervalInMs is not a positive number");
        checkGeneralPreconditions(host, port, timeoutInMs, errorHandler);

        final String adjustedHost = adjustHost(host);

        return Observable.interval(initialIntervalInMs, intervalInMs, TimeUnit.MILLISECONDS,
                Schedulers.io()).map(new Function<Long, Boolean>() {
            @Override public Boolean apply(@NonNull Long tick) throws Exception {
                return isConnected(adjustedHost, port, timeoutInMs, errorHandler);
            }
        }).distinctUntilChanged();
    }

    @Override public Single<Boolean> checkInternetConnectivity(final String host, final int port,
                                                               final int timeoutInMs, final ErrorHandler errorHandler) {
        checkGeneralPreconditions(host, port, timeoutInMs, errorHandler);

        return Single.create(new SingleOnSubscribe<Boolean>() {
            @Override public void subscribe(@NonNull SingleEmitter<Boolean> emitter) throws Exception {
                emitter.onSuccess(isConnected(host, port, timeoutInMs, errorHandler));
            }
        });
    }

    protected String adjustHost(final String host) {
        if (!host.startsWith(HTTP_PROTOCOL) && !host.startsWith(HTTPS_PROTOCOL)) {
            return HTTP_PROTOCOL.concat(host);
        }

        return host;
    }

    private void checkGeneralPreconditions(final String host, final int port, final int timeoutInMs,
                                           final ErrorHandler errorHandler) {
        Preconditions.checkNotNullOrEmpty(host, "host is null or empty");
        Preconditions.checkGreaterThanZero(port, "port is not a positive number");
        Preconditions.checkGreaterThanZero(timeoutInMs, "timeoutInMs is not a positive number");
        Preconditions.checkNotNull(errorHandler, "errorHandler is null");
    }

    protected Boolean isConnected(final String host, final int port, final int timeoutInMs,
                                  final ErrorHandler errorHandler) {
        HttpURLConnection urlConnection = null;
        try {
            urlConnection = createHttpUrlConnection(host, port, timeoutInMs);
            return urlConnection.getResponseCode() == HttpURLConnection.HTTP_NO_CONTENT;
        } catch (IOException e) {
            errorHandler.handleError(e, "Could not establish connection with WalledGardenStrategy");
            return Boolean.FALSE;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    protected HttpURLConnection createHttpUrlConnection(final String host, final int port,
                                                        final int timeoutInMs) throws IOException {
        URL initialUrl = new URL(host);
        URL url = new URL(initialUrl.getProtocol(), initialUrl.getHost(), port, initialUrl.getFile());
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setConnectTimeout(timeoutInMs);
        urlConnection.setReadTimeout(timeoutInMs);
        urlConnection.setInstanceFollowRedirects(false);
        urlConnection.setUseCaches(false);
        return urlConnection;
    }
}

