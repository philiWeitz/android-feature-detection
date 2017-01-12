package sandbox.org.featuredetection.camera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;



public class CanvasSurfaceView extends SurfaceView {
    private static final String TAG = "CanvasSurface";

    private Paint mPaint;
    private List<Pair<Integer, Integer>> mFramePoints = new LinkedList<>();


    public CanvasSurfaceView(Context context) {
        super(context);
        init();
    }

    public CanvasSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CanvasSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }


    private void init() {
        mPaint = new Paint();
        mPaint.setColor(Color.RED);
        mPaint.setStrokeWidth(8);

        this.getHolder().setFormat(PixelFormat.TRANSPARENT);
        this.getHolder().addCallback(mSurfaceHolderCallback);
    }


    public void setFramePoints(int[] framePoints) {
        mFramePoints.clear();

        for(int i = 0; i < framePoints.length-1; i += 2) {
            mFramePoints.add(new Pair<>(framePoints[i+1],framePoints[i]));
        }
    }


    SurfaceHolder.Callback mSurfaceHolderCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            tryDrawing(surfaceHolder);
        }

        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
            tryDrawing(surfaceHolder);
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
            Log.e("TAG", "DESTROY");
        }
    };


    public void tryDrawing() {
        tryDrawing(getHolder());
    }


    private void tryDrawing(SurfaceHolder holder) {
        Canvas canvas = holder.lockCanvas();

        if (canvas == null) {
            Log.e(TAG, "Cannot draw onto the canvas as it's null");
        } else {

            // clear canvas
            canvas.drawColor(0, PorterDuff.Mode.CLEAR);

            //draw onto the mCanvas if needed (maybe only the parts of animation that changed)
            if(null != mFramePoints && mFramePoints.size() >= 2) {
                for(int i = 0; i < (mFramePoints.size()); ++i) {
                    Pair<Integer,Integer> point1 = mFramePoints.get(i);
                    Pair<Integer,Integer> point2 = mFramePoints.get((i+1) % mFramePoints.size());

                    canvas.drawLine(point1.first, point1.second, point2.first, point2.second, mPaint);
                }
            }

            holder.unlockCanvasAndPost(canvas);
        }
    }
}
