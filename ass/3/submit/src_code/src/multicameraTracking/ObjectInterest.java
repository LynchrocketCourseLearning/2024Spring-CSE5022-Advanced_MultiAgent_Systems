package multicameraTracking;

import java.util.List;

import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.query.space.grid.GridCell;
import repast.simphony.query.space.grid.GridCellNgh;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.SpatialMath;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import repast.simphony.util.SimUtilities;

public class ObjectInterest {
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	public int id;

	private boolean istracked;
	private double importance;
	private double threadLevel;

	private double updateInterval = 1;
	private double resourceAllocation = 1;

	public ObjectInterest(ContinuousSpace<Object> space, Grid<Object> grid, int id) {
		this.space = space;
		this.grid = grid;
		this.id = id;

		this.istracked = false;
		this.importance = RandomHelper.nextDoubleFromTo(0, 100);
		this.threadLevel = RandomHelper.nextDoubleFromTo(0, 100);
	}

	@ScheduledMethod(start = 1, interval = 1, priority = 1)
	public void step() {
		List<GridCell<Object>> gridCells = getThisGridNeighbour(Object.class, true, 1, 1);

		if (!gridCells.isEmpty()) {
			// Choose a random cell from the shuffled list
			GridPoint nextSpot = gridCells.get(0).getPoint(); // Select the first cell after shuffling for randomness
			moveTowards(nextSpot);
		}
	}

	public void moveTowards(GridPoint pt) {
		if (!pt.equals(grid.getLocation(this))) {
			NdPoint mypoint = space.getLocation(this);
			NdPoint otherPoint = new NdPoint(pt.getX(), pt.getY());
			double angle = SpatialMath.calcAngleFor2DMovement(space, mypoint, otherPoint);
			space.moveByVector(this, 1, angle, 0);

			mypoint = space.getLocation(this);
			grid.moveTo(this, (int) mypoint.getX(), (int) mypoint.getY());
		}
	}

	public boolean isTracked() {
		return this.istracked;
	}

	public void setTracking(boolean track) {
		this.istracked = track;
	}

	public double getImportance() {
		return this.importance;
	}

	public double getThreadLevel() {
		return this.threadLevel;
	}

	public double getUpdateInterval() {
		return this.updateInterval;
	}

	public void setUpdateInterval(double updateInterval) {
		this.updateInterval = updateInterval;
	}

	public double getResourceAllocation() {
		return this.resourceAllocation;
	}

	public void setResourceAllocation(double resourceAllocation) {
		this.resourceAllocation = resourceAllocation;
	}

	/**
	 * get neighbor in extent units
	 */
	public <T> List<GridCell<T>> getThisGridNeighbour(Class<T> clazz, boolean needShuffle, int... extent) {
		return getObjGridNeighbour(this, clazz, needShuffle, extent);
	}

	public <T> List<GridCell<T>> getObjGridNeighbour(Object obj, Class<T> clazz, boolean needShuffle, int... extent) {
		GridPoint pt = grid.getLocation(obj);
		GridCellNgh<T> nghCreator = new GridCellNgh<>(grid, pt, clazz, extent);
		List<GridCell<T>> nghCells = nghCreator.getNeighborhood(false);
		if (needShuffle) {
			SimUtilities.shuffle(nghCells, RandomHelper.getUniform());
		}

		return nghCells;
	}
}
