package usask.hci.fastdraw;

import android.graphics.Canvas;

public interface Tool {
	public void touchStart(int id, float x, float y, Canvas canvas);
	public void touchMove(int id, float x, float y, Canvas canvas);
	public void touchStop(int id, float x, float y, Canvas canvas);
	public void clearFingers();
	public String getName();
}
