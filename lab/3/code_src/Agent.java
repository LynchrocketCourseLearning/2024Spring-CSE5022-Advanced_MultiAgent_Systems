package tileWorld;

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
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import repast.simphony.util.ContextUtils;
import repast.simphony.util.SimUtilities;

public class Agent{
	private int id;
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	
	private int energy, startingEnergy;
	private int energyThreshold;
	private int senseRatio;
	private int score;
	
	private List<Tile> tilesCarry;
	private List<Hole> holesFound;
	private List<Home> homesFound;
	
	private GridPoint targetPt;
	
	public Agent(ContinuousSpace<Object> space, Grid<Object> grid, int energy, int id) {
		this.id = id;
		this.space = space;
		this.grid = grid;
		this.energy = this.startingEnergy = energy;
		this.energyThreshold = energy / 3;
		this.score = 0;
		this.tilesCarry = new ArrayList<>();
		this.holesFound = new ArrayList<>();
		this.homesFound = new ArrayList<>();
	}

	@ScheduledMethod(start=1, interval=1)
	public void step() {
		// always finding neighbor homes in case of running out of energy
		// but do not move to home here
		List<GridCell<Home>> homeCells = getGridNeighbour(Home.class, false, 4, 4);
		for (GridCell<Home> cell : homeCells) {
			if (cell.size() > 0) {
				Iterable<Object> objectsAtCurrentLocation = grid.getObjectsAt(cell.getPoint().getX(), cell.getPoint().getY());
				for (Object obj : objectsAtCurrentLocation) {
					if (obj instanceof Home home) {
						this.homesFound.add(home);
					}
				}
			}
		}
		
		// try picking up a tile or filling a hole at current location
		// try picking first then filling. The short circuit of '&&' will make it. 
		// if there is no tile to pick up or no tile to pick up, then move
		if (!pickTile() && !putTilesOnHoles()) {
			if (energy <= energyThreshold) { 
				// if energy is lower than threshold, then it needs to refuel at home
				targetPt = findHomeSpot();
				if(moveTowards(targetPt)) {
					targetPt = null;
					// refuel
					energy = startingEnergy;
				}
			} else if(targetPt != null) { 
				// continue moving to target point
				if(moveTowards(targetPt)) {
					targetPt = null;
				}
			} else if (tilesCarry.isEmpty()) { 
				// if not taking a tile, find and pick one
				targetPt = findTileSpot();
				// move and pick tile
				if(moveTowards(targetPt)) {
					targetPt = null;
					pickTile();
				}
			} else { 
				// if already taken a tile, find and fill the hole
				targetPt = findHoleSpot();
				// move and fill hole
				if(moveTowards(targetPt)) {
					targetPt = null;
					putTilesOnHoles();
				}
			}
		}
		
		System.out.print("Agent "+this.id+", score: "+this.score+", energy: "+this.energy+", tiles: "+this.tilesCarry.size()+" \n\n");
	}
	
	public GridPoint findHomeSpot() {
		List<GridCell<Home>> homeCells = getGridNeighbour(Home.class, true, 4, 4);
		
		// find the spot with most holes
		GridPoint homeSpot = null;
		int maxCount = -1;
		for (GridCell<Home> cell : homeCells) {
			if (cell.size() > 0) {
				Iterable<Object> objectsAtCurrentLocation = grid.getObjectsAt(cell.getPoint().getX(), cell.getPoint().getY());
				for (Object obj : objectsAtCurrentLocation) {
					if (obj instanceof Home home) {
						this.homesFound.add(home);
					}
				}
			}
			if (cell.size() > maxCount) {
				homeSpot = cell.getPoint();
				maxCount = cell.size();
			}
		}
		
		if (maxCount == 0 && !homesFound.isEmpty()) {
			GridPoint pt = grid.getLocation(this);
			homesFound.sort((home1, home2)-> 
				(int)(grid.getDistance(pt, grid.getLocation(home1))-grid.getDistance(pt, grid.getLocation(home2)))
			);
			homeSpot = grid.getLocation(homesFound.get(0));
		}
		return homeSpot;
	}
	
	public GridPoint findTileSpot() {
		List<GridCell<Tile>> tileCells = getGridNeighbour(Tile.class, true, 4, 4);
		
		// find the spot with most tiles
		GridPoint tileSpot = null;
		int maxCount = -1;
		for (GridCell<Tile> cell : tileCells) {
			if (cell.size() > maxCount) {
				tileSpot = cell.getPoint();
				maxCount = cell.size();
			}
		}
		
		return tileSpot;
	}
	
	public GridPoint findHoleSpot() {
		List<GridCell<Hole>> holeCells = getGridNeighbour(Hole.class, true, 4, 4);
		
		// find the spot with most holes
		GridPoint holeSpot = null;
		int maxCount = -1;
		for (GridCell<Hole> cell : holeCells) {
			// store the location of the holes
			if (cell.size() > 0) {
				Iterable<Object> objectsAtCurrentLocation = grid.getObjectsAt(cell.getPoint().getX(), cell.getPoint().getY());
				for (Object obj : objectsAtCurrentLocation) {
					if (obj instanceof Hole hole) {
						this.holesFound.add(hole);
					}
				}
			}
			if (cell.size() > maxCount) {
				holeSpot = cell.getPoint();
				maxCount = cell.size();
			}
		}
		
		// no hole found in 5x5.
		// instead of randomly moving, move to the hole found before
		if (maxCount == 0 && !holesFound.isEmpty()) {
			GridPoint pt = grid.getLocation(this);
			holesFound.sort((hole1, hole2) -> 
				(int)(grid.getDistance(pt, grid.getLocation(hole1))-grid.getDistance(pt, grid.getLocation(hole2)))
			);
			holeSpot = grid.getLocation(holesFound.get(0));
		}
		
		return holeSpot;
	}
	
