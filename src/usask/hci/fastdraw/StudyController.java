package usask.hci.fastdraw;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class StudyController {
	private final String[][] mTrials;
	private final StudyLogger mLog;
	private int mTrialNum;
	private int mTrialIndex;
	private int mBlockNum;
	private Set<String> mToSelect;
	private Integer[] mTrialOrder;
	private long mTrialStart;
	private int mNumErrors;
	private StringBuilder mErrors;
	private int mTimesOverlayShown;

	public StudyController(StudyLogger logger) {
		mLog = logger;

		mTrials = new String[][] {
			{"Normal", "Red", "Line"},
			{"Thin", "Yellow", "Rectangle"},
			{"Fine", "White", "Circle"},
			{"Wide", "Magenta", "Line"},
			{"Normal", "Cyan", "Paintbrush"},
			{"Fine", "Green", "Rectangle"},
			{"Wide", "White", "Paintbrush"},
			{"Normal", "Blue", "Circle"},
			{"Wide", "Yellow", "Line"},
			{"Thin", "Black", "Paintbrush"}
		};

		mBlockNum = 0;
		
		mTrialOrder = new Integer[mTrials.length];
		for (int i = 0; i < mTrialOrder.length; i++)
			mTrialOrder[i] = i;
		
		nextBlock();
	}
	
	public void handleSelected(String selection) {
		if (mToSelect.contains(selection)) {
			mToSelect.remove(selection);

			if (mToSelect.isEmpty()) {
				long now = System.nanoTime();
				
				int numTargets = mTrials[mTrialIndex].length;
				StringBuilder targetString = new StringBuilder();
				for (int i = 0; i < numTargets; i++) {
					if (i != 0)
						targetString.append(" ");
					targetString.append(mTrials[mTrialIndex][i]);
				}

				mLog.trial(mBlockNum, mTrialNum, mTrials[mTrialIndex].length, targetString.toString(), mNumErrors, mErrors.toString(), mTimesOverlayShown, now - mTrialStart);
				
				if (mTrialNum < mTrials.length)
					nextTrial();
				else
					nextBlock();
			}
		} else {
			if (mNumErrors != 0)
				mErrors.append(" ");
			
			mErrors.append(selection);
			mNumErrors++;
		}
	}
	
	public void handleOverlayShown() {
		mTimesOverlayShown++;
	}
	
	public String getPrompt() {
		String progress = "(" + mTrialNum + "/" + mTrials.length + ")";
    	StringBuilder title = new StringBuilder(progress + " Please select: ");
    	
    	for (String toSelect : mToSelect) {
    		title.append(toSelect);
    		title.append(", ");
    	}
    	
    	return title.substring(0, title.length() - 2);
	}
	
	private void nextBlock() {
		Collections.shuffle(Arrays.asList(mTrialOrder));
		mBlockNum++;

		mTrialNum = 0;
		nextTrial();
	}
	
	private void nextTrial() {
		mTrialStart = System.nanoTime();
		mTimesOverlayShown = 0;
		mNumErrors = 0;
		mErrors = new StringBuilder();
		
		mTrialIndex = mTrialOrder[mTrialNum];
		mTrialNum++;
		
		mToSelect = new LinkedHashSet<String>(Arrays.asList(mTrials[mTrialIndex]));
	}
}
