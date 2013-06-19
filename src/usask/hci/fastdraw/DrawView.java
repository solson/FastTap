package usask.hci.fastdraw;

import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.view.MotionEvent;
import android.view.View;

public class DrawView extends View {
	private Bitmap mBitmap;
    private Canvas mCanvas;
    private Paint mBitmapPaint;
	private Tool mTool;
	private final int mCols = 4;
	private final int mRows = 5;
	private float mColWidth;
	private float mRowHeight;
	private boolean mShowCM;
	private Paint mCMPaint;
	private int mSelected;
	private long mPressedOutsideTime;
	private long mPressedInsideTime;
	private boolean mSwitchTools;
	private int mFingerInside;
    private boolean mCheckToolSwitch;
    private Set<Integer> mIgnoredFingers;

    public DrawView(Context c) {
        super(c);

		mTool = new PenTool(Color.RED, 16);
        mBitmapPaint = new Paint(Paint.DITHER_FLAG);
        mCMPaint = new Paint();
        mCMPaint.setTextSize(26);
        mCMPaint.setTextAlign(Align.CENTER);
        mSelected = -1;
        mFingerInside = -1;
        mCheckToolSwitch = true;
        mIgnoredFingers = new HashSet<Integer>();
        
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
        	@Override
        	public void run() {
        		long now = System.nanoTime();
        		
        		if (now - mPressedOutsideTime < 1000 * 1000 * 200 && now - mPressedInsideTime < 1000 * 1000 * 200 && mCheckToolSwitch) {
        			mSwitchTools = true;
        			mCheckToolSwitch = false;
        		} else if (mFingerInside != -1 && mCheckToolSwitch) {
        			mShowCM = true;
        			mTool.clearFingers();
        		}
        	}
        }, 25, 25);
    }
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
        mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
        mCanvas.drawRGB(0xFF, 0xFF, 0xFF);
        mColWidth = (float)w / mCols;
        mRowHeight = (float)h / mRows;
	}

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
        
    	mCMPaint.setColor(0x88FFFF00);
    	canvas.drawRect(0, mRowHeight * (mRows - 1), mColWidth, mRowHeight * mRows, mCMPaint);
    	mCMPaint.setColor(0x88888888);
    	
        if (mShowCM) {
        	for (int i = 0; i < mRows; i++) {
        		float top = i * mRowHeight;    		
        		canvas.drawLine(0, top, mColWidth * mCols, top, mCMPaint);
        	}
        	for (int i = 0; i < mCols; i++) {
        		float left = i * mColWidth;    		
        		canvas.drawLine(left, 0, left, mRowHeight * mRows, mCMPaint);
        	}
        } else {
        	float top = mRowHeight * (mRows - 1);
        	float right = mColWidth;
        	canvas.drawLine(0, top, right, top, mCMPaint);
        	canvas.drawLine(right, top, right, mRowHeight * mRows, mCMPaint);
        }

    	float top = mRowHeight * (mRows - 1);
        canvas.drawText(mTool.getName(), mColWidth / 2, top + mRowHeight / 2, mCMPaint);
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
            	if (event.getPointerCount() == 1)
            		mCheckToolSwitch = true;
            	
            	long now = System.nanoTime();
            	if (x < mColWidth && y > mRowHeight * (mRows - 1)) {
            		mFingerInside = id;
            		mPressedInsideTime = now;
            	} else {
            		int col = (int) (x / mColWidth);
            		int row = (int) (y / mRowHeight);
            		mSelected = row * mCols + col;
            		
            		mPressedOutsideTime = now;
            	}
            	
            	if (mShowCM) {
            		if (event.getPointerCount() == 2) {
            			mTool = getSelectedTool(mSelected);
            			mSwitchTools = false;
            			mCheckToolSwitch = false;
            			mShowCM = false;
            			mFingerInside = -1;
            			
            			for (int i = 0; i < 2; i++)
            				mIgnoredFingers.add(event.getPointerId(i));
            		}
            		
            		break;
            	}
            	
            	mTool.touchStart(id, x, y, mCanvas);
                break;
                
            case MotionEvent.ACTION_MOVE:
            	if (mShowCM)
            		break;
            	
            	int count = event.getPointerCount();
            	
            	for (int i = 0; i < count; i++) {
            		int fingerId = event.getPointerId(i);
            		
            		if(mIgnoredFingers.contains(fingerId))
            			continue;
            		
                    float x2 = event.getX(i);
                    float y2 = event.getY(i);
                	mTool.touchMove(fingerId, x2, y2, mCanvas);
            	}
            	
                break;
                
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            	if (id == mFingerInside)
            		mFingerInside = -1;
            	
            	if (mIgnoredFingers.contains(id)) {
            		mIgnoredFingers.remove(id);
            		break;
            	}
            	
            	if (mShowCM) {
            		if (event.getPointerCount() == 1) {
            			mShowCM = false;
            		}
            	} else {
            		mTool.touchStop(id, x, y, mCanvas);
            	}
            	
        		if (event.getPointerCount() == 1) {
        	    	if (mSwitchTools) {
        				mTool = getSelectedTool(mSelected);
        				mSwitchTools = false;
        	    	}
        		}
        		
                break;
        }

        invalidate();
        return true;
    }
    
    private Tool getSelectedTool(int selected) {
    	switch (selected) {
    		case 0: return new PencilTool(Color.RED);
    		case 1: return new PenTool(Color.RED, 16);
    		case 2: return new EraserTool();
    		default: return mTool;
    	}
    }
}
