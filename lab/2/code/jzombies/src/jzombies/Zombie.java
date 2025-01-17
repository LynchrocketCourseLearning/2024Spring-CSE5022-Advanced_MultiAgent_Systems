package jzombies;

import java.util.ArrayList;
import java.util.List;

import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.query.space.grid.GridCell;
import repast.simphony.query.space.grid.GridCellNgh;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.SpatialMath;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.graph.Network;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import repast.simphony.util.ContextUtils;
import repast.simphony.util.SimUtilities;

public class Zombie {
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	private Boolean moved;

	//Zombie constructor, only two attributes are considered continous space and grid
	public Zombie(ContinuousSpace<Object> space, Grid<Object> grid) {
		this.space = space;
		this.grid = grid;
	}

	//Method to find a point with the highest amount of humans

	@ScheduledMethod(start=1, interval=1)
	public void step() {
		//Get the location of this zombie within the grid
		GridPoint pt = grid.getLocation(this);

		// Get the neighboring cells around a single zombie
		GridCellNgh<Human> nghCreator = new GridCellNgh<>(grid, pt, Human.class, 1, 1);
		List<GridCell<Human>> gridCells = nghCreator.getNeighborhood(true);
		SimUtilities.shuffle(gridCells, RandomHelper.getUniform());

		//Iterating around a list of cells containing only Humans, and determining only the one with Humans
		GridPoint pointWithMostHumans = null;
		int maxCount = -1;
		for (GridCell<Human> cell : gridCells) {
			if(cell.size() > maxCount) {
				pointWithMostHumans = cell.getPoint();
				maxCount = cell.size();
			}
		}
		moveTowards(pointWithMostHumans);
		infect();
	}

	//Move the zombie towards direction obtained previously
	public void moveTowards(GridPoint pt) {
		//Just to ensure the zombie is not in the point we want to move
		if(!pt.equals(grid.getLocation(this))) {
			//Zombie current location
			NdPoint mypoint = space.getLocation(this);

			//Necessary to have the point whithin continous space
			NdPoint otherPoint = new NdPoint(pt.getX(), pt.getY());
			double angle = SpatialMath.calcAngleFor2DMovement(space, mypoint, otherPoint);

			//Zombie is moved
			space.moveByVector(this, 1, angle,0);
			mypoint = space.getLocation(this);
			grid.moveTo(this, (int)mypoint.getX(), (int)mypoint.getY());

			moved = true;
		}
	}
	
	public void infect() {
		GridPoint pt = grid.getLocation(this);
		List<Human> humans = new ArrayList<Human>();
		for (Object obj: grid.getObjectsAt(pt.getX(), pt.getY())) {
			if (obj instanceof Human human) {
				if(!human.isSafe()) {
					humans.add(human);
				}
			}
		}
		if (humans.size() > 0) {
			int index = RandomHelper.nextIntFromTo(0, humans.size()-1);
			
			Human human = humans.get(index);
			//Get Human Location
			NdPoint spacePt = space.getLocation(human);
			
			Context<Object> context = ContextUtils.getContext(human);
			context.remove(human);
			Zombie zombie = new Zombie(space,grid);
			context.add(zombie);
			space.moveTo(zombie, spacePt.getX(), spacePt.getY());
			grid.moveTo(zombie,  pt.getX(), pt.getY());
			
			Network<Object> net = (Network<Object>)context.getProjection("infected network");
			net.addEdge(this,zombie);
		}
		
	}
}