package sandbox.org.featuredetection.camera;


import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import sandbox.org.featuredetection.jni.NativeWrapper;

public class CameraWrapper implements ICameraWrapper {

    private static final String TAG = "CameraWrapper";
    public static final int REQUEST_CAMERA_PERMISSION = 200;

    private String mCameraId;
    private Activity mActivity;
    private Size mImageDimension;

    private CameraDevice mCameraDevice;
    private SurfaceView mSurfaceView;
    private CaptureRequest.Builder mPreviewRequestBuilder;

    private ImageReader mImageReader;

    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;

    private TextView mMessages;
    private boolean processing = false;


    public CameraWrapper(Activity activity, SurfaceView surfaceView, TextView messages) {
        mActivity = activity;
        mSurfaceView = surfaceView;
        mMessages = messages;

        mSurfaceView.getHolder().addCallback(mSurfaceHolderCallback);
    }


    @Override
    public void resumeCamera() {
        startBackgroundThread();
        //openCamera();
    }


    @Override
    public void pauseCamera() {
        stopBackgroundThread();
    }


    ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader imageReader) {
            Image img = imageReader.acquireLatestImage();

            if (!processing) {

                // checks if the image is valid
                if (null != img && img.getPlanes().length > 0) {
                    processing = true;

                    // converts the jpg image into byte array
                    final ByteBuffer buffer = img.getPlanes()[0].getBuffer();
                    final byte[] data = new byte[buffer.capacity()];
                    buffer.get(data);

                    new Handler().post(new ImageProcessingRunnable(data));
                }
            }

            // release the image
            if (null != img) {
                img.close();
            }
        }
    };


    private class ImageProcessingRunnable implements Runnable {
        private byte[] mJpegByteArray;

        public ImageProcessingRunnable(byte[] jpgByteArray) {
            mJpegByteArray = jpgByteArray;
        }

        @Override
        public void run() {
            // TODO: quick way of displaying the number of matches
            final int matches = NativeWrapper.processImage(mJpegByteArray);

            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (matches >= 10) {
                        mMessages.setText("Number of matches: " + matches + " -> Image is there!");
                    } else {
                        mMessages.setText("Number of matches: " + matches);
                    }

                    processing = false;
                }
            });
        }
    }


    public void openCamera() {
        CameraManager manager = (CameraManager) mActivity.getSystemService(Context.CAMERA_SERVICE);

        try {
            // get the first camera
            mCameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(mCameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            // store characteristics
            mImageDimension = map.getOutputSizes(SurfaceTexture.class)[0];

            // set the surface dimensions
            mSurfaceView.getHolder().setFixedSize(mImageDimension.getWidth(), mImageDimension.getHeight());

            // Add permission for camera and let user grant the permission
            if (ActivityCompat.checkSelfPermission(mActivity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(mActivity,
                        new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
                return;
            }

            // open the camera
            manager.openCamera(mCameraId, mCameraStateCallback, mBackgroundHandler);

        } catch (CameraAccessException e) {
            Log.e(TAG, "Error opening the camera", e);
        }
    }


    private final CameraDevice.StateCallback mCameraStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(CameraDevice camera) {
            mCameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            mCameraDevice.close();
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    };


    private void createCameraPreview() {
        try {
            // creates a surface for the preview
            Surface textureSurface = mSurfaceView.getHolder().getSurface();

            // creates the image reader to process preview frames
            mImageReader = ImageReader.newInstance(
                    mImageDimension.getWidth(),
                    mImageDimension.getHeight(),
                    ImageFormat.JPEG, 1);
            mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundHandler);

            // adds both surfaces to the request builder
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(textureSurface);
            mPreviewRequestBuilder.addTarget(mImageReader.getSurface());

            // creates a list of surfaces for the session
            List<Surface> surfaces = Arrays.asList(textureSurface, mImageReader.getSurface());

            mCameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    //The camera is already closed
                    if (null == mCameraDevice) {
                        return;
                    }

                    // When the session is ready, we start displaying the preview.
                    mPreviewRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

                    try {
                        cameraCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), null, null);
                    } catch (CameraAccessException e) {
                        Log.e(TAG, "Error setting update preview", e);
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(mActivity, "Configuration change", Toast.LENGTH_SHORT).show();
                }
            }, mBackgroundHandler);

        } catch (CameraAccessException e) {
            Log.e(TAG, "Error creating capture session", e);
        }
    }


    public void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }


    public void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private SurfaceHolder.Callback mSurfaceHolderCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            //open your camera here
            openCamera();
        }

        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

        }
    };
}