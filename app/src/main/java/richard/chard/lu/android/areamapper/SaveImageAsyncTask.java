package richard.chard.lu.android.areamapper;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Environment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * @author Richard Lu
 */
public class SaveImageAsyncTask extends AsyncTask<Void, Void, Void> {

    public interface OnImageSavedListener {

        public void onImageSaved(File imageFile);

    }

    private static final Logger LOG = Logger.create(SaveImageAsyncTask.class);

    private final Bitmap bitmap;
    private final Bitmap.CompressFormat format;
    private final int quality;
    private final String folderName;
    private final String filePrefix;
    private final String fileSuffix;
    private final OnImageSavedListener listener;
    private File imageFile;

    public SaveImageAsyncTask(
            Bitmap bitmap,
            Bitmap.CompressFormat format,
            int quality,
            String folderName,
            String filePrefix,
            String fileSuffix,
            OnImageSavedListener listener) {
        LOG.trace("Entry");

        this.bitmap = bitmap;
        this.format = format;
        this.quality = quality;
        this.folderName = folderName;
        this.filePrefix = filePrefix;
        this.fileSuffix = fileSuffix;
        this.listener = listener;

        LOG.trace("Exit");
    }

    @Override
    protected Void doInBackground(Void... voids) {
        LOG.trace("Entry");

        // Compress the bitmap

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(
                format,
                quality,
                byteArrayOutputStream
        );

        // Check for image folder, create if needed

        File imageFolder = new File(folderName);

        if (!imageFolder.mkdirs() && !imageFolder.isDirectory()) {
            throw new RuntimeException("Failed to create directory "+imageFolder.getPath());
        }

        // Create unique file name

        List<String> existingImageNames = Arrays.asList(imageFolder.list());

        int imageFileIndex = 0;
        while (existingImageNames.contains(
                filePrefix + imageFileIndex + fileSuffix)) {
            imageFileIndex++;
        }

        String imageFileName = filePrefix + imageFileIndex + fileSuffix;

        // Write file

        imageFile = new File(imageFolder, imageFileName);

        try {
            FileOutputStream fileOutputStream = new FileOutputStream(imageFile);
            fileOutputStream.write(byteArrayOutputStream.toByteArray());
            fileOutputStream.close();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

        LOG.trace("Exit");
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        LOG.trace("Entry");

        listener.onImageSaved(imageFile);

        LOG.trace("Exit");
    }
}
