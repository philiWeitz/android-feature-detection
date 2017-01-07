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

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }


    public static void initializeFeatureDetection(Activity activity) {
        // create the BRISKD and flann matcher
        NativeWrapper.initFeatureDetection();

        // loads a static template image
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;

        ByteArrayOutputStream jpegStream = new ByteArrayOutputStream();
        Bitmap bm = BitmapFactory.decodeResource(
                activity.getResources(), R.drawable.template_image, options);
        bm.compress(Bitmap.CompressFormat.JPEG, 100, jpegStream);

        // extracts the features from the template image
        NativeWrapper.setTemplateImage(jpegStream.toByteArray());
    }


    private NativeWrapper() {

    }


    private static native void initFeatureDetection();

    public static native int processImage(byte[] imgArray);

    public static native void setTemplateImage(byte[] imgArray);

    public static native int[] getFramePoints();
}
