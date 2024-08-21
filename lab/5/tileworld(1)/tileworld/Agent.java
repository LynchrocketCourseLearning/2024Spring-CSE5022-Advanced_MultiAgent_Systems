package tileworld;

import java.awt.Color;
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
import repast.simphony.visualizationOGL2D.DefaultStyleOGL2D;

public class Agent{
	//private Context<Object> context;
	private int id;
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	private int energy_level, startingEnergy;
	private List<Tile> currentTiles;
	private List<Hole> currentHoles;
	private int holesCompleted = 0;
	private int tilesCollected = 0;
	private int score;
	private boolean carryingTile; // Initially, the agent is not carrying any tile.
	
	public Agent(ContinuousSpace<Object> space, Grid<Object> grid, int energy, int id) {
		this.id = id;
		this.space = space;
		this.grid = grid;
		this.energy_level = startingEnergy = energy;
		this.score = 0;
		this.currentTiles = new ArrayList<Tile>();
		this.currentHoles = new ArrayList<Hole>();
		this.carryingTile = false;
	}

	@ScheduledMethod(start=1, interval=1)
	public void step() {
		int minimumEnergy = 20;
		GridPoint pt = grid.getLocation(this);
		
		int senseRadio = 1;
		
		if (this.carryingTile == false) {
			System.out.print("Looking for Tiles");
			GridCellNgh<Tile> nghCreator = new GridCellNgh<>(grid, pt, Tile.class, senseRadio, senseRadio);			
			List<GridCell<Tile>> gridCells = nghCreator.getNeighborhood(true);
			SimUtilities.shuffle(gridCells, RandomHelper.getUniform());
			
			/* IMPLEMENT YOUR CODE HERE */


			
		}else {
			System.out.print("Looking for Holes");
			GridCellNgh<Hole> nghCreator = new GridCellNgh<>(grid, pt, Hole.class, senseRadio, senseRadio);
			List<GridCell<Hole>> gridCells = nghCreator.getNeighborhood(true);
			SimUtilities.shuffle(gridCells, RandomHelper.getUniform());
			
			/* IMPLEMENT YOUR CODE HERE */
			
		}
		this.energy_level--;
		
		
		if (this.currentTiles.size() >0) {
			tilesCollected = this.currentTiles.size();
		}
		System.out.print("Agent "+this.id+" score: "+this.score+" tiles_collected: "+this.currentTiles.size()+" holes_completed: "+this.holesCompleted+" energy: "+this.energy_level+" \n\n");
		clean();
	}
		

	public void moveTowards(GridPoint pt) {
		if(!pt.equals(grid.getLocation(this))) {
			NdPoint mypoint = space.getLocation(this);
			NdPoint otherPoint = new NdPoint(pt.getX(), pt.getY());
			double angle = SpatialMath.calcAngleFor2DMovement(space, mypoint, otherPoint);
			space.moveByVector(this, 1, angle,0);
			mypoint = space.getLocation(this);
			grid.moveTo(this, (int)mypoint.getX(), (int)mypoint.getY());

		}
	}
	
	public void pickTile() {
		GridPoint pt = grid.getLocation(this);
		List<Object> tiles = new ArrayList<Object>();		
		for (Object obj: grid.getObjectsAt(pt.getX(), pt.getY())) {
			if(obj instanceof Tile) {
				Tile tile = (Tile)obj;
				tiles.add(obj);

			}
		}
		
		if (tiles.size() > 0) {
			int index = RandomHelper.nextIntFromTo(0, tiles.size()-1);
			Object obj = tiles.get(index);
			this.currentTiles.add((Tile) obj);
			System.out.print("Tile collected");
			//Tile location
			Context<Object> context = ContextUtils.getContext(obj);
			context.remove(obj);			
			this.carryingTile = true;
		}
		
		
	}
	
	
	
	public void putTiles_Holes() {
	
		/* IMPLEMENT YOUR CODE HERE */
		





	}
	
	public void clean() {
		System.out.print("*******************\n\n");
	}	
	

}
