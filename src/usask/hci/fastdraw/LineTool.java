package usask.hci.fastdraw;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.util.SparseArray;

public class LineTool extends Tool {
    private Paint mPaint;
    private Paint mCirclePaint;
    private SparseArray<PointF> mOrigins;
    private SparseArray<PointF> mEnds;

    public LineTool(DrawView drawView) {
        super(drawView);
        
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setStyle(Style.STROKE);
        mPaint.setStrokeCap(Cap.ROUND);
        mPaint.setStrokeJoin(Join.ROUND);
        
        mCirclePaint = new Paint();
        mCirclePaint.setAntiAlias(true);
        mCirclePaint.setDither(true);
        mCirclePaint.setStyle(Style.FILL);

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
            mCirclePaint.setColor(getColor());
            
            canvas.drawLine(origin.x, origin.y, end.x, end.y, mPaint);
            canvas.drawCircle(origin.x, origin.y, (float)getThickness() / 2, mCirclePaint);
            canvas.drawCircle(end.x, end.y, (float)getThickness() / 2, mCirclePaint);
        }
    }
}
