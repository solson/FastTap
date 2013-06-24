package usask.hci.fastdraw;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.util.SparseArray;

public class PaintTool extends Tool {
	private Paint mPaint;
	private SparseArray<PointF> mPoints;
	private SparseArray<Path> mPaths;
	
	public PaintTool(DrawView drawView, int width) {
		super(drawView);
		
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setStyle(Style.STROKE);
        mPaint.setStrokeJoin(Join.ROUND);
        mPaint.setStrokeCap(Cap.ROUND);
        mPaint.setStrokeWidth(width);
        
        mPoints = new SparseArray<PointF>();
        mPaths = new SparseArray<Path>();
	}
    
	public void touchStart(int id, float x, float y) {
		Path path = new Path();
		path.moveTo(x, y);
		
		mPaths.put(id, path);
    	mPoints.put(id, new PointF(x, y));
	}

	public void touchMove(int id, float x, float y) {
    	PointF point = mPoints.get(id);
    	Path path = mPaths.get(id);
    	
    	float midX = (point.x + x) / 2;
    	float midY = (point.y + y) / 2;
    	
    	path.quadTo(point.x, point.y, midX, midY);
    	
    	point.x = x;
    	point.y = y;
	}

	public void touchStop(int id, float x, float y, Canvas canvas) {
    	Path path = mPaths.get(id);
    	canvas.drawPath(path, mPaint);
    	
        mPoints.delete(id);
    	mPaths.delete(id);
	}
	
	public void clearFingers() {
		mPoints.clear();
		mPaths.clear();
	}
	
	public void draw(Canvas canvas) {
        mPaint.setColor(getColor());
        
        for (int i = 0; i < mPaths.size(); i++) {
        	Path path = mPaths.valueAt(i);
        	canvas.drawPath(path, mPaint);
        }
	}
}
