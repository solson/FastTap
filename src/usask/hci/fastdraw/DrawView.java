package usask.hci.fastdraw;

import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.widget.NumberPicker;

public class DrawView extends View {
	private MainActivity mMainActivity;
	private StudyLogger mLog;
	private StudyController mStudyCtl;
	private boolean mStudyMode;
	private Bitmap mBitmap;
    private Paint mBitmapPaint;
	private final int mCols = 4;
	private final int mRows = 5;
	private float mColWidth;
	private float mRowHeight;
	private boolean mShowOverlay;
	private long mOverlayStart;
	private Paint mOverlayPaint;
	private int mSelected;
	private long mPressedInsideTime;
	private int mFingerInside;
    private boolean mCheckOverlay;
    private Set<Integer> mFingers;
    private Set<Integer> mIgnoredFingers;
    private Selection[] mSelections;
	private Tool mTool;
    private int mColor;
    private int mThickness;
    private String mToolName;
    private String mColorName;
    private String mThicknessName;
    private Bitmap mUndo;
    private Bitmap mNextUndo;
    private boolean mLeftHanded;
    private final float mThreshold = 10; // pixel distance before tool registers
    private SparseArray<PointF> mOrigins;
    private boolean mPermanentGrid;
    private Rect mTextBounds;
    private SparseArray<Long> mFlashTimes;
    private SparseArray<Long> mRecentTouches;
    private boolean mChanged;
    private final int mChordDelay = 1000 * 1000 * 200; // 200ms in ns
	private final int mFlashDelay = 1000 * 1000 * 400; // 400ms in ns
	private final int mOverlayButtonIndex = 16;
    
    private enum Action {
    	SAVE, CLEAR, UNDO
    }

    private enum SelectionType {
    	TOOL, COLOR, THICKNESS, ACTION
    }
    
	private class Selection {
		public Object object;
		public String name;
		public SelectionType type;
		
		public Selection(Object object, String name, SelectionType type) {
			this.object = object;
			this.name = name;
			this.type = type;
		}
	}

