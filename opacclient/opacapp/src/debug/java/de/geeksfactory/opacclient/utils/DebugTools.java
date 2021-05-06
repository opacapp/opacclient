package de.geeksfactory.opacclient.utils;

import android.app.Application;

import com.facebook.flipper.android.AndroidFlipperClient;
import com.facebook.flipper.android.utils.FlipperUtils;
import com.facebook.flipper.core.FlipperClient;
import com.facebook.flipper.plugins.databases.DatabasesFlipperPlugin;
import com.facebook.flipper.plugins.inspector.DescriptorMapping;
import com.facebook.flipper.plugins.inspector.InspectorFlipperPlugin;
import com.facebook.flipper.plugins.network.FlipperOkhttpInterceptor;
import com.facebook.flipper.plugins.network.NetworkFlipperPlugin;
import com.facebook.soloader.SoLoader;

import de.geeksfactory.opacclient.BuildConfig;
import okhttp3.OkHttpClient;

public class DebugTools {
    static NetworkFlipperPlugin networkPlugin;

    public static void init(Application app) {
        SoLoader.init(app, false);

        if (BuildConfig.DEBUG && FlipperUtils.shouldEnableFlipper(app)) {
            final FlipperClient client = AndroidFlipperClient.getInstance(app);
            client.addPlugin(new InspectorFlipperPlugin(app, DescriptorMapping.withDefaults()));
            networkPlugin = new NetworkFlipperPlugin();
            client.addPlugin(networkPlugin);
            client.addPlugin(new DatabasesFlipperPlugin(app));

            client.start();
        }
    }

    public static OkHttpClient.Builder prepareHttpClient(OkHttpClient.Builder builder) {

        return builder.addNetworkInterceptor(new FlipperOkhttpInterceptor(networkPlugin));
    }
}
