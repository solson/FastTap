package usask.hci.fastdraw;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public class StudyController {
	private final String[][] mTasks;
	private int mCurrTaskIdx;
	private Set<String> mCurrTask;
	private StudyLogger mLog;

	public StudyController(StudyLogger logger) {
		mLog = logger;

		mTasks = new String[][] {
			{"Normal", "Red", "Line"},
			{"Thin", "Yellow"},
			{"Circle", "Save"}
		};

		mCurrTaskIdx = 0;
		mCurrTask = new LinkedHashSet<String>(Arrays.asList(mTasks[mCurrTaskIdx]));
	}
	
	public void handleSelected(String selection) {
		if (mCurrTask.contains(selection)) {
			mCurrTask.remove(selection);

			if (mCurrTask.isEmpty()) {
				mCurrTaskIdx = (mCurrTaskIdx + 1) % mTasks.length;
				mCurrTask = new LinkedHashSet<String>(Arrays.asList(mTasks[mCurrTaskIdx]));
			}
		} else {
			mLog.log("Selection error: " + selection);
		}
	}
	
	public String getPrompt() {
		String progress = "(" + (mCurrTaskIdx + 1) + "/" + mTasks.length + ")";
    	StringBuilder title = new StringBuilder(progress + " Please select: ");
    	
    	for (String toSelect : mCurrTask) {
    		title.append(toSelect);
    		title.append(", ");
    	}
    	
    	return title.substring(0, title.length() - 2);
	}
}
