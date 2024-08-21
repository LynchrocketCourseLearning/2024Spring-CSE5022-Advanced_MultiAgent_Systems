package tileWorld;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.query.space.grid.GridCell;
import repast.simphony.query.space.grid.GridCellNgh;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import repast.simphony.util.ContextUtils;
import repast.simphony.util.SimUtilities;

public class Agent{
	private int id;

	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	
	private int changeInterval;
	
	/**
	 * energy used in moving
	 * */
	private int energy, startingEnergy, energyThreshold;
	/**
	 * the ratio in sensing the environment
	 * */
	private int senseRatio;
	/**
	 * the ratio in moving at a step
	 * */
	private int moveRatio;
	
	/**
	 * achieved score
	 * */
	private int score;
	
	/**
	 * found tiles then carry them
	 * */
	private List<Tile> tilesCarried;
	/**
	 * used for static holes
	 * */
	private List<GridPoint> holesFound;
	
	/**
	 * the flag indicating whether heading to home
	 * */
	private boolean headingHome;
	/**
	 * last passed point
	 * */
	private GridPoint lastPt;
	/**
	 * other passed points
	 * */
	private FixedSizeQueue passedPts;
	/**
	 * the point that is headed
	 * */
	private GridPoint targetPt;
	
	public Agent(ContinuousSpace<Object> space, Grid<Object> grid, int id, int energy, int changeInterval) {
		this.id = id;
		
		this.space = space;
		this.grid = grid;
		
		this.changeInterval = changeInterval;
		
		this.energy = this.startingEnergy = energy;
		this.energyThreshold = energy / 3;
		
		this.senseRatio = 4;
		this.moveRatio = 1;
		
		this.score = 0;
		
		this.headingHome = false;
		this.tilesCarried = new ArrayList<>();
		this.holesFound = new ArrayList<>();
		
		this.lastPt = grid.getLocation(this);
		this.passedPts = new FixedSizeQueue(3, 2);
		this.targetPt = null;
	}

	@ScheduledMethod(start=1, interval=1)
	public void step() {
		System.out.println("At "+grid.getLocation(this));
		
		// reset target when refreshing. Not practical
//		double tick = RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
//		if (tick % changeInterval == 0.0) {
//			targetPt = null;
//		}

		// try picking up a tile or filling a hole at current location
		// try picking first then filling. The short circuit of '&&' will make it. 
		// if there is no tile to pick up or no tile to pick up, then move
		if (!pickTile() && !fillHole()) {
			if (energy <= energyThreshold) { 
				// if energy is lower than threshold, then it needs to refuel at home
				if (!headingHome) {
					targetPt = findHomeSpot();
					headingHome = true;
				}
				System.out.println("Heading to home at "+targetPt);
				if(moveTowards(targetPt)) {
					targetPt = null;
					// refuel
					energy = startingEnergy;
					headingHome = false;
				}
			} else if(targetPt != null) {
				System.out.println("Heading to target at "+targetPt);
				// continue moving to target
				if(moveTowards(targetPt)) {
					targetPt = null;
				}
			} else if (tilesCarried.isEmpty()) { 
				// if not taking a tile, find and pick one
				targetPt = findTileSpot();
				System.out.println("Found tile at "+targetPt);
				// move and pick tile
				if(moveTowards(targetPt)) {
					System.out.println("Succeed in finding a tile.");
					targetPt = null;
					pickTile();
				}
			} else {
				// if already taken a tile, find and fill the hole
				targetPt = findHoleSpotStatic();
				System.out.println("Found hole at "+targetPt);
				// move and fill hole
				if(moveTowards(targetPt)) {
					System.out.println("Succeed in finding a hole.");
					targetPt = null;
					fillHole();
				}
			}
		}
		
		System.out.print("Agent "+this.id+", score: "+this.score+", energy: "+this.energy+", tiles: "+this.tilesCarried.size()+" \n\n");
	}
	
	@ScheduledMethod(start=1, interval=1)
	public void recordData() {
		double tick = RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
		DataCollector.INSTANCE.insertData("tick", tick);
		DataCollector.INSTANCE.insertData("agent", this.id);
		DataCollector.INSTANCE.insertData("score", this.score);
		DataCollector.INSTANCE.insertData("energy", this.energy);
		DataCollector.INSTANCE.insertData("location", grid.getLocation(this));
		DataCollector.INSTANCE.insertData("target", this.targetPt);
	}
	
