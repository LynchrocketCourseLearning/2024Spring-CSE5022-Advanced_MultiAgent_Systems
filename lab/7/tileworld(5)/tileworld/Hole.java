package tileworld;

import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;

public class Hole {
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	private int score;
	
	public Hole(ContinuousSpace<Object> space, Grid<Object> grid, int score) {
		this.space = space;
		this.grid = grid;
		this.score = score;
	}
	public int get_score() {
		return this.score;
	}
	

}
