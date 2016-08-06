package de.geeksfactory.opacclient.utils;

import android.content.Context;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;

import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.objects.DetailledItem;

public class PrintUtils {
    public static String printDetails(DetailledItem detailledItem, Context context) {

        InputStream is = context.getResources().openRawResource(R.raw.print_template);
        BufferedReader r = new BufferedReader(new InputStreamReader(is));
        MustacheFactory mf = new DefaultMustacheFactory();
        Mustache mustache = mf.compile(r, "print_template.mustache");
        StringWriter sw = new StringWriter();
        try {
            mustache.execute(sw, detailledItem).flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sw.toString();
    }
}
