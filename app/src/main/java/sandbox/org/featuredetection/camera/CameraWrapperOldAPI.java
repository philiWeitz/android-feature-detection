package sandbox.org.featuredetection.camera;

import android.app.Activity;
import android.app.admin.SystemUpdatePolicy;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.util.List;

import sandbox.org.featuredetection.jni.NativeWrapper;


public class CameraWrapperOldAPI implements ICameraWrapper {
    private static final String TAG = "CameraOldAPI";

    private boolean mProcessPreviewFrame = true;

    private Camera mCamera;
    private Activity mCtx;
    private SurfaceView mPreview;
    private CanvasSurfaceView mCanvasSurfaceView;
    private TextView mMessages;
    private Camera.CameraInfo mCameraInfo = new Camera.CameraInfo();

    private long mCallbackPerfTimer = 0;


    public CameraWrapperOldAPI(Activity ctx, SurfaceView previewSurfaceView,
                               CanvasSurfaceView canvasSurfaceView, TextView messages) {
        mCtx = ctx;
        mPreview = previewSurfaceView;
        mCanvasSurfaceView = canvasSurfaceView;
        mMessages = messages;

        mPreview.getHolder().addCallback(mSurfaceHolderCallback);
    }


    public void resumeCamera() {
        if(null != mCamera) {
            mCamera.startPreview();
        }
    }


    public void pauseCamera() {
        // stop preview and release camera
        if(null != mCamera) {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }


    SurfaceHolder.Callback mSurfaceHolderCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            for (int cameraIdx = 0; cameraIdx < Camera.getNumberOfCameras(); ++cameraIdx) {
                Camera.getCameraInfo(cameraIdx, mCameraInfo);

                if(mCameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK
                        || mCameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    // open front facing camera
                    try {
                        mCamera = Camera.open(cameraIdx);
                        mCamera.getParameters().setJpegQuality(100);
                        mCamera.getParameters().setAutoWhiteBalanceLock(false);
                        mCamera.getParameters().setAutoExposureLock(false);
                        mCamera.setPreviewDisplay(holder);

                    } catch (Exception e) {
                        Log.e(TAG, "Unable to open front facing camera", e);
                        return;
                    }
                }
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            if(null != mCamera) {
                if (holder.getSurface() == null) {
                    return;
                }

                mCamera.stopPreview();
                setCameraPreviewOrientation();
                setOptimalPreviewSize(width, height);
                setOptimalFrameRate();
                mCamera.setPreviewCallback(mPreviewCallback);
                mCamera.startPreview();
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            pauseCamera();
        }
    };


    private void setCameraPreviewOrientation() {

        WindowManager winManager = (WindowManager) mCtx.getSystemService(Context.WINDOW_SERVICE);
        int rotation = winManager.getDefaultDisplay().getRotation();

        int degrees = 0;

        switch (rotation)
        {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        // adjust the display orientation
        int angle = (mCameraInfo.orientation + degrees) % 360;
        angle = (360 - angle) % 360;
        mCamera.setDisplayOrientation(angle);
    }


    private void setOptimalFrameRate() {
        int maxFrameRate = 0;
        for(int frameRate : mCamera.getParameters().getSupportedPreviewFrameRates()) {
            maxFrameRate = Math.max(maxFrameRate, frameRate);
        }

        if(maxFrameRate > 0) {
            Camera.Parameters param = mCamera.getParameters();
            param.setPreviewFrameRate(maxFrameRate);
            mCamera.setParameters(param);
        }
    }


    private void setOptimalPreviewSize(int w, int h) {

        List<Camera.Size> sizes = mCamera.getParameters().getSupportedPreviewSizes();

        if(w < h) {
            int portraitWidth = h;
            h = w;
            w = portraitWidth;
        }

        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;

        if (sizes == null) {
            return;
        }

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }

        if(null != optimalSize) {
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPreviewSize(optimalSize.width, optimalSize.height);
            mCamera.setParameters(parameters);
        }
    }


    private Camera.PreviewCallback mPreviewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] bytes, Camera camera) {
            if(mProcessPreviewFrame) {
                mProcessPreviewFrame = false;

                long perfNow = System.currentTimeMillis();
                Log.e(TAG,"Performance Preview Callback: " + (perfNow - mCallbackPerfTimer));
                mCallbackPerfTimer = System.currentTimeMillis();

                // get YUV image
                Camera.Parameters parameters = camera.getParameters();
                Camera.Size size = parameters.getPreviewSize();
                YuvImage image = new YuvImage(bytes, ImageFormat.NV21, size.width, size.height, null);

                // set frame for JPEG image
                Rect rectangle = new Rect();
                rectangle.bottom = size.height;
                rectangle.top = 0;
                rectangle.left = 0;
                rectangle.right = size.width;

                // convert YUV to JPEG byte array
                ByteArrayOutputStream jpegStream = new ByteArrayOutputStream();
                image.compressToJpeg(rectangle, 90, jpegStream);

                int matches = NativeWrapper.processImage(jpegStream.toByteArray());
                int[] framePoints = NativeWrapper.getFramePoints();

                //int blur = NativeWrapper.getBlurFactor(jpegStream.toByteArray());
                //Log.e("BLUR DETECT","Blur: " + blur + " - Matches: " + matches);

                mCanvasSurfaceView.setFramePoints(framePoints);
                mCanvasSurfaceView.tryDrawing();

                mMessages.setText("Number of matches: " + matches);
                mProcessPreviewFrame = true;

                try {
                    jpegStream.close();
                } catch (Exception e) {
                    Log.e(TAG, "Error closing jpeg stream", e);
                }
            }
        }
    };
}