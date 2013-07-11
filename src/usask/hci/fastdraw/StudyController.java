package usask.hci.fastdraw;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class StudyController {
	private final String[][][] mTrials;
	private final StudyLogger mLog;
	private int mTrialNum;
	private int mTrialIndex;
	private int mBlockNum;
	private int mSetIndex;
	private Set<String> mToSelect;
	private Integer[] mTrialOrder;
	private long mTrialStart;
	private int mNumErrors;
	private StringBuilder mErrors;
	private int mTimesOverlayShown;
	private int[] mBlocksPerSet;

	public StudyController(StudyLogger logger) {
		mLog = logger;

		mTrials = new String[][][] {
			{{"Paintbrush"}, {"Line"}, {"Circle"}, {"Rectangle"},
			{"Black"}, {"Red"}, {"Green"}, {"Blue"},
			{"White"}, {"Yellow"}, {"Cyan"}, {"Magenta"},
			{"Fine"}, {"Thin"}, {"Normal"}, {"Wide"}},
			
			{{"Normal", "Circle"},
			{"Blue", "Rectangle"},
			{"Fine", "White"},
			{"Wide", "Line"},
			{"Cyan", "Paintbrush"},
			{"Fine", "Green"},
			{"Yellow", "Paintbrush"},
			{"Magenta", "Circle"},
			{"Normal", "Red"},
			{"Thin", "Black"}},
			
			{{"Normal", "Red", "Line"},
			{"Thin", "Yellow", "Rectangle"},
			{"Fine", "White", "Circle"},
			{"Wide", "Magenta", "Line"},
			{"Normal", "Cyan", "Paintbrush"},
			{"Fine", "Green", "Rectangle"},
			{"Wide", "White", "Paintbrush"},
			{"Normal", "Blue", "Circle"},
			{"Wide", "Yellow", "Line"},
			{"Thin", "Black", "Paintbrush"}}
		};
		
		mBlocksPerSet = new int[] { 5, 5, 5 };

		mSetIndex = 0;
		mBlockNum = 0;
		
		nextBlock();
	}
	
	public void handleSelected(String selection) {
		if (mToSelect.contains(selection)) {
			mToSelect.remove(selection);

			if (mToSelect.isEmpty()) {
				long now = System.nanoTime();
				
				int numTargets = mTrials[mSetIndex][mTrialIndex].length;
				StringBuilder targetString = new StringBuilder();
				for (int i = 0; i < numTargets; i++) {
					if (i != 0)
						targetString.append(" ");
					targetString.append(mTrials[mSetIndex][mTrialIndex][i]);
				}

				mLog.trial(mBlockNum, mTrialNum, mTrials[mSetIndex][mTrialIndex].length,
						targetString.toString(), mNumErrors, mErrors.toString(),
						mTimesOverlayShown, now - mTrialStart);
				
				nextTrial();
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
		String progress = "#" + mBlockNum + " (" + mTrialNum + "/" + mTrials[mSetIndex].length + ")";
    	StringBuilder title = new StringBuilder(progress + " Please select: ");
    	
    	for (String toSelect : mToSelect) {
    		title.append(toSelect);
    		title.append(", ");
    	}
    	
    	return title.substring(0, title.length() - 2);
	}
	
	private void nextSet() {
		mSetIndex = (mSetIndex + 1) % mTrials.length;
		mBlockNum = 0;
		nextBlock();
	}
	
	private void nextBlock() {
		if (mBlockNum >= mBlocksPerSet[mSetIndex]) {
			nextSet();
			return;
		}
		
		mTrialOrder = new Integer[mTrials[mSetIndex].length];
		for (int i = 0; i < mTrialOrder.length; i++)
			mTrialOrder[i] = i;
		Collections.shuffle(Arrays.asList(mTrialOrder));
		
		mBlockNum++;
		mTrialNum = 0;
		nextTrial();
	}
	
	private void nextTrial() {
		if (mTrialNum >= mTrials[mSetIndex].length) {
			nextBlock();
			return;
		}
		
		mTrialStart = System.nanoTime();
		mTimesOverlayShown = 0;
		mNumErrors = 0;
		mErrors = new StringBuilder();

		mTrialIndex = mTrialOrder[mTrialNum];
		mTrialNum++;
		
		mToSelect = new LinkedHashSet<String>(Arrays.asList(mTrials[mSetIndex][mTrialIndex]));
	}
}
