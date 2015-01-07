package richard.chard.lu.android.areamapper;

import android.util.Log;

import java.util.regex.Matcher;

/**
 * @author Richard Lu
 */
public class Logger {

    public static Logger create(Class clazz) {
        return new Logger(clazz);
    }

    private String className;

    private Logger(Class clazz) {
        className = clazz.getName().substring(clazz.getName().lastIndexOf('.')+1);
    }

    private void log(
            int level,
            String messageFormat,
            Object... data) {

        for (Object datum : data) {
            if (datum == null) {
                datum = "null";
            }
            messageFormat = messageFormat.replaceFirst("\\{\\}", Matcher.quoteReplacement(datum.toString()));
        }

        StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[4];

        String formattedMessage = "." + stackTraceElement.getMethodName() + ": " + messageFormat;

        switch (level) {
            case Log.VERBOSE:
                Log.v(className, formattedMessage);
                break;
            case Log.DEBUG:
                Log.d(className, formattedMessage);
                break;
            case Log.INFO:
                Log.i(className, formattedMessage);
                break;
            case Log.WARN:
                Log.w(className, formattedMessage);
                break;
            case Log.ERROR:
                Log.e(className, formattedMessage);
                break;
            default:
                throw new RuntimeException("Unknown log level: "+level);
        }

    }

    public void debug(String messageFormat, Object... data) {
        log(Log.DEBUG, messageFormat, data);
    }

    public void error(String messageFormat, Object... data) {
        log(Log.ERROR, messageFormat, data);
    }

    public void info(String messageFormat, Object... data) {
        log(Log.INFO, messageFormat, data);
    }

    public void trace(String messageFormat, Object... data) {
        log(Log.VERBOSE, messageFormat, data);
    }

    public void warn(String messageFormat, Object... data) {
        log(Log.WARN, messageFormat, data);
    }

}
