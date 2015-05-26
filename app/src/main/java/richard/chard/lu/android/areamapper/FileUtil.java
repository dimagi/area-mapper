package richard.chard.lu.android.areamapper;

import android.content.Context;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by wpride1 on 3/10/15.
 */
public class FileUtil {

    public static Properties loadProperties(Context c) throws IOException {
        String[] fileList = { "local.properties" };
        Properties prop = new Properties();
        for (int i = fileList.length - 1; i >= 0; i--) {
            String file = fileList[i];
            try {
                InputStream fileStream = c.getAssets().open(file);
                prop.load(fileStream);
                fileStream.close();
            }  catch (FileNotFoundException e) {
                System.out.println("couldn't find file: " + file);
                e.printStackTrace();
            }
        }
        return prop;
    }
}
