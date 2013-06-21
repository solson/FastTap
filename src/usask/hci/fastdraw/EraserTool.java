package usask.hci.fastdraw;

import android.graphics.Color;

public class EraserTool extends PenTool {
	public EraserTool(DrawView drawView) {
		super(drawView, 75);
	}

	@Override
	public String getName() {
		return "Eraser";
	}
	
	@Override
	public int getColor() {
		return Color.WHITE;
	}
}
