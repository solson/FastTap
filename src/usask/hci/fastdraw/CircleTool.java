package usask.hci.fastdraw;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PointF;
import android.util.SparseArray;

public class CircleTool extends Tool {
	private Paint mPaint;
	private SparseArray<PointF> mOrigins;
	private SparseArray<PointF> mEnds;
	
	public CircleTool(DrawView drawView) {
		super(drawView);
		
		mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setStyle(Style.STROKE);
        mPaint.setStrokeWidth(16);
        
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
			float dx = origin.x - end.x;
			float dy = origin.y - end.y;
			float dist = (float)Math.sqrt(dx*dx + dy*dy);
			
			float midX = (origin.x + end.x) / 2;
			float midY = (origin.y + end.y) / 2;
			
			mPaint.setColor(getColor());
			canvas.drawCircle(midX, midY, dist / 2, mPaint);
		}
	}
}
