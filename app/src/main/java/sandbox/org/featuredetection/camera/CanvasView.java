package sandbox.org.featuredetection.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.View;

import java.util.LinkedList;
import java.util.List;


public class CanvasView extends View {

    private Canvas mCanvas;
    private Bitmap mBitmap;
    private List<Pair<Integer, Integer>> mFramePoints = new LinkedList<>();


    public CanvasView(Context context) {
        super(context);
    }

    public CanvasView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CanvasView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    public void setFramePoints(int[] framePoints) {
        mFramePoints.clear();

        for(int i = 0; i < framePoints.length-1; i += 2) {
            mFramePoints.add(new Pair<Integer, Integer>(framePoints[i+1],framePoints[i]));
        }
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (mBitmap != null) {
            mBitmap.recycle();
        }
        mCanvas = new Canvas();
        mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        mCanvas.setBitmap(mBitmap);
    }


    public void destroy() {
        if (mBitmap != null) {
            mBitmap.recycle();
        }
    }


    @Override
    public void onDraw(Canvas c) {
        mCanvas.drawColor(0, PorterDuff.Mode.CLEAR);

        Paint paint = new Paint();
        paint.setStrokeWidth(10);
        paint.setColor(Color.RED);

        //draw onto the mCanvas if needed (maybe only the parts of animation that changed)
        if(null != mFramePoints && mFramePoints.size() >= 2) {
            for(int i = 0; i < (mFramePoints.size()); ++i) {
                Pair<Integer,Integer> point1 = mFramePoints.get(i);
                Pair<Integer,Integer> point2 = mFramePoints.get((i+1) % mFramePoints.size());

                mCanvas.drawLine(point1.first, point1.second, point2.first, point2.second, paint);
            }
        }

        //draw the mBitmap to the real mCanvas c
        c.drawBitmap(mBitmap,
                new Rect(0,0, mBitmap.getWidth(), mBitmap.getHeight()),
                new Rect(0,0, mBitmap.getWidth(), mBitmap.getHeight()), null);
    }
}
