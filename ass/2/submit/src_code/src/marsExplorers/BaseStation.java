package marsExplorers;

import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;

public class BaseStation {
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;

	public BaseStation(ContinuousSpace<Object> space, Grid<Object> grid) {
		this.space = space;
		this.grid = grid;
	}
}