	/**
	 * find the home to refuel
	 * agent knows the context when finding home
	 * */
	public GridPoint findHomeSpot() {
		Context<Object> context = ContextUtils.getContext(this);
		ArrayList<Home> homes = new ArrayList<>();
		for (Object obj : context) {
			if (obj instanceof Home home) {
				homes.add(home);
			}
		}
		
		GridPoint pt = grid.getLocation(this);
		homes.sort((home1, home2)-> 
			(int)(grid.getDistance(pt, grid.getLocation(home1))-grid.getDistance(pt, grid.getLocation(home2)))
		);
		GridPoint homeSpot = grid.getLocation(homes.get(0));
		
		return homeSpot;
	}
	
	/**
	 * find the tile
	 * */
	public GridPoint findTileSpot() {
		List<GridCell<Tile>> tileCells = getThisGridNeighbour(Tile.class, true, senseRatio, senseRatio);

		GridPoint thisPt = grid.getLocation(this);
		
		GridPoint tileSpot = null;
		int maxCount = -1;
		for (GridCell<Tile> cell : tileCells) {
			if (cell.size() > maxCount) {
				tileSpot = cell.getPoint();
				maxCount = cell.size();
			} else if (cell.size() == maxCount && maxCount > 0) {
				if (grid.getDistance(thisPt, tileSpot) > grid.getDistance(thisPt, cell.getPoint())) {
					tileSpot = cell.getPoint();
				}
			}
		}
		
		return tileSpot;
	}
	
	/**
	 * find the hole
	 * simple strategy
	 * */
	public GridPoint findHoleSpot() {
		List<GridCell<Hole>> holeCells = getThisGridNeighbour(Hole.class, true, senseRatio, senseRatio);
		
		GridPoint thisPt = grid.getLocation(this);
		
		GridPoint holeSpot = null;
		int maxCount = -1;
		for (GridCell<Hole> cell : holeCells) {
			if (cell.size() > maxCount) {
				holeSpot = cell.getPoint();
				maxCount = cell.size();
			} else if (cell.size() == maxCount && maxCount > 0) {
				if (grid.getDistance(thisPt, holeSpot) > grid.getDistance(thisPt, cell.getPoint())) {
					holeSpot = cell.getPoint();
				}
			}
		}
		
		return holeSpot;
	}
	
	/**
	 * find the hole
	 * used when the holes are static
	 * */
	public GridPoint findHoleSpotStatic() {
		List<GridCell<Hole>> holeCells = getThisGridNeighbour(Hole.class, true, senseRatio, senseRatio);
		
		GridPoint thisPt = grid.getLocation(this);

		GridPoint holeSpot = null;
		int maxCount = -1;
		for (GridCell<Hole> cell : holeCells) {
			// store the location of the holes
			if (cell.size() > 0) {
				if (!this.holesFound.contains(cell.getPoint())) {
					this.holesFound.add(cell.getPoint());
				}
			}
			if (cell.size() > maxCount) {
				holeSpot = cell.getPoint();
				maxCount = cell.size();
			} else if (cell.size() == maxCount && maxCount > 0) {
				if (grid.getDistance(thisPt, holeSpot) > grid.getDistance(thisPt, cell.getPoint())) {
					holeSpot = cell.getPoint();
				}
			}
		}
		
		// no hole found in senseRatio.
		// instead of randomly moving, move to the hole found before
		if (maxCount == 0 && !this.holesFound.isEmpty()) {
			if (holesFound.size() > 1) {
				holesFound.sort((hole1, hole2) -> 
					(int)(grid.getDistance(thisPt, hole1)-grid.getDistance(thisPt, hole2))
				);
			}
			holeSpot = holesFound.get(0);
		}
		
		return holeSpot;
	}

