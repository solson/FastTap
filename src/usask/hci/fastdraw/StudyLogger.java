package usask.hci.fastdraw;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Environment;
import android.util.Log;

public class StudyLogger {
	private File mFile;
	private Context mContext;
	
	public StudyLogger(Context c) {
		mContext = c;
		
		File dir = new File(Environment.getExternalStorageDirectory() + "/Fast Draw");
		dir.mkdirs();
		mFile = new File(dir, "study.txt");
	}
	
	public void log(String message) {
    	long milliseconds = System.nanoTime() / 1000 / 1000;
    	Log.i("FastDraw", milliseconds + ": " + message);
    	
		Writer s;
		
		try {
			s = new OutputStreamWriter(new FileOutputStream(mFile, true));
		} catch (FileNotFoundException e) {
			notifyError(e);
			return;
		}

		try {
			s.write(message + "\r\n");
			s.flush();
		} catch (IOException e) {
			notifyError(e);
		} finally {
			try { s.close(); } catch (IOException e) {}
		}
	}
	
	private void notifyError(Exception e) {
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		
        new AlertDialog.Builder(mContext)
        	.setTitle(R.string.dialog_logging_error)
        	.setMessage(sw.toString())
        	.setPositiveButton(android.R.string.ok, null)
        	.show();
	}
}
