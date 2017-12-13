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
package de.geeksfactory.opacclient;

import de.geeksfactory.opacclient.apis.Adis;
import de.geeksfactory.opacclient.apis.BiBer1992;
import de.geeksfactory.opacclient.apis.Bibliotheca;
import de.geeksfactory.opacclient.apis.Heidi;
import de.geeksfactory.opacclient.apis.IOpac;
import de.geeksfactory.opacclient.apis.Littera;
import de.geeksfactory.opacclient.apis.OpacApi;
import de.geeksfactory.opacclient.apis.Open;
import de.geeksfactory.opacclient.apis.PicaLBS;
import de.geeksfactory.opacclient.apis.PicaOld;
import de.geeksfactory.opacclient.apis.Primo;
import de.geeksfactory.opacclient.apis.SISIS;
import de.geeksfactory.opacclient.apis.SRU;
import de.geeksfactory.opacclient.apis.TestApi;
import de.geeksfactory.opacclient.apis.TouchPoint;
import de.geeksfactory.opacclient.apis.VuFind;
import de.geeksfactory.opacclient.apis.WebOpacNet;
import de.geeksfactory.opacclient.apis.WinBiap;
import de.geeksfactory.opacclient.apis.Zones;
import de.geeksfactory.opacclient.i18n.DummyStringProvider;
import de.geeksfactory.opacclient.i18n.StringProvider;
import de.geeksfactory.opacclient.networking.HttpClientFactory;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.reporting.ReportHandler;

/**
 * This class is used to simplify obtaining {@link OpacApi} instances.
 *
 * @author Johan von Forstner
 */
public class OpacApiFactory {
    protected OpacApiFactory() {
    }

    /**
     * Creates an {@link OpacApi} instance for accessing the given {@link Library}. This method will
     * use a {@link DummyStringProvider} (not provide any human-readable error messages), the
     * default {@link HttpClientFactory} with the User-Agent "libopac" and provide results in the
     * library's default language.
     *
     * @param lib       the {@link Library} you want to connect to
     * @param userAgent the value to use as the User-Agent header for HTTP requests. Will be
     *                  overridden if the library's configuration contains the {@code disguise}
     *                  parameter.
     * @return a new {@link OpacApi} instance
     */
    public static OpacApi create(Library lib, String userAgent) {
        return create(lib, new DummyStringProvider(), new HttpClientFactory(userAgent), null, null);
    }

    public static OpacApi create(Library lib, StringProvider sp, HttpClientFactory hcf,
            String lang) {
        return create(lib, sp, hcf, lang, null);
    }

    /**
     * Creates an {@link OpacApi} instance for accessing the given {@link Library}
     *
     * @param lib  the {@link Library} you want to connect to
     * @param sp   the {@link StringProvider} to use
     * @param hcf  the {@link HttpClientFactory} to use
     * @param lang the preferred language as a ISO-639-1 code, see {@link OpacApi#setLanguage(String)}
     * @return a new {@link OpacApi} instance
     */
    public static OpacApi create(Library lib, StringProvider sp, HttpClientFactory hcf,
            String lang, ReportHandler reportHandler) {
        OpacApi newApiInstance;
        if (lib.getApi().equals("bibliotheca")) {
            newApiInstance = new Bibliotheca();
        } else if (lib.getApi().equals("sisis")) {
            newApiInstance = new SISIS();
        } else if (lib.getApi().equals("zones")) {
            newApiInstance = new Zones();
        } else if (lib.getApi().equals("biber1992")) {
            newApiInstance = new BiBer1992();
        } else if (lib.getApi().equals("pica")) {
            switch (lib.getData().optString("account_system", "")) {
                case "lbs":
                    newApiInstance = new PicaLBS();
                    break;
                case "default":
                    newApiInstance = new PicaOld();
                    break;
                default:
                    newApiInstance = new PicaOld();
                    break;
            }
        } else if (lib.getApi().equals("iopac")) {
            newApiInstance = new IOpac();
        } else if (lib.getApi().equals("adis")) {
            newApiInstance = new Adis();
        } else if (lib.getApi().equals("sru")) {
            newApiInstance = new SRU();
        } else if (lib.getApi().equals("primo")) {
            newApiInstance = new Primo();
        } else if (lib.getApi().equals("vufind")) {
            newApiInstance = new VuFind();
        } else if (lib.getApi().equals("webopac.net")) {
            newApiInstance = new WebOpacNet();
        } else if (lib.getApi().equals("web-opac.at") || lib.getApi().equals("littera")) {
            newApiInstance = new Littera();
        } else if (lib.getApi().equals("winbiap")) {
            newApiInstance = new WinBiap();
        } else if (lib.getApi().equals("heidi")) {
            newApiInstance = new Heidi();
        } else if (lib.getApi().equals("touchpoint")) {
            newApiInstance = new TouchPoint();
        } else if (lib.getApi().equals("open")) {
            newApiInstance = new Open();
        } else if (lib.getApi().equals("test")) {
            newApiInstance = new TestApi();
        } else {
            return null;
        }
        newApiInstance.init(lib, hcf);
        newApiInstance.setStringProvider(sp);
        newApiInstance.setReportHandler(reportHandler);
        if (lang != null) newApiInstance.setLanguage(lang);
        return newApiInstance;
    }
}
