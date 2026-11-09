package uz.yangialifbo.klaviatura;

import android.app.Application;
import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

/** Ilova: kutilmagan xatolarni faylga yozib qöyadi, keyin bosh ekranda körsatiladi. */
public class App extends Application {

    static final String CRASH_FILE = "oxirgi_xato.txt";

    @Override
    public void onCreate() {
        super.onCreate();
        final Thread.UncaughtExceptionHandler prev = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            save(this, e);
            if (prev != null) prev.uncaughtException(t, e);
        });
    }

    static void save(Context c, Throwable e) {
        try {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String v = "";
            try {
                v = c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName;
            } catch (Exception ignored) {
                // versiya topilmasa
            }
            String text = "Versiya: " + v + "\nAndroid: " + android.os.Build.VERSION.SDK_INT
                    + " (" + android.os.Build.MODEL + ")\n\n" + sw;
            try (FileOutputStream out = new FileOutputStream(new File(c.getFilesDir(), CRASH_FILE))) {
                out.write(text.getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception ignored) {
            // xatoni yozib bölmasa — heç narsa qilmaymiz
        }
    }

    static String read(Context c) {
        File f = new File(c.getFilesDir(), CRASH_FILE);
        if (!f.exists()) return null;
        try {
            return new String(java.nio.file.Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    static void clear(Context c) {
        //noinspection ResultOfMethodCallIgnored
        new File(c.getFilesDir(), CRASH_FILE).delete();
    }
}
