package usask.hci.fastdraw;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.util.SparseArray;

public class PenTool implements Tool {
	private Paint mPaint;
	private SparseArray<PointF> mPoints;
	
	public PenTool(int color, int width) {
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(color);
        mPaint.setStyle(Style.STROKE);
        mPaint.setStrokeJoin(Join.ROUND);
        mPaint.setStrokeCap(Cap.ROUND);
        mPaint.setStrokeWidth(width);
        
        mPoints = new SparseArray<PointF>();
	}
    
	public void touchStart(int id, float x, float y, Canvas canvas) {
    	mPoints.put(id, new PointF(x, y));
	}

	public void touchMove(int id, float x, float y, Canvas canvas) {
    	PointF point = mPoints.get(id);
    	
    	float midX = (point.x + x) / 2;
    	float midY = (point.y + y) / 2;
    	
    	Path path = new Path();
    	path.moveTo(point.x, point.y);
    	path.quadTo(point.x, point.y, midX, midY);
    	canvas.drawPath(path, mPaint);
    	
    	point.x = midX;
    	point.y = midY;
	}

	public void touchStop(int id, float x, float y, Canvas canvas) {
    	PointF point = mPoints.get(id);
        canvas.drawLine(point.x, point.y, x, y, mPaint);
        mPoints.delete(id);
	}

	@Override
	public String getName() {
		return "Pen";
	}
}
