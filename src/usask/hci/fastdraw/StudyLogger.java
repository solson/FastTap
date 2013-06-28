package usask.hci.fastdraw;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.os.Environment;

public class StudyLogger {
	private File mFile;
	private Context mContext;
	
	public StudyLogger(Context c) {
		mContext = c;
		
		File dir = new File(Environment.getExternalStorageDirectory() + "/Fast Draw");
		dir.mkdirs();
		
		mFile = new File(dir, "study.txt");
		
		try {
			mFile.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		// Workaround for an Android bug to force the file to be rescanned
		// so it shows up on a computer.
		MediaScannerConnection.scanFile(mContext, new String[] { mFile.getAbsolutePath() }, null, null);
	}
	
	public void log(String message) {
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
