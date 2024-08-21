package tileWorld;

import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;

public class Obstacle {
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	
	public Obstacle(ContinuousSpace<Object> space, Grid<Object> grid) {
		this.space = space;
		this.grid = grid;
	}
}
