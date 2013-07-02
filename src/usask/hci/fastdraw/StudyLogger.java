package usask.hci.fastdraw;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import android.os.Environment;
import android.util.Log;

public class StudyLogger {
	private File mFile;
	
	public StudyLogger() {
		File dir = new File(Environment.getExternalStorageDirectory() + "/Fast Draw");
		dir.mkdirs();
		
		mFile = new File(dir, "study.txt");
		
		try {
			mFile.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
	}
	
	public void log(String message) {
    	long milliseconds = System.nanoTime() / 1000 / 1000;
    	Log.i("FastDraw", milliseconds + ": " + message);
    	
		Writer s;
		
		try {
			s = new OutputStreamWriter(new FileOutputStream(mFile, true));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}

		try {
			s.write(message + "\r\n");
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try { s.close(); } catch (IOException e) {}
		}
	}
}
