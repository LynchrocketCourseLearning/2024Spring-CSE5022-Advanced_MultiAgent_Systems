package jzombies;

import java.util.List;

import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.engine.watcher.Watch;
import repast.simphony.engine.watcher.WatcherTriggerSchedule;
import repast.simphony.query.space.grid.GridCell;
import repast.simphony.query.space.grid.GridCellNgh;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.SpatialMath;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import repast.simphony.util.SimUtilities;

public class Human {
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	private int energy, startingEnergy;
	private Boolean isSafe;

	public Human(ContinuousSpace<Object> space, Grid<Object> grid, int energy) {
		this.space = space;
		this.grid = grid;
		this.energy = startingEnergy = energy;
		this.isSafe = false;
	}

//	@Watch(watcheeClassName = "jzombies.Zombie", watcheeFieldNames = "moved", query = "within_moore 1", whenToTrigger = WatcherTriggerSchedule.IMMEDIATE)
	public void run() {
		GridPoint pt = grid.getLocation(this);
		GridCellNgh<Zombie> nghCreator = new GridCellNgh<Zombie>(grid, pt, Zombie.class, 1, 1);
		List<GridCell<Zombie>> gridCells = nghCreator.getNeighborhood(true);
		SimUtilities.shuffle(gridCells, RandomHelper.getUniform());

		GridPoint pointWithLeastZombies = null;
		int minCount = Integer.MAX_VALUE;
		for(GridCell<Zombie> cell : gridCells) {
			if (cell.size() < minCount) {
				pointWithLeastZombies = cell.getPoint();
				minCount = cell.size();
			}
		}
		
		if(energy > 0) {
			moveTowards(pointWithLeastZombies);
		}else {
			energy = startingEnergy;
		}
	}
	
	@ScheduledMethod(start=1, interval=1)
	public void findShelters() {
		//Get the location of this zombie within the grid
		GridPoint pt = grid.getLocation(this);
		// Get the neighboring cells around a single human
		GridCellNgh<Shelter> nghCreator = new GridCellNgh<>(grid, pt, Shelter.class, 1, 1);
		List<GridCell<Shelter>> gridCells = nghCreator.getNeighborhood(true);
		SimUtilities.shuffle(gridCells, RandomHelper.getUniform());

		//Iterating around a list of cells containing only Humans, and determining only the one with Humans
		GridPoint pointWithShelter = null;
		int maxCount = -1;
		for (GridCell<Shelter> cell : gridCells) {
			if(cell.size() > maxCount) {
				pointWithShelter = cell.getPoint();
				maxCount = cell.size();
			}
		}
		
		if(!this.isSafe) {
			List<Object> objects = grid.getObjectsAt(pt.getX(), pt.getY());
			for (Object obj : gridCells) {
				
			}
			if (this.energy > 0) {
				moveTowards(pointWithShelter);
			}else {
				this.energy = this.startingEnergy;
			}
		}
	}
	
	public boolean isSafe() {
		return this.isSafe;
	}

	public void moveTowards(GridPoint pt) {
		//verify if Human is not in the Point of Interest
		if(!pt.equals(grid.getLocation(this))) {
			//Zombie current location
			NdPoint mypoint = space.getLocation(this);
			NdPoint otherpoint = new NdPoint(pt.getX(), pt.getY());
			double angle = SpatialMath.calcAngleFor2DMovement(space, mypoint, otherpoint);
			space.moveByVector(this, 2, angle, 0);

			//Human moves within the space
			mypoint = space.getLocation(this);
			grid.moveTo(this, (int)mypoint.getX(), (int)mypoint.getY());
			energy--;
		}

	}


}