	/**
	 * get neighbour in extent units
	 * currently 5x5 units
	 * */
	public <T> List<GridCell<T>> getGridNeighbour(Class<T> clazz, boolean needShuffle, int... extent) {
		GridPoint pt = grid.getLocation(this);
		GridCellNgh<T> nghCreator = new GridCellNgh<>(grid, pt, clazz, extent);
		List<GridCell<T>> nghCells = nghCreator.getNeighborhood(true);
		if (needShuffle) SimUtilities.shuffle(nghCells, RandomHelper.getUniform());
		
		return nghCells;
	}

	/**
	 * move towards the point
	 * one distance a time
	 * return true if it has reached the point; otherwise, false.
	 * */
	public boolean moveTowards(GridPoint pt) {
		if (energy <= 0) {
			return grid.getLocation(this).equals(pt);
		}
		if(!grid.getLocation(this).equals(pt)) {
			NdPoint myPoint = space.getLocation(this);
			NdPoint otherPoint = new NdPoint(pt.getX(),pt.getY());

			List<GridCell<Obstacle>> obstacleCells = getGridNeighbour(Obstacle.class, false, 1, 1);
			List<Double> illegalAngles = new ArrayList<>();
			for (GridCell<Obstacle> cell : obstacleCells) {
				if (cell.size() > 0) {
					NdPoint obstaclePt = new NdPoint(cell.getPoint().getX(), cell.getPoint().getY());
					double illegalAngle = SpatialMath.calcAngleFor2DMovement(space, myPoint, obstaclePt);
					illegalAngles.add(illegalAngle);
				}
			}
			
			double angle = SpatialMath.calcAngleFor2DMovement(space, myPoint, otherPoint);
			double angle45degree = Math.toRadians(45);
			for (double illegal : illegalAngles) {
				if (Double.compare(Math.abs(illegal - angle), angle45degree) >= 0) {
					continue;
				}
				double illegalPre = illegal - angle45degree;
				double illegalNext = illegal + angle45degree;
				
				double angleDiff1 = Math.abs(illegal - angle);
				double angleDiff2 = Math.abs(illegalPre - angle);
				double angleDiff3 = Math.abs(illegalNext - angle);
				if (Double.compare(angleDiff1, angleDiff2) <= 0 && Double.compare(angleDiff1, angleDiff3) <= 0) {
					angle = (Double.compare(angleDiff2, angleDiff3) < 0) ? angleDiff2 : angleDiff3;
				}
			}
			
			space.moveByVector(this, 1, angle, 0);
			NdPoint newPt = space.getLocation(this);
			grid.moveTo(this, (int)newPt.getX(), (int)newPt.getY());
			
			// decrease energy after moving
			this.energy--;
			return grid.getLocation(this).equals(pt);
		} 
		return true;
	}
	
	
	/**
	 * pick up tile at current location
	 * return true if it has picked up a tile; otherwise, false.
	 * */
	public boolean pickTile() {
		GridPoint pt = grid.getLocation(this);
		List<Tile> currentTiles = new ArrayList<>();

		/* Complete code here */
		Iterable<Object> objectsAtCurrentLocation = grid.getObjectsAt(pt.getX(), pt.getY());
		for (Object obj : objectsAtCurrentLocation) {
			if (obj instanceof Tile tile) {
				currentTiles.add(tile);
			}
		}
		
		if (currentTiles.size() > 0) {
			int index = RandomHelper.nextIntFromTo(0, currentTiles.size()-1);
			Tile tile = currentTiles.get(index);
			this.tilesCarry.add(tile);
			
			this.removeFromContext(tile);
			return true;
		}
		return false;
	}
	
	
	/**
	 * fill a hole at current location
	 * return true if it has filled a hole; otherwise, false.
	 * */
	public boolean putTilesOnHoles() {
		GridPoint pt = grid.getLocation(this);
		List<Hole> currentHoles = new ArrayList<>();

		/* Complete code here */ 
		Iterable<Object> objectsAtCurrentLocation = grid.getObjectsAt(pt.getX(), pt.getY());
		for (Object obj : objectsAtCurrentLocation) {
			if (obj instanceof Hole hole) {
				currentHoles.add(hole);
			}
		}
		
		if (currentHoles.size() > 0 && !this.tilesCarry.isEmpty()) {
			int index = RandomHelper.nextIntFromTo(0, currentHoles.size()-1);
			Hole hole = currentHoles.get(index);
			this.tilesCarry.remove(0);
			this.holesFound.remove(hole);
			this.score += hole.getScore();
			
			this.removeFromContext(hole);

			return true;
		}
		return false;
	}
	
	public void removeFromContext(Object obj) {
		NdPoint spacePt = space.getLocation(obj);
		Context<Object> context = ContextUtils.getContext(obj);
		context.remove(obj);
	}
}