package tileWorld;

import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;

public class Home {
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	
	public Home(ContinuousSpace<Object> space, Grid<Object> grid) {
		this.space = space;
		this.grid = grid;
	}
}
