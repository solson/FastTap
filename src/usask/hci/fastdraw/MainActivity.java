package usask.hci.fastdraw;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;

public class MainActivity extends Activity {
	private Paint mPaint;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(new DrawView(this));

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(0xFFFF0000);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(12);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	private class Pointer {
		public float x;
		public float y;
		public Path path;
		
		public Pointer() {
			path = new Path();
		}
	}

	public class DrawView extends View {
		private Bitmap  mBitmap;
        private Canvas  mCanvas;
        private Paint   mBitmapPaint;
        private SparseArray<Pointer> mPointers;

        public DrawView(Context c) {
            super(c);

            mBitmapPaint = new Paint(Paint.DITHER_FLAG);
            mPointers = new SparseArray<Pointer>();
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            mCanvas = new Canvas(mBitmap);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            canvas.drawColor(0xFFFFFFFF);

            canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);

            int size = mPointers.size();
            for(int i = 0; i < size; i++) {
                Pointer pointer = mPointers.valueAt(i);
            	canvas.drawPath(pointer.path, mPaint);
            }
        }

        private void touch_start(int id, float x, float y) {
        	Pointer pointer = new Pointer();
        	mPointers.put(id, pointer);
            
            pointer.path.moveTo(x, y);
            pointer.x = x;
            pointer.y = y;

            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setDither(true);
            paint.setColor(0xFFFF0000);
            mCanvas.drawCircle(x, y, 6, paint);
        }
        
        private void touch_move(int id, float x, float y) {
        	Pointer pointer = mPointers.get(id);
        	pointer.path.quadTo(pointer.x, pointer.y, (x + pointer.x)/2, (y + pointer.y)/2);
        	pointer.x = x;
        	pointer.y = y;
        }
        
        private void touch_up(int id) {
        	Pointer pointer = mPointers.get(id);
        	pointer.path.lineTo(pointer.x, pointer.y);
        	
            // commit the path to our offscreen
            mCanvas.drawPath(pointer.path, mPaint);
            
            mPointers.delete(id);
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
                	touch_start(id, x, y);
                    invalidate();
                    break;
                    
                case MotionEvent.ACTION_MOVE:
                	int count = event.getPointerCount();
                	
                	for (int i = 0; i < count; i++) {
                        float x2 = event.getX(i);
                        float y2 = event.getY(i);
                		touch_move(event.getPointerId(i), x2, y2);
                	}
                	
                    invalidate();
                    break;
                    
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                	touch_up(id);
                    invalidate();
                    break;
            }
            
            return true;
        }
    }
}
