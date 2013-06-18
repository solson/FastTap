package usask.hci.fastdraw;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

public class DrawView extends View {
	private Bitmap mBitmap;
    private Canvas mCanvas;
    private Paint  mBitmapPaint;
	private Tool   mTool;

    public DrawView(Context c) {
        super(c);

		mTool = new PencilTool(0xFFFF0000);
        mBitmapPaint = new Paint(Paint.DITHER_FLAG);
    }
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
        mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
	}

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
    	int index = event.getActionIndex();
        float x = event.getX(index);
        float y = event.getY(index);
        int id = event.getPointerId(index);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
            	mTool.touchStart(id, x, y, mCanvas);
                invalidate();
                break;
                
            case MotionEvent.ACTION_MOVE:
            	int count = event.getPointerCount();
            	
            	for (int i = 0; i < count; i++) {
                    float x2 = event.getX(i);
                    float y2 = event.getY(i);
                	mTool.touchMove(event.getPointerId(i), x2, y2, mCanvas);
            	}
            	
                invalidate();
                break;
                
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            	mTool.touchStop(id, x, y, mCanvas);
                invalidate();
                break;
        }
        
        return true;
    }
}
