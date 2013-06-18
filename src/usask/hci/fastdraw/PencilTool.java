package usask.hci.fastdraw;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Paint.Style;
import android.util.SparseArray;

public class PencilTool implements Tool {
	private Paint mPaint;
	private SparseArray<PointF> mPoints;
	
	public PencilTool(int color) {
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(color);
        mPaint.setStyle(Style.STROKE);
        mPaint.setStrokeJoin(Join.ROUND);
        mPaint.setStrokeCap(Cap.ROUND);
        mPaint.setStrokeWidth(1);
        
        mPoints = new SparseArray<PointF>();
	}
    
	public void touchStart(int id, float x, float y, Canvas canvas) {
    	mPoints.put(id, new PointF(x, y));
	}

	public void touchMove(int id, float x, float y, Canvas canvas) {
    	PointF point = mPoints.get(id);
    	
    	Path path = new Path();
    	path.moveTo(point.x, point.y);
    	path.quadTo(point.x, point.y, x, y);
    	canvas.drawPath(path, mPaint);
    	
    	point.x = x;
    	point.y = y;
	}

	public void touchStop(int id, float x, float y, Canvas canvas) {
    	PointF point = mPoints.get(id);
        canvas.drawLine(point.x, point.y, x, y, mPaint);
        mPoints.delete(id);
	}
}
