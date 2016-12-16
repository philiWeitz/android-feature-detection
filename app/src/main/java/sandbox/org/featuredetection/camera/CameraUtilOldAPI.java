package sandbox.org.featuredetection.camera;


import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.view.Surface;

import java.io.ByteArrayOutputStream;

public class CameraUtilOldAPI {

    private static final String TAG = "CamUtilOldApi";


    private CameraUtilOldAPI() {

    }


    public static byte[] rotateCameraJpegImage(Activity ctx, Camera.CameraInfo cameraInfo, byte[] jpegByteArray) {
        // get the rotation angle
        int angleToRotate = getCameraCaptureRotationAngle(ctx, cameraInfo);

        // Solve image inverting problem
        angleToRotate = angleToRotate + 180;

        // rotate image
        Bitmap originalImage = BitmapFactory.decodeByteArray(jpegByteArray, 0, jpegByteArray.length);
        Bitmap bitmapImage = rotateCameraBitmap(originalImage, angleToRotate);

        // convert the bitmap back into JPEG
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmapImage.compress(Bitmap.CompressFormat.JPEG, 100, stream);

        byte[] result = stream.toByteArray();
        originalImage.recycle();
        bitmapImage.recycle();

        return result;
    }


    private static Bitmap rotateCameraBitmap(Bitmap bitmap, int degree) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        Matrix mtx = new Matrix();
        mtx.postRotate(degree);

        return Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, true);
    }


    private static int getCameraCaptureRotationAngle(Activity mContext, Camera.CameraInfo cameraInfo) {
        int rotation = mContext.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;

        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (cameraInfo.orientation + degrees) % 360;
            result = (((360 + result) % 360) + 180 % 360) ; // compensate the mirror

        } else { // back-facing
            result = (cameraInfo.orientation - degrees + 360) % 360;
        }

        return result;
    }
}