package usask.hci.fastdraw;

import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.widget.CheckBox;
import android.widget.NumberPicker;

public class DrawView extends View {
    private MainActivity mMainActivity;
    private StudyLogger mLog;
    private Bitmap mBitmap;
    private Paint mBitmapPaint;
    private final int mCols = 4;
    private final int mRows = 5;
    private float mColWidth;
    private float mRowHeight;
    private boolean mShowOverlay;
    private long mOverlayStart;
    private Paint mPaint;
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
    private static final int mChordDelay = 1000 * 1000 * 200; // 200 ms in ns
    private static final int mFlashDelay = 1000 * 1000 * 500; // 500 ms in ns
    private static final int mOverlayButtonIndex = 16;
    
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
        public Bitmap icon;
        
        public Selection(Object object, String name, int iconResource, SelectionType type) {
            this.object = object;
            this.name = name;
            this.type = type;
            this.icon = BitmapFactory.decodeResource(getResources(), iconResource);
        }
    }

    public DrawView(Context mainActivity) {
        super(mainActivity);
        
        if (!(mainActivity instanceof MainActivity)) {
            Log.e("DrawView", "DrawView was not given the MainActivity");
            return;
        }

        mMainActivity = (MainActivity) mainActivity;
        mLog = new StudyLogger(mainActivity);
        mBitmapPaint = new Paint(Paint.DITHER_FLAG);
        mPaint = new Paint();
        mPaint.setTextSize(26);
        mPaint.setTextAlign(Align.CENTER);
        mPaint.setAntiAlias(true);
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
            new Selection(new PaintTool(this), "Paintbrush", R.drawable.paintbrush, SelectionType.TOOL),
            new Selection(new LineTool(this), "Line", R.drawable.line, SelectionType.TOOL),
            new Selection(new CircleTool(this), "Circle", R.drawable.circle, SelectionType.TOOL),
            new Selection(new RectangleTool(this), "Rectangle", R.drawable.rectangle, SelectionType.TOOL),
            
            new Selection(Color.BLACK, "Black", R.drawable.black, SelectionType.COLOR),
            new Selection(Color.RED, "Red", R.drawable.red, SelectionType.COLOR),
            new Selection(Color.GREEN, "Green", R.drawable.green, SelectionType.COLOR),
            new Selection(Color.BLUE, "Blue", R.drawable.blue, SelectionType.COLOR),
            
            new Selection(Color.WHITE, "White", R.drawable.white, SelectionType.COLOR),
            new Selection(Color.YELLOW, "Yellow", R.drawable.yellow, SelectionType.COLOR),
            new Selection(Color.CYAN, "Cyan", R.drawable.cyan, SelectionType.COLOR),
            new Selection(Color.MAGENTA, "Magenta", R.drawable.magenta, SelectionType.COLOR),

            new Selection(1, "Fine", R.drawable.fine, SelectionType.THICKNESS),
            new Selection(6, "Thin", R.drawable.thin, SelectionType.THICKNESS),
            new Selection(16, "Medium", R.drawable.medium, SelectionType.THICKNESS),
            new Selection(50, "Wide", R.drawable.wide, SelectionType.THICKNESS),
            
            null, // The position of the command map button
            new Selection(Action.SAVE, "Save", R.drawable.save, SelectionType.ACTION),
            new Selection(Action.CLEAR, "Clear", R.drawable.clear, SelectionType.ACTION),
            new Selection(Action.UNDO, "Undo", R.drawable.undo, SelectionType.ACTION)
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
                }
            }
        }, 25, 25);
        
        View studySetupLayout = mMainActivity.getLayoutInflater().inflate(R.layout.study_setup, null);
        final CheckBox leftHandedCheckBox = (CheckBox) studySetupLayout.findViewById(R.id.left_handed_checkbox);
        final CheckBox permanentGridCheckBox = (CheckBox) studySetupLayout.findViewById(R.id.permanent_grid_checkbox);
        
        final NumberPicker subjectIdPicker = (NumberPicker) studySetupLayout.findViewById(R.id.subject_id_picker);
        subjectIdPicker.setMinValue(0);
        subjectIdPicker.setMaxValue(99);
        subjectIdPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS); // Remove the virtual keyboard

        new AlertDialog.Builder(mainActivity)
            .setMessage(R.string.dialog_study_mode)
            .setCancelable(false)
            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    new AlertDialog.Builder(mMainActivity)
                        .setMessage("Press OK when you are ready to begin.")
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                mLog.event("New session started");
                            }
                        })
                        .show();
                    
                    mLog.setSubjectId(subjectIdPicker.getValue());
                    mLeftHanded = leftHandedCheckBox.isChecked();
                    mPermanentGrid = permanentGridCheckBox.isChecked();
                    DrawView.this.invalidate();
                }
            })
            .setView(studySetupLayout)
            .show();

        mMainActivity.getActionBar().setIcon(R.drawable.trans);
    }
    
    public void alert(String message) {
        new AlertDialog.Builder(mMainActivity)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton(android.R.string.yes, null)
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

        if (mFingerInside != -1 || mFlashTimes.size() > 0)
            mPaint.setColor(0xEEDDDD88);
        else
            mPaint.setColor(0xEEFFFFAA);
        
        canvas.drawRect(bounds, mPaint);
        
        if (mShowOverlay || mPermanentGrid || mFlashTimes.size() > 0) {
            mPaint.setColor(0x44666666);

            for (int i = 0; i < mRows; i++) {
                float top = i * mRowHeight;
                canvas.drawLine(0, top, mColWidth * mCols, top, mPaint);
            }
            for (int i = 0; i < mCols; i++) {
                float left = i * mColWidth;
                canvas.drawLine(left, 0, left, mRowHeight * mRows, mPaint);
            }
        }
        
        if (mShowOverlay) {
            mPaint.setColor(0xFF000000);
            for (int y = 0; y < mRows; y++) {
                for (int x = 0; x < mCols; x++) {
                    int realX = x;
                    if (mLeftHanded)
                        realX = mCols - x - 1;
                    
                    int i = y * mCols + realX;
                    if (mSelections[i] != null && mFlashTimes.get(i) == null) {
                        String name = mSelections[i].name;
                        int heightAdj = getTextHeight(name, mPaint) / 2;
                        float centerX = (x + 0.5f) * mColWidth;
                        float centerY = (y + 0.5f) * mRowHeight;
                        
                        Bitmap icon = mSelections[i].icon;
                        float iconWidth = icon.getWidth();
                        float iconHeight = icon.getHeight();
                        
                        canvas.drawBitmap(icon, centerX - iconWidth / 2, centerY - iconHeight * 3 / 4, mPaint);
                        canvas.drawText(name, centerX, centerY + iconHeight / 2 + heightAdj, mPaint);
                    }
                }
            }
        } else if (!mPermanentGrid) {
            mPaint.setColor(0x44666666);
            canvas.drawLine(bounds.left, bounds.top, bounds.right, bounds.top, mPaint);
            
            if (mLeftHanded)
                canvas.drawLine(bounds.left, bounds.top, bounds.left, bounds.bottom, mPaint);
            else
                canvas.drawLine(bounds.right, bounds.top, bounds.right, bounds.bottom, mPaint);
        }
        
        synchronized (mFlashTimes) {
            for (int i = 0; i < mFlashTimes.size(); i++) {
                int selectionNum = mFlashTimes.keyAt(i);
                Selection selection = mSelections[selectionNum];
                if (selection != null) {
                    RectF buttonBounds = getButtonBounds(selectionNum);
                    
                    mPaint.setColor(0xCCE5E5E5);
                    canvas.drawRect(buttonBounds, mPaint);
                    
                    mPaint.setColor(0x44666666);
                    mPaint.setStyle(Style.STROKE);
                    canvas.drawRect(buttonBounds, mPaint);
                    mPaint.setStyle(Style.FILL);
                    
                    Bitmap icon = selection.icon;
                    float iconWidth = icon.getWidth();
                    float iconHeight = icon.getHeight();
                    float centerX = buttonBounds.left + 0.5f * mColWidth;
                    float centerY = buttonBounds.top + 0.5f * mRowHeight;
                    
                    mPaint.setColor(0xFF000000);
                    String name = selection.name;
                    int heightAdj = getTextHeight(name, mPaint) / 2;
                    
                    canvas.drawBitmap(icon, centerX - iconWidth / 2, centerY - iconHeight * 3 / 4, mPaint);
                    canvas.drawText(name, centerX, centerY + iconHeight / 2 + heightAdj, mPaint);
                }
            }
        }
        
        mPaint.setColor(0xFF666666);
        
        canvas.drawText(mThicknessName, bounds.left + mColWidth / 2,
                bounds.top + mRowHeight / 2 + getTextHeight(mThicknessName, mPaint) / 2 - 30, mPaint);
        canvas.drawText(mColorName, bounds.left + mColWidth / 2,
                bounds.top + mRowHeight / 2 + getTextHeight(mColorName, mPaint) / 2, mPaint);
        canvas.drawText(mToolName, bounds.left + mColWidth / 2,
                bounds.top + mRowHeight / 2 + getTextHeight(mToolName, mPaint) / 2 + 30, mPaint);
    }
    
    private int getTextHeight(String text, Paint paint) {
        mPaint.getTextBounds(text, 0, text.length(), mTextBounds);
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
                    float x2 = event.getX(i);
                    float y2 = event.getY(i);
                    
                    if(mIgnoredFingers.contains(fingerId))
                        continue;
                    
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

                if (id == mFingerInside) {
                    mFingerInside = -1;
                }

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
                    if (event.getPointerCount() == 1 && mChanged) {
                        mUndo = mNextUndo;
                    }
                    
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
        
        if (fromUser)
            mLog.event("Target selected: " + selectionTypeName(selection.type) + " / " + selection.name);
        
        switch (selection.type) {
            case TOOL:
                mTool = (Tool) selection.object;
                mToolName = selection.name;
                break;
                
            case COLOR:
                mColor = (Integer) selection.object;
                mColorName = selection.name;
                break;
                
            case THICKNESS:
                mThickness = (Integer) selection.object;
                mThicknessName = selection.name;
                break;
                
            case ACTION:
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
    }
    
    private String selectionTypeName(SelectionType type) {
        switch (type) {
            case TOOL: return "Tool";
            case COLOR: return "Color";
            case ACTION: return "Action";
            case THICKNESS: return "Thickness";
            default: return "Unknown";
        }
    }
}
