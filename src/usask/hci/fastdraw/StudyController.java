package usask.hci.fastdraw;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import android.util.Log;

public class StudyController {
	private final String[][] mTasks;
	private int mCurrTaskIdx;
	private Set<String> mCurrTask;

	public StudyController() {
		mTasks = new String[][] {
			{"Normal", "Red", "Line"},
			{"Thin", "Yellow"},
			{"Circle", "Save"}
		};

		mCurrTaskIdx = 0;
		mCurrTask = new LinkedHashSet<String>(Arrays.asList(mTasks[mCurrTaskIdx]));
	}
	
	public void handleSelected(String selection) {
		mCurrTask.remove(selection);
		
		if (mCurrTask.isEmpty()) {
			Log.i("logger", "finished " + mCurrTaskIdx);
			mCurrTaskIdx = (mCurrTaskIdx + 1) % mTasks.length;
			mCurrTask = new LinkedHashSet<String>(Arrays.asList(mTasks[mCurrTaskIdx]));
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