	/**
	 * pick up a tile at current location
	 * return true if it has picked up a tile; otherwise, false.
	 * */
	public boolean pickTile() {
		GridPoint pt = grid.getLocation(this);
		
		List<Tile> currentTiles = new ArrayList<>();
		Iterable<Object> objectsAtCurrentLocation = grid.getObjectsAt(pt.getX(), pt.getY());
		for (Object obj : objectsAtCurrentLocation) {
			if (obj instanceof Tile tile) {
				currentTiles.add(tile);
			}
		}
		
		if (currentTiles.size() > 0) {
			int index = RandomHelper.nextIntFromTo(0, currentTiles.size()-1);
			Tile tile = currentTiles.get(index);
			this.tilesCarried.add(tile);
			
			this.removeFromContext(tile);
			return true;
		}
		return false;
	}
	
	/**
	 * fill a hole at current location
	 * return true if it has filled a hole; otherwise, false.
	 * */
	public boolean fillHole() {
		GridPoint pt = grid.getLocation(this);
		
		List<Hole> currentHoles = new ArrayList<>();
		Iterable<Object> objectsAtCurrentLocation = grid.getObjectsAt(pt.getX(), pt.getY());
		for (Object obj : objectsAtCurrentLocation) {
			if (obj instanceof Hole hole) {
				currentHoles.add(hole);
			}
		}
		
		// if no tile is carried, then do not fill the hole
		if (currentHoles.size() > 0 && !this.tilesCarried.isEmpty()) {
			int index = RandomHelper.nextIntFromTo(0, currentHoles.size()-1);
			Hole hole = currentHoles.get(index);
			this.tilesCarried.remove(0);
			GridPoint loc = grid.getLocation(hole);
			if (loc != null && this.holesFound.contains(loc)) {
				this.holesFound.remove(loc);
			}
			this.score += hole.getScore();
			
			this.removeFromContext(hole);
			return true;
		}
		return false;
	}
	
	/**
	 * get neighbor in extent units
	 * */
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
	
	/**
	 * indicating the point
	 * */
	public <T> List<GridCell<T>> getPtGridNeighbour(GridPoint pt, Class<T> clazz, boolean needShuffle, int... extent) {
		GridCellNgh<T> nghCreator = new GridCellNgh<>(grid, pt, clazz, extent);
		List<GridCell<T>> nghCells = nghCreator.getNeighborhood(false);
		if (needShuffle) {
			SimUtilities.shuffle(nghCells, RandomHelper.getUniform());
		}
		
		return nghCells;
	}
	
	/**
	 * move towards the point
	 * one distance a time
	 * return true if it has reached the point; otherwise, false.
	 * */
	public boolean moveTowards(GridPoint targetPt) {
		// if no target, then stay still
		// return true to make it re-choose the target at next tick
		if (targetPt == null) {
			return true;
		}
		
		GridPoint thisPt = grid.getLocation(this);
		// if no energy, it cannot move
		if (energy <= 0) {
			return thisPt.equals(targetPt);
		}
		
		// not reached the target
		if (!thisPt.equals(targetPt)) {
			// find the legal points around
			List<GridCell<Obstacle>> nghCells = getThisGridNeighbour(Obstacle.class, false, moveRatio, moveRatio);
			List<GridPoint> nghLegalPts = nghCells.stream()
					.filter(cell -> cell.size() <= 0)
					.map(cell -> cell.getPoint())
					.filter(point -> !point.equals(thisPt) && !point.equals(lastPt) && !passedPts.contains(point))
					.toList();
			
			// if surrounded by the obstacle, then stay still
			// return false to keep target and make agent wait to the environment to refresh
			if (nghLegalPts.isEmpty()) {
				return false;
			}
			
			List<GridCell<Obstacle>> senseCells = getThisGridNeighbour(Obstacle.class, false, senseRatio, senseRatio);
			List<GridPoint> senseLegalPts = senseCells.stream()
					.filter(cell -> cell.size() <= 0)
					.map(cell -> cell.getPoint())
					.filter(point -> !point.equals(thisPt) && !point.equals(lastPt) && !passedPts.contains(point))
					.toList();
			
			boolean hasSensedTarget = senseLegalPts.stream().filter(pt -> targetPt.equals(pt)).toList().size() > 0;
			// if target under the senseRatio, then the agent can directly move to it; otherwise, move to the around point that nearest to target
			GridPoint nextPoint = hasSensedTarget ? 
					shortestPath(thisPt, targetPt).get(1) : 
					Collections.min(nghLegalPts, (p1, p2) -> (int)(grid.getDistance(p1, targetPt) - grid.getDistance(p2, targetPt)));
			
			// record passed point
			lastPt = thisPt;
			passedPts.offer(thisPt);
			
			System.out.println("Next move to " + nextPoint);
			moveTo(nextPoint);
			// decrease energy after moving
			this.energy--;
			
			return grid.getLocation(this).equals(targetPt);
		}
		return true;
	}
	
