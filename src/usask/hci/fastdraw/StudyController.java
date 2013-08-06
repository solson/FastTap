package usask.hci.fastdraw;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import android.graphics.Color;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StrikethroughSpan;

public class StudyController {
    private final String[][] mTrials;
    private final StudyLogger mLog;
    private int mTrialNum;
    private int mTrialIndex;
    private int mBlockNum;
    private Set<String> mToSelect;
    private Set<String> mSelected;
    private Integer[] mTrialOrder;
    private long mTrialStart;
    private int mNumErrors;
    private StringBuilder mErrors;
    private int mTimesPainted;
    private boolean mFinished;
    private long mUITime;
    private boolean mWaiting;
    private int mNumWaitDots;
    private boolean mHideTargets;
    private final int mNumBlocks = 11;
    
    public StudyController(StudyLogger logger) {
        mLog = logger;

        mTrials = new String[][] {
            {"Paintbrush"},
            {"Rectangle"},
            {"Black"},
            {"Red"},
            {"Glowing"},
            {"Blurred"},
            {"Thin"},
            {"Wide"},
            
            {"Black", "Paintbrush"},
            {"Blurred", "Paintbrush"},
            {"Thin", "Red"},
            {"Wide", "Black"},
            
            {"Glowing", "Red", "Rectangle"},
            {"Thin", "Blurred", "Rectangle"},
            {"Thin", "Red", "Rectangle"},
            {"Wide", "Glowing", "Paintbrush"},
        };

        mSelected = new HashSet<String>();
        mBlockNum = 0;
        mFinished = false;
        mWaiting = false;
        mNumWaitDots = 0;
        mHideTargets = false;
        
        nextBlock();
    }
    
    public void hideTargets() {
        mHideTargets = true;
    }
    
    public void waitStep(boolean goNextTrial) {
        if (!mWaiting) {
            mWaiting = true;
        } else if (mNumWaitDots == 3) {
            mWaiting = false;
            mNumWaitDots = 0;
            
            if (mHideTargets)
                mHideTargets = false;
            
            if (goNextTrial)
                nextTrial();
        } else {
            mNumWaitDots++;
        }
    }
    
    public void finish() {
        mFinished = true;
    }
    
    public boolean isFinished() {
        return mFinished;
    }
    
    public long getTrialStart() {
        return mTrialStart;
    }
    
    public int getNumBlocks() {
        return mNumBlocks;
    }
    
    public void setBlockNum(int block) {
        mBlockNum = block - 1;
        nextBlock();
    }
    
    public void incrementTimesPainted() {
        mTimesPainted++;
    }
    
    // Return whether the selection was one of the current targets.
    public boolean handleSelected(String selection, boolean gesture) {
        if (mFinished)
            return false;
        
        if (mToSelect.contains(selection) && !mSelected.contains(selection)) {
            mSelected.add(selection);

            if (mToSelect.size() == mSelected.size()) {
                long now = System.nanoTime();

                int numTargets = mTrials[mTrialIndex].length;
                StringBuilder targetString = new StringBuilder();
                for (int i = 0; i < numTargets; i++) {
                    if (i != 0)
                        targetString.append(",");
                    targetString.append(mTrials[mTrialIndex][i]);
                }

                if (gesture) {
                    mLog.gestureTrial(now,  mBlockNum, mTrialNum, mTrials[mTrialIndex].length,
                            targetString.toString(), mNumErrors, mErrors.toString(),
                            mTimesPainted, mUITime, now - mTrialStart, mBlockNum == 1);
                } else {
                    mLog.chordTrial(now, mBlockNum, mTrialNum, mTrials[mTrialIndex].length,
                            targetString.toString(), mNumErrors, mErrors.toString(),
                            mTimesPainted, mUITime, now - mTrialStart, mBlockNum == 1);
                }
            }
            
            return true;
        } else {
            if (mNumErrors != 0)
                mErrors.append(",");
            
            mErrors.append(selection);
            mNumErrors++;
            
            return false;
        }
    }
    
    public void addUITime(long duration) {
        if (!mWaiting)
            mUITime += duration;
    }
    
    public CharSequence getPrompt() {
        if (mFinished)
            return "You are finished!";
        
        String progress = "#" + mBlockNum + " (" + mTrialNum + "/" + mTrials.length + ")";
        
        SpannableStringBuilder title = new SpannableStringBuilder(progress + " Please select: ");
        
        if (!mHideTargets) {
            int index = 0;
            int last = mToSelect.size() - 1;
            for (String toSelect : mToSelect) {
                SpannableString text = new SpannableString(toSelect);
    
                if (mSelected.contains(toSelect)) {
                    text.setSpan(new StrikethroughSpan(), 0, text.length(), 0);
                    text.setSpan(new ForegroundColorSpan(Color.DKGRAY), 0, text.length(), 0);
                }
    
                title.append(text);
                
                if (index != last) {
                    if (mSelected.contains(toSelect)) {
                        SpannableString comma = new SpannableString(", ");
                        comma.setSpan(new ForegroundColorSpan(Color.DKGRAY), 0, comma.length(), 0);
                        title.append(comma);
                    } else {
                        title.append(", ");
                    }
                }
                
                index++;
            }
        }
        
        if (mWaiting) {
            title.append(" ");
            
            for (int i = 0; i < mNumWaitDots; i++)
                title.append(".");
        }
        
        return title;
    }
    
    private void nextBlock() {
        if (mBlockNum >= mNumBlocks) {
            mFinished = true;
            return;
        }
        
        mTrialOrder = new Integer[mTrials.length];
        for (int i = 0; i < mTrialOrder.length; i++)
            mTrialOrder[i] = i;
        Collections.shuffle(Arrays.asList(mTrialOrder));
        
        mBlockNum++;
        mTrialNum = 0;
        nextTrial();
    }
    
    public void nextTrial() {
        if (mTrialNum >= mTrials.length) {
            nextBlock();
            return;
        }
        
        mTrialStart = System.nanoTime();
        mUITime = 0;
        mNumErrors = 0;
        mTimesPainted = 0;
        mErrors = new StringBuilder();

        mTrialIndex = mTrialOrder[mTrialNum];
        mTrialNum++;
        
        mToSelect = new LinkedHashSet<String>(Arrays.asList(mTrials[mTrialIndex]));
        mSelected.clear();
    }

    public boolean isOnLastTarget() {
        return mToSelect.size() - mSelected.size() == 1;
    }

    public boolean isOnLastTrial() {
        return mTrialNum == mTrials.length;
    }

    public boolean isOnLastBlock() {
        return mBlockNum == mNumBlocks;
    }
}
