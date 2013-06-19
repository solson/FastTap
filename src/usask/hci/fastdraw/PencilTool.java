package usask.hci.fastdraw;

public class PencilTool extends PenTool {
	public PencilTool(int color) {
		super(color, 1);
	}

	@Override
	public String getName() {
		return "Pencil";
	}
}
