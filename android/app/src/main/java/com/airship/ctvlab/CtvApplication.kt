package com.airship.ctvlab

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.airship.ctvlab.airship.AirshipLab
import com.airship.ctvlab.airship.correctThomasFocus
import okhttp3.OkHttpClient

class CtvApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        AirshipLab.takeOff(this)
        correctThomasFocus()
    }

    /**
     * Wikimedia serves the catalogue artwork and answers 403 to OkHttp's default
     * `okhttp/x.y` User-Agent, so image requests have to name the app instead.
     */
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .okHttpClient {
                OkHttpClient.Builder()
                    .addInterceptor { chain ->
                        chain.proceed(
                            chain.request().newBuilder()
                                .header("User-Agent", IMAGE_USER_AGENT)
                                .build(),
                        )
                    }
                    .build()
            }
            .crossfade(true)
            .build()

    private companion object {
        const val IMAGE_USER_AGENT = "AirshipCtvLab/0.1 (Airship SDK lab sample for Android TV)"
    }
}
