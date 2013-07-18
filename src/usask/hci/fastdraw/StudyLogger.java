package usask.hci.fastdraw;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Environment;
import android.util.Log;

public class StudyLogger {
    private File mLogDir;
    private Context mContext;
    private int mSubjectId;
    private Date mStart;
    private final char columnSeparator = '\t';
    
    public StudyLogger(Context c) {
        mContext = c;
        mStart = new Date();
        mSubjectId = -1;
        mLogDir = new File(Environment.getExternalStorageDirectory() + "/Fast Draw");
    }

    public void setSubjectId(int subjectId) {
        mSubjectId = subjectId;
    }

    // Log format:
    // sid, timestamp, ui type, set, block, trial, #targets, targets, #errors, errors, #times painted, time ui shown, duration of trial
    public void chordTrial(long timeNs, int setNum, int blockNum, int trialNum, int numTargets, String targets, int numErrors,
            String errors, int timesPainted, long uiTimeNs, long durationNs) {
        long durationMs = durationNs / 1000000;
        long timeMs = timeNs / 1000000;
        long uiTimeMs = uiTimeNs / 1000000;
        char s = columnSeparator;
        
        log("ChordTrial", "s" + mSubjectId + s + timeMs + s + "chord" + s + setNum + s +
                blockNum + s + trialNum + s + numTargets + s + targets + s + numErrors +
                s + errors + s + timesPainted + s + uiTimeMs + s + durationMs);
    }
    
    // Log format:
    // sid, timestamp, ui type, set, block, trial, #targets, targets, #errors, errors, #times painted, time ui shown, duration of trial
    public void gestureTrial(long timeNs, int setNum, int blockNum, int trialNum, int numTargets, String targets, int numErrors,
            String errors, int timesPainted, long uiTimeNs, long durationNs) {
        long durationMs = durationNs / 1000000;
        long timeMs = timeNs / 1000000;
        long uiTimeMs = uiTimeNs / 1000000;
        char s = columnSeparator;

        log("GestureTrial", "s" + mSubjectId + s + timeMs + s + "gesture" + s + setNum + s +
                blockNum + s + trialNum + s + numTargets + s + targets + s + numErrors + s +
                errors + s + timesPainted + s + uiTimeMs + s + durationMs);
    }

    // Log format:
    // sid, timestamp, event message
    public void event(String message) {
        long timeMs = System.nanoTime() / 1000000;
        log("Event", mSubjectId + "," + timeMs + "," + message);
    }

    private void log(String type, String message) {
        if (mSubjectId == -1)
            return;
        
        Log.i("FastDraw" + type, message);
        
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd HH.mm.ss", Locale.US);
        File dir = new File(mLogDir, mSubjectId + " - " + formatter.format(mStart));
        dir.mkdirs();
        
        formatter = new SimpleDateFormat("yyyy.MM.dd HH.mm.ss", Locale.US);
        File file = new File(dir, mSubjectId + " - " + type + " - " + formatter.format(mStart) + ".txt");

        Writer s;
        try {
            s = new OutputStreamWriter(new FileOutputStream(file, true));
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
