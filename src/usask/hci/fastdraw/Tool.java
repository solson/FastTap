package usask.hci.fastdraw;

import android.graphics.Canvas;

public abstract class Tool {
	private DrawView mDrawView;
	
	public Tool(DrawView drawView) {
		mDrawView = drawView;
	}
	
	public int getColor() {
		return mDrawView.getColor();
	}
	
	public int getThickness() {
		return mDrawView.getThickness();
	}
	
	public abstract void touchStart(int id, float x, float y);
	public abstract void touchMove(int id, float x, float y);
	public abstract void touchStop(int id, float x, float y, Canvas canvas);
	public abstract void clearFingers();
	public abstract void draw(Canvas canvas);
}
