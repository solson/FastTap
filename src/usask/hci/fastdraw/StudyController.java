package usask.hci.fastdraw;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public class StudyController {
	private final String[][] mTrials;
	private int mCurrTrial;
	private Set<String> mToSelect;
	private StudyLogger mLog;
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

		selectTask(0);
	}
	
	public void handleSelected(String selection) {
		if (mToSelect.contains(selection)) {
			mToSelect.remove(selection);

			if (mToSelect.isEmpty()) {
				long now = System.nanoTime();
				
				int numTargets = mTrials[mCurrTrial].length;
				StringBuilder targetString = new StringBuilder();
				for (int i = 0; i < numTargets; i++) {
					if (i != 0)
						targetString.append(" ");
					targetString.append(mTrials[mCurrTrial][i]);
				}

				mLog.task(mCurrTrial + 1, mTrials[mCurrTrial].length, targetString.toString(), mNumErrors, mErrors.toString(), mTimesOverlayShown, now - mTrialStart);

				selectTask((mCurrTrial + 1) % mTrials.length);
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
		String progress = "(" + (mCurrTrial + 1) + "/" + mTrials.length + ")";
    	StringBuilder title = new StringBuilder(progress + " Please select: ");
    	
    	for (String toSelect : mToSelect) {
    		title.append(toSelect);
    		title.append(", ");
    	}
    	
    	return title.substring(0, title.length() - 2);
	}
	
	private void selectTask(int taskIndex) {
		mTrialStart = System.nanoTime();
		mTimesOverlayShown = 0;
		mNumErrors = 0;
		mErrors = new StringBuilder();
		mCurrTrial = taskIndex;
		mToSelect = new LinkedHashSet<String>(Arrays.asList(mTrials[mCurrTrial]));
	}
}
