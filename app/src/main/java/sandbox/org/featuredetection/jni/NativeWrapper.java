package sandbox.org.featuredetection.jni;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayOutputStream;

import sandbox.org.featuredetection.activity.R;

/**
 * Created by my on 13.12.2016.
 */

public class NativeWrapper {

    private static long sHandle;

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }


    public static void initializeFeatureDetection(Activity activity) {
        // create the BRISKD and flann matcher
        sHandle = NativeWrapper.initFeatureDetection();

        // loads a static template image
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;

        ByteArrayOutputStream jpegStream = new ByteArrayOutputStream();
        Bitmap bm = BitmapFactory.decodeResource(
                activity.getResources(), R.drawable.template_image, options);
        bm.compress(Bitmap.CompressFormat.JPEG, 100, jpegStream);

        // extracts the features from the template image
        setTemplateImage(sHandle, jpegStream.toByteArray());
    }


    public static int processImage(byte[] imgArray) {
        return processImage(sHandle, imgArray);
    }


    public static int[] getFramePoints() {
        return getFramePoints(sHandle);
    }


    private NativeWrapper() {

    }


    private static native long initFeatureDetection();

    public static native int processImage(long handle, byte[] imgArray);

    public static native void setTemplateImage(long handle, byte[] imgArray);

    public static native int[] getFramePoints(long handle);

    public static native void destroyHandle(long handle);
}
