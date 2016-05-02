package de.geeksfactory.opacclient.logging;

import android.content.Context;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class AppLogger {
    static public Logger loggerSingleton;
    static public Handler handler;
    static public final String LOG_FILE_NAME = "opacapp.log";
    static public final int LOG_FILE_MAX_SIZE = 1024 * 900;
    static public final String LOGGER_NAME = "opacapp";

    static public Logger getLogger(Context ctx) {
        if (loggerSingleton != null) {
            return loggerSingleton;
        }
        File file = new File(ctx.getFilesDir(), LOG_FILE_NAME);

        Logger logger = Logger.getLogger(LOGGER_NAME);
        logger.setLevel(Level.INFO);

        for (Handler h : logger.getHandlers()) {
            logger.removeHandler(h);
        }

        logger.setLevel(Level.ALL);
        try {
            handler = new FileHandler(file.getAbsolutePath(), LOG_FILE_MAX_SIZE, 1, true);
            Formatter formatterTxt = new AppFormatter();
            handler.setFormatter(formatterTxt);
            logger.addHandler(handler);
        } catch (IOException e) {
            e.printStackTrace();
        }
        loggerSingleton = logger;
        return logger;
    }

    static public void flush() {
        if (handler != null) {
            handler.flush();
        }
    }

    static public Logger reInitialize(Context ctx) {
        if (handler != null) {
            handler.close();
        }
        loggerSingleton = null;
        return getLogger(ctx);
    }

    public static class AppFormatter extends Formatter {
        public AppFormatter() {
        }

        @Override
        public String format(LogRecord r) {
            StringBuilder sb = new StringBuilder();
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.GERMAN);
            sb.append("\r\n ");
            sb.append(df.format(new Date(r.getMillis()))).append(" ");
            sb.append(r.getLevel().getName()).append(" ");
            sb.append(r.getSourceClassName()).append(":");
            sb.append(r.getSourceMethodName()).append(" – ");
            sb.append(formatMessage(r)).append("\r\n");
            if (r.getThrown() != null) {
                sb.append("Throwable occurred: ");
                Throwable t = r.getThrown();
                PrintWriter pw = null;
                try {
                    StringWriter sw = new StringWriter();
                    pw = new PrintWriter(sw);
                    t.printStackTrace(pw);
                    sb.append(sw.toString());
                } finally {
                    if (pw != null) {
                        pw.close();
                    }
                }
            }
            return sb.toString();
        }
    }
}
