package jzombies;

import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;

public class Shelter {
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	private int humanCount;
	
	public Shelter(ContinuousSpace<Object> space, Grid<Object> grid) {
		this.space = space;
		this.grid = grid;
		this.humanCount = 0;
	}
}
