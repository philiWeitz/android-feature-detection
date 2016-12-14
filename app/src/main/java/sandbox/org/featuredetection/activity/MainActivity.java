package sandbox.org.featuredetection.activity;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.TextureView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;

import sandbox.org.featuredetection.camera.CameraWrapper;
import sandbox.org.featuredetection.jni.NativeWrapper;

public class MainActivity extends AppCompatActivity {

    private TextureView textureView;
    private CameraWrapper mCameraWrapper;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView messages = (TextView) findViewById(R.id.messages);

        textureView = (TextureView) findViewById(R.id.texture);
        mCameraWrapper = new CameraWrapper(this, textureView, messages);

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

        mCameraWrapper.startBackgroundThread();

        if (textureView.isAvailable()) {
            mCameraWrapper.openCamera();
        }
    }


    @Override
    protected void onPause() {
        mCameraWrapper.stopBackgroundThread();
        super.onPause();
    }
}
