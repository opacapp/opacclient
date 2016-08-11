/**
 * Copyright (C) 2016 by Johan von Forstner under the MIT license:
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package de.geeksfactory.opacclient.storage;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.joda.time.DateTime;

import java.io.IOException;
import java.io.InputStream;

import de.geeksfactory.opacclient.utils.Utils;

/**
 * Provides a type-safe interface to some SharedPreferences settings.
 */
public class PreferenceDataSource {

    private static final String LAST_LIBRARY_CONFIG_UPDATE = "last_library_config_update";
    protected SharedPreferences sp;
    protected Context context;

    private static final String LAST_LIBRARY_CONFIG_UPDATE_FILE = "last_library_config_update.txt";

    public PreferenceDataSource(Context context) {
        this.context = context;
        sp = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public DateTime getLastLibraryConfigUpdate() {
        String lastUpdate = sp.getString(LAST_LIBRARY_CONFIG_UPDATE, null);
        if (lastUpdate == null) {
            try {
                InputStream is = context.getAssets().open(LAST_LIBRARY_CONFIG_UPDATE_FILE);
                return new DateTime(Utils.readStreamToString(is));
            } catch (IOException e) {
                return null;
            }
        }
        return new DateTime(lastUpdate);
    }

    public void setLastLibraryConfigUpdate(DateTime lastUpdate) {
        sp.edit().putString(LAST_LIBRARY_CONFIG_UPDATE, lastUpdate.toString()).apply();
    }
}
