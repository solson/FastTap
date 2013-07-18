package usask.hci.fastdraw;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PointF;
import android.util.SparseArray;

public class RectangleTool extends Tool {
    private Paint mPaint;
    private SparseArray<PointF> mOrigins;
    private SparseArray<PointF> mEnds;
    
    public RectangleTool(DrawView drawView) {
        super(drawView);
        
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setStyle(Style.STROKE);
        
        mOrigins = new SparseArray<PointF>();
        mEnds = new SparseArray<PointF>();
    }

    @Override
    public void touchStart(int id, float x, float y) {
        mOrigins.put(id, new PointF(x, y));
    }

    @Override
    public void touchMove(int id, float x, float y) {
        mEnds.put(id, new PointF(x, y));
    }

    @Override
    public void touchStop(int id, float x, float y, Canvas canvas) {
        drawId(id, canvas);
        mOrigins.delete(id);
        mEnds.delete(id);
    }

    @Override
    public void clearFingers() {
        mOrigins.clear();
        mEnds.clear();
    }

    @Override
    public void draw(Canvas canvas) {
        for (int i = 0; i < mOrigins.size(); i++) {
            int id = mOrigins.keyAt(i);
            drawId(id, canvas);
        }
    }
    
    private void drawId(int id, Canvas canvas) {
        PointF origin = mOrigins.get(id);
        PointF end = mEnds.get(id);
        
        if (end != null) {
            mPaint.setColor(getColor());
            mPaint.setStrokeWidth(getThickness());
            
            float left, top, right, bottom;
            
            if (origin.x <= end.x) {
                left = origin.x;
                right = end.x;
            } else {
                left = end.x;
                right = origin.x;
            }
            
            if (origin.y <= end.y) {
                top = origin.y;
                bottom = end.y;
            } else {
                top = end.y;
                bottom = origin.y;
            }
            
            canvas.drawRect(left, top, right, bottom, mPaint);
        }
    }
}