	private void moveTo(GridPoint pt) {
		space.moveTo(this, pt.getX(), pt.getY());
		grid.moveTo(this, pt.getX(), pt.getY());
	}
	
	/**
	 * A* algorithm
	 * maybe using too much information
	 * */
	public List<GridPoint> shortestPath(GridPoint startPt, GridPoint targePt) {
		PriorityQueue<PointWithCost> openList = new PriorityQueue<>();
		PriorityQueue<PointWithCost> closeList = new PriorityQueue<>();
		openList.add(new PointWithCost(startPt, 0));
		PointWithCost pt = null;
		while (!openList.isEmpty()) {
			pt = openList.poll();
			GridPoint ptGrid = pt.pt;
			if (ptGrid.equals(targePt)) {
				return calculatePath(pt);
			}
			
			List<GridCell<Obstacle>> nghCells = getPtGridNeighbour(ptGrid, Obstacle.class, false, moveRatio, moveRatio);
			List<GridPoint> nghLegalPts = nghCells.stream()
					.filter(cell -> cell.size() <= 0)
					.map(cell -> cell.getPoint())
					.filter(point -> !ptGrid.equals(point))
					.toList();	
			for (GridPoint nextPtGrid : nghLegalPts) {
				PointWithCost nextPt = new PointWithCost(nextPtGrid, 0);
				if (!closeList.contains(nextPt)) {
					nextPt.cost = pt.cost + 1 + grid.getDistance(ptGrid, nextPtGrid);
					nextPt.parentPt = pt;

					if (!openList.contains(nextPt)) {
						openList.add(nextPt);
					}
				}
			}
			closeList.add(pt);
		}
		
		return calculatePath(pt);
	}
	
	private List<GridPoint> calculatePath(PointWithCost pt) {
		Deque<GridPoint> path = new LinkedList<>();
		PointWithCost cur = pt;
		while (cur != null) {
			path.addFirst(cur.pt);
			cur = cur.parentPt;
		}
		return new ArrayList<>(path);
	}
	
	public void removeFromContext(Object obj) {
		Context<Object> context = ContextUtils.getContext(obj);
		context.remove(obj);
	}
	
	public class PointWithCost implements Comparable<PointWithCost>{
		public GridPoint pt;
		public double cost;
		
		public PointWithCost parentPt;
		
		public PointWithCost(GridPoint pt, double cost) {
			this.pt = pt;
			this.cost = cost;
		}
		
		@Override
		public int compareTo(PointWithCost o) {
			return (this.cost >= o.cost) ? 1 : -1;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof PointWithCost o) {
				return pt.equals(o.pt);
			}
			return false;
		}
	}
	
	public class FixedSizeQueue {
		private int size;
		private int threshold;
		private List<PointWithCost> queue;
		
		public FixedSizeQueue(int size, int threshold) {
			this.size = size;
			this.threshold = threshold;
			this.queue = new ArrayList<>();
		}
		
		public void offer(GridPoint pt) {
			PointWithCost pwc = new PointWithCost(pt, 1);
			int idx = this.queue.indexOf(pwc);
			if (idx == -1) {
				this.queue.add(pwc);
			} else {
				this.queue.get(idx).cost++;
			}
			if (this.queue.size() > size) {
				this.queue.remove(0);
			}
		}
		
		public PointWithCost poll() {
			return this.queue.remove(0);
		}
		
		public boolean contains(GridPoint pt) {
			for (PointWithCost pwc : queue) {
				if (pwc.cost >= threshold && pwc.pt.equals(pt)) {
					return true;
				}
			}
			return false;
		}
	}
}