    public DrawView(Context mainActivity) {
        super(mainActivity);
        
        if (!(mainActivity instanceof MainActivity)) {
        	Log.e("DrawView", "DrawView was not given the MainActivity");
        	return;
        }

        mMainActivity = (MainActivity) mainActivity;
        mStudyMode = false;
        mLog = new StudyLogger(mainActivity);
        mBitmapPaint = new Paint(Paint.DITHER_FLAG);
        mOverlayPaint = new Paint();
        mOverlayPaint.setTextSize(26);
        mOverlayPaint.setTextAlign(Align.CENTER);
        mSelected = -1;
        mFingerInside = -1;
        mCheckOverlay = true;
        mFingers = new HashSet<Integer>();
        mIgnoredFingers = new HashSet<Integer>();
        mLeftHanded = false;
        mPermanentGrid = false;
        mOrigins = new SparseArray<PointF>();
        mTextBounds = new Rect();
        mFlashTimes = new SparseArray<Long>();
        mRecentTouches = new SparseArray<Long>();
        mChanged = false;
        
        mSelections = new Selection[] {
        	new Selection(new PaintTool(this), "Paintbrush", SelectionType.TOOL),
        	new Selection(new LineTool(this), "Line", SelectionType.TOOL),
        	new Selection(new CircleTool(this), "Circle", SelectionType.TOOL),
        	new Selection(new RectangleTool(this), "Rectangle", SelectionType.TOOL),
        	
        	new Selection(Color.BLACK, "Black", SelectionType.COLOR),
        	new Selection(Color.RED, "Red", SelectionType.COLOR),
        	new Selection(Color.GREEN, "Green", SelectionType.COLOR),
        	new Selection(Color.BLUE, "Blue", SelectionType.COLOR),

        	new Selection(Color.WHITE, "White", SelectionType.COLOR),
        	new Selection(Color.YELLOW, "Yellow", SelectionType.COLOR),
        	new Selection(Color.CYAN, "Cyan", SelectionType.COLOR),
        	new Selection(Color.MAGENTA, "Magenta", SelectionType.COLOR),

        	new Selection(1, "Fine", SelectionType.THICKNESS),
        	new Selection(6, "Thin", SelectionType.THICKNESS),
        	new Selection(16, "Normal", SelectionType.THICKNESS),
        	new Selection(50, "Wide", SelectionType.THICKNESS),
        	
        	null, // The position of the command map button
        	new Selection(Action.SAVE, "Save", SelectionType.ACTION),
        	new Selection(Action.CLEAR, "Clear", SelectionType.ACTION),
        	new Selection(Action.UNDO, "Undo", SelectionType.ACTION)
        };
        
        // Default to thin black paintbrush
        changeSelection(0, false);
        changeSelection(4, false);
        changeSelection(13, false);
        
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
        	@Override
        	public void run() {
        		long now = System.nanoTime();
        		
        		synchronized (mFlashTimes) {
	        		for (int i = 0; i < mFlashTimes.size(); i++) {
	        			long time = mFlashTimes.valueAt(i);
		        		if (now - time > mFlashDelay) {
		        			mFlashTimes.removeAt(i);
		        			postInvalidate();
		        		}
	        		}
        		}
        		
        		if (mFingerInside != -1 && now - mPressedInsideTime > mChordDelay && mCheckOverlay && !mShowOverlay) {
        			mOverlayStart = now;
        			mShowOverlay = true;
        			mLog.event("Overlay shown");
        			mTool.clearFingers();
        			postInvalidate();
        			
        			if (mStudyMode)
        				mStudyCtl.handleOverlayShown();
        		}
        	}
        }, 25, 25);

        final NumberPicker subjectIdPicker = new NumberPicker(mainActivity);
        subjectIdPicker.setMinValue(0);
        subjectIdPicker.setMaxValue(999);
        subjectIdPicker.setWrapSelectorWheel(false);
        
        // Prevent the keyboard from popping up.
        subjectIdPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        
        new AlertDialog.Builder(mainActivity)
        	.setMessage(R.string.dialog_study_mode)
        	.setCancelable(false)
        	.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
        		public void onClick(DialogInterface dialog, int which) {
        			mStudyMode = true;
        			mStudyCtl = new StudyController(mLog);
        			mMainActivity.setTitle(mStudyCtl.getPrompt());
        			mLog.setSubjectId(subjectIdPicker.getValue());
        		}
        	})
        	.setNegativeButton(android.R.string.no, null)
        	.setView(subjectIdPicker)
        	.show();
    }
    
    public int getColor() {
    	return mColor;
    }
    
    public int getThickness() {
    	return mThickness;
    }
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
        mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(mBitmap);
        canvas.drawRGB(0xFF, 0xFF, 0xFF);
        mColWidth = (float)w / mCols;
        mRowHeight = (float)h / mRows;
	}
	
	private RectF getButtonBounds(int index) {
		int y = index / mCols;
		int x = index % mCols;
		
		if (mLeftHanded)
			x = mCols - x - 1;
		
		float top = mRowHeight * y;
		float bottom = top + mRowHeight;
		float left = mColWidth * x;
		float right = left + mColWidth;
		
		return new RectF(left, top, right, bottom);
	}
	
	private RectF getOverlayButtonBounds() {
		return getButtonBounds(mOverlayButtonIndex);
	}

    @Override
    protected void onDraw(Canvas canvas) {
    	RectF bounds = getOverlayButtonBounds();
    	
        canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
        mTool.draw(canvas);
        
        if (mShowOverlay)
        	canvas.drawARGB(0xAA, 0xFF, 0xFF, 0xFF);
        
    	mOverlayPaint.setColor(0x88FFFF00);
    	canvas.drawRect(bounds, mOverlayPaint);
    	
        if (mShowOverlay || mPermanentGrid) {
        	mOverlayPaint.setColor(0x44666666);

        	for (int i = 0; i < mRows; i++) {
        		float top = i * mRowHeight;    		
        		canvas.drawLine(0, top, mColWidth * mCols, top, mOverlayPaint);
        	}
        	for (int i = 0; i < mCols; i++) {
        		float left = i * mColWidth;    		
        		canvas.drawLine(left, 0, left, mRowHeight * mRows, mOverlayPaint);
        	}
        }
        
        if (mShowOverlay) {
    		mOverlayPaint.setColor(0xFF000000);
        	for (int y = 0; y < mRows; y++) {
        		for (int x = 0; x < mCols; x++) {
        			int realX = x;
        			if (mLeftHanded)
        				realX = mCols - x - 1;
        			
        			int i = y * mCols + realX;
        			if (mSelections[i] != null) {
        				String name = mSelections[i].name;
        				int heightAdj = getTextHeight(name, mOverlayPaint) / 2;
        				canvas.drawText(name, (x + 0.5f) * mColWidth, (y + 0.5f) * mRowHeight + heightAdj, mOverlayPaint);
        			}
        		}
        	}
        } else if (!mPermanentGrid) {
    		mOverlayPaint.setColor(0x44666666);
        	canvas.drawLine(bounds.left, bounds.top, bounds.right, bounds.top, mOverlayPaint);
        	
        	if (mLeftHanded)
        		canvas.drawLine(bounds.left, bounds.top, bounds.left, bounds.bottom, mOverlayPaint);
        	else
        		canvas.drawLine(bounds.right, bounds.top, bounds.right, bounds.bottom, mOverlayPaint);
        }
        
        synchronized (mFlashTimes) {
        	for (int i = 0; i < mFlashTimes.size(); i++) {
        		int selectionNum = mFlashTimes.keyAt(i);
    	        Selection selection = mSelections[selectionNum];
    	        if (selection != null) {
    	        	RectF buttonBounds = getButtonBounds(selectionNum);
    	        	
    	        	mOverlayPaint.setColor(0xBBF5F5F5);
    	        	canvas.drawRect(buttonBounds, mOverlayPaint);
    	        	
    	        	mOverlayPaint.setColor(0x44666666);
    	        	mOverlayPaint.setStyle(Style.STROKE);
    	        	canvas.drawRect(buttonBounds, mOverlayPaint);
    	        	mOverlayPaint.setStyle(Style.FILL);
    	        	
    	    		mOverlayPaint.setColor(0xFF000000);
    				String name = selection.name;
    				int heightAdj = getTextHeight(name, mOverlayPaint) / 2;
    				canvas.drawText(name, buttonBounds.left + 0.5f * mColWidth, buttonBounds.top + 0.5f * mRowHeight + heightAdj, mOverlayPaint);
    	        }
            }
		}
        
		mOverlayPaint.setColor(0xFF666666);
        canvas.drawText(mThicknessName, bounds.left + mColWidth / 2,
        		bounds.top + mRowHeight / 2 + getTextHeight(mThicknessName, mOverlayPaint) / 2 - 30, mOverlayPaint);
        canvas.drawText(mColorName, bounds.left + mColWidth / 2,
        		bounds.top + mRowHeight / 2 + getTextHeight(mColorName, mOverlayPaint) / 2, mOverlayPaint);
        canvas.drawText(mToolName, bounds.left + mColWidth / 2,
        		bounds.top + mRowHeight / 2 + getTextHeight(mToolName, mOverlayPaint) / 2 + 30, mOverlayPaint);
    }
    
    private int getTextHeight(String text, Paint paint) {
		mOverlayPaint.getTextBounds(text, 0, text.length(), mTextBounds);
		return mTextBounds.height();
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
    	int index = event.getActionIndex();
        float x = event.getX(index);
        float y = event.getY(index);
        int id = event.getPointerId(index);
    	long now = System.nanoTime();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
            	mLog.event("Touch down: " + id);
            	
            	mFingers.add(id);
            	
            	if (event.getPointerCount() == 1)
            		mCheckOverlay = true;
            	
            	if (getOverlayButtonBounds().contains(x, y)) {
            		mFingerInside = id;
            		mPressedInsideTime = now;
            		mIgnoredFingers.add(mFingerInside);
            	} else {
            		int col = (int) (x / mColWidth);
            		int row = (int) (y / mRowHeight);
            		
            		if (mLeftHanded)
            			col = mCols - col - 1;
            		
            		mSelected = row * mCols + col;
                	mRecentTouches.put(mSelected, now);
            	}
            	
            	for (int i = 0; i < mRecentTouches.size(); i++) {
            		int selection = mRecentTouches.keyAt(i);
            		long time = mRecentTouches.valueAt(i);
            		
            		if ((now - time < mChordDelay && now - mPressedInsideTime < mChordDelay) || mShowOverlay) {
            			changeSelection(selection);
            			mCheckOverlay = false;
            			mRecentTouches.removeAt(i);
            			i--;
            		} else if (now - time > mChordDelay) {
            			mRecentTouches.removeAt(i);
            			i--;
            		}
            	}
            	
            	if (!mShowOverlay) {
	            	mOrigins.put(id, new PointF(x, y));
	            	mTool.touchStart(id, x, y);
            	}
                break;
                
            case MotionEvent.ACTION_MOVE:
            	if (mShowOverlay)
            		break;
            	
            	int count = event.getPointerCount();
            	
            	for (int i = 0; i < count; i++) {
            		int fingerId = event.getPointerId(i);
            		
            		if(mIgnoredFingers.contains(fingerId))
            			continue;
            		
                    float x2 = event.getX(i);
                    float y2 = event.getY(i);
                    PointF origin = mOrigins.get(fingerId);
                    
                    if (origin != null) {
	                    float dx = origin.x - x2;
	                    float dy = origin.y - y2;
	                    double dist = Math.sqrt(dx*dx + dy*dy);
	                    
	                    if (dist > mThreshold) {
	                    	mOrigins.delete(fingerId);
	                    	origin = null;
	                    }
                    }
                    
                    if (origin == null) {
                    	mTool.touchMove(fingerId, x2, y2);
                    	
                        if (!mChanged) {
                        	mChanged = true;
                        	mNextUndo = mBitmap.copy(mBitmap.getConfig(), true);
                        }
                    }
            	}
            	
                break;
                
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            	mLog.event("Touch up: " + id);

            	mOrigins.delete(id);
                mFingers.remove(id);

            	if (id == mFingerInside)
            		mFingerInside = -1;
            	
        		boolean draw = true;
        		
            	if (mIgnoredFingers.contains(id)) {
            		mIgnoredFingers.remove(id);
            		draw = false;
            	}
            	
            	if (mShowOverlay) {
            		if (event.getPointerCount() == 1) {
            			mShowOverlay = false;
            			long duration = now - mOverlayStart;
            			mLog.event("Overlay hidden after " + duration / 1000000 + " ms");
            		}
            	} else if (draw) {
                    if (event.getPointerCount() == 1 && mChanged)
                    	mUndo = mNextUndo;
                    
            		mTool.touchStop(id, x, y, new Canvas(mBitmap));
            	}

            	if (event.getPointerCount() == 1)
            		mChanged = false;
        		
                break;
        }

        invalidate();
        return true;
    }
    
    private void changeSelection(int selected) {
    	changeSelection(selected, true);
    }
    
    private void changeSelection(int selected, boolean fromUser) {
		if (mTool != null)
			mTool.clearFingers();
		
		for (int id : mFingers) {
			mIgnoredFingers.add(id);
		}
		
		mOrigins.clear();
		
    	Selection selection = mSelections[selected];
    	
    	if (selection == null)
    		return;
		
		if (fromUser) {
			synchronized (mFlashTimes) {
				mFlashTimes.put(selected, System.nanoTime());
			}
			
			invalidate();
		}
    	
    	switch (selection.type) {
    		case TOOL:
    			if (fromUser)
    				mLog.event("Tool selected: " + selection.name);
    			
    			mTool = (Tool) selection.object;
    			mToolName = selection.name;
    			break;
    			
    		case COLOR:
    			if (fromUser)
    				mLog.event("Color selected: " + selection.name);
    			
    			mColor = (Integer) selection.object;
    			mColorName = selection.name;
    			break;
    			
    		case THICKNESS:
    			if (fromUser)
    				mLog.event("Thickness selected: " + selection.name);
    			
    			mThickness = (Integer) selection.object;
    			mThicknessName = selection.name;
    			break;
    			
    		case ACTION:
    			if (fromUser)
    				mLog.event("Action selected: " + selection.name);
    			
    			switch ((Action) selection.object) {
					case SAVE:
						break;
				
    				case CLEAR:
    					mUndo = mBitmap.copy(mBitmap.getConfig(), true);
    			        Canvas canvas = new Canvas(mBitmap);
    			        canvas.drawRGB(0xFF, 0xFF, 0xFF);
    					break;
    					
    				case UNDO:
    					if (mUndo != null) {
        					Bitmap temp = mBitmap;
        					mBitmap = mUndo;
        					mUndo = temp;
        				}
    					break;
    			}
    			break;
    	}
    	
    	if (fromUser && mStudyMode) {
	    	mStudyCtl.handleSelected(selection.name);
	    	mMainActivity.setTitle(mStudyCtl.getPrompt());
    	}
    }

	public void loadPreferences(SharedPreferences sharedPreferences) {
		mLeftHanded = sharedPreferences.getBoolean("pref_left_handed", false);
		mPermanentGrid = sharedPreferences.getBoolean("pref_permanent_grid", false);
		invalidate();
	}
}
