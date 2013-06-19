package usask.hci.fastdraw;

import android.graphics.Color;

public class EraserTool extends PenTool {
	public EraserTool() {
		super(Color.WHITE, 75);
	}

	@Override
	public String getName() {
		return "Eraser";
	}
}
