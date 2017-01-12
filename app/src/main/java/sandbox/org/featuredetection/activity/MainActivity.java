package sandbox.org.featuredetection.activity;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceView;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import sandbox.org.featuredetection.camera.CameraWrapper;
import sandbox.org.featuredetection.camera.CameraWrapperOldAPI;
import sandbox.org.featuredetection.camera.CanvasSurfaceView;
import sandbox.org.featuredetection.camera.ICameraWrapper;
import sandbox.org.featuredetection.jni.NativeWrapper;

public class MainActivity extends AppCompatActivity {

    private SurfaceView surfaceView;
    private ICameraWrapper mCameraWrapper;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView messages = (TextView) findViewById(R.id.messages);
        surfaceView = (SurfaceView) findViewById(R.id.previewSurfaceView);

        CanvasSurfaceView canvasSurfaceView =
                (CanvasSurfaceView) findViewById(R.id.overlayCanvasSurfaceView);

        canvasSurfaceView.setZOrderMediaOverlay(true);
        mCameraWrapper = new CameraWrapperOldAPI(this, surfaceView, canvasSurfaceView, messages);

        // NEW API - DO NOT USE YET!
        //mCameraWrapper = new CameraWrapper(this, surfaceView, messages);

        NativeWrapper.initializeFeatureDetection(this);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == CameraWrapper.REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {

                // close the app
                Toast.makeText(MainActivity.this,
                        "Camera permissions required for this application!", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        mCameraWrapper.resumeCamera();
    }


    @Override
    protected void onPause() {
        mCameraWrapper.pauseCamera();
        super.onPause();
    }
}
