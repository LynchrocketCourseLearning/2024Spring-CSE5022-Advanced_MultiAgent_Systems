package tileworld;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import com.jogamp.nativewindow.util.Point;

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
	boolean critial_state = false;
	private List<GridPoint> shortPathStation;
	
	public Agent(ContinuousSpace<Object> space, Grid<Object> grid, int energy, int id) {
		this.id = id;
		this.space = space;
		this.grid = grid;
		this.energy_level = startingEnergy = energy;
		this.score = 0;
		this.currentTiles = new ArrayList<Tile>();
		this.currentHoles = new ArrayList<Hole>();
		this.carryingTile = false;
		this.critial_state = false;
	}

	@ScheduledMethod(start=1, interval=1, priority=1)
	public void step() {
		int minimumEnergy = 20;
		GridPoint pt = grid.getLocation(this);
		double nearestTile = 0.0;
		int senseRadio = 1;
		double nearestStation = get_distance_from_EnergyStation();
		critial(nearestStation);
		
		
		if (this.carryingTile == false) {
			//System.out.print("Looking for Tiles");
			GridCellNgh<Tile> nghCreator = new GridCellNgh<>(grid, pt, Tile.class, senseRadio, senseRadio);
			
			List<GridCell<Tile>> gridCells = nghCreator.getNeighborhood(true);
			SimUtilities.shuffle(gridCells, RandomHelper.getUniform());
			
			GridPoint tileSpot = null;
			int maxCount = -1;
			int i = 1;
			for (GridCell<Tile> cell : gridCells) {
				
				/*System.out.print(cell.getClass().getSimpleName());
				System.out.println("size: "+cell.size());
				System.out.println(i+"\n");*/
				if(cell.size() > maxCount) {
					if (cell.size() > 0) {
						System.out.println("There is a Tile around");
						//updateDistanceTile();
						//nearestTile = distance(tileSpot);
						
						System.out.println("Distance to nearest Tile: "+nearestTile);
						System.out.println(carryingTile);
						
					}	
					
					tileSpot = cell.getPoint();
					//nearestTile = 
					maxCount = cell.size();
					
				}
				i++;
			}
			
			
			//Random Movement
			moveTowards(tileSpot);
			pickTile();
			//List<GridPoint> findPath(GridPoint origin, GridPoint dest)
			
		}else {
			//System.out.print("Looking for Holes");
			GridCellNgh<Hole> nghCreator = new GridCellNgh<>(grid, pt, Hole.class, senseRadio, senseRadio);
			List<GridCell<Hole>> gridCells = nghCreator.getNeighborhood(true);
			SimUtilities.shuffle(gridCells, RandomHelper.getUniform());
			
			GridPoint holeSpot = null;
			int maxCount = -1;
			
			for (GridCell<Hole> cell : gridCells) {
				if(cell.size() > maxCount) {
					/*System.out.println("There is a Hole around");
					System.out.print(cell.getClass().getSimpleName());*/
					holeSpot = cell.getPoint();
					maxCount = cell.size();
					
				}
			}
			moveTowards(holeSpot);
			putTiles_Holes();
			
		}

		this.energy_level--;
		
		
		if (this.currentTiles.size() >0) {
			tilesCollected = this.currentTiles.size();
		}

		
		System.out.print(get_distance_from_EnergyStation()); 
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
	
	public void critial(double nearestStation) {
		if(this.energy_level > (int)nearestStation) {
			System.out.println("I am Okay");
			this.critial_state = true;
		}else {
			System.out.println("Hurry! get to the Energy station");
			this.critial_state = false;
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
		GridPoint pt = grid.getLocation(this);
		List<Object> holes = new ArrayList<Object>();		
		for (Object obj: grid.getObjectsAt(pt.getX(), pt.getY())) {
			if(obj instanceof Hole) {
				Hole hole = (Hole)obj;
				holes.add(hole);
				
			}
		}
		
		if (holes.size() > 0) {
			int index = RandomHelper.nextIntFromTo(0, holes.size()-1);
			Object obj = holes.get(index);
			Hole hole = (Hole)obj;
			
			//System.out.print("Tile placed");
			
			this.score += hole.get_score();
			this.holesCompleted++;			
			//this.currentTiles.remove(0);
			
			//this.currentTiles.add((Hole) obj);
			//Tile location
			Context<Object> context = ContextUtils.getContext(obj);
			context.remove(obj);
			this.carryingTile = false;
			
		
		}
	}
	
	public void clean() {
		//System.out.print("*******************\n\n");
	}
		
	public double get_distance_from_EnergyStation() {
		List<EnergyStation> stations = new ArrayList<>();
		EnergyStation station = null;
		
		Context<Object> context = ContextUtils.getContext(this);
		double distance_from_EnergyStation = 0.0;
		
        for (Object obj : context) {
            if (obj instanceof EnergyStation) {
            	station = (EnergyStation) obj;
            			stations.add(station);
            			System.out.println(station);
            }
        }
        
        NdPoint mypoint = space.getLocation(this);
        NdPoint stationpoint = space.getLocation(station);
        System.out.println("Distance to energy station: "+space.getDistance(mypoint, stationpoint));
        distance_from_EnergyStation = space.getDistance(mypoint, stationpoint);
        return distance_from_EnergyStation;
        
	}
	
	
	public double distance(GridPoint pt) {
		double distance = 0.0;
		
		NdPoint mypoint = space.getLocation(this);
        NdPoint objectPoint = new NdPoint(pt.getX(), pt.getY());
        distance = space.getDistance(mypoint, objectPoint);        
		return distance;
		
	}
	
    public List<GridPoint> findPath(GridPoint origin, GridPoint dest) {
        List<GridPoint> path = new ArrayList<>();
        int currentX = origin.getX();
        int currentY = origin.getY();

        // Add the starting point to the path
        path.add(new GridPoint(currentX, currentY));

        // Move horizontally towards the target
        while (currentX != dest.getX()) {
            if (currentX < dest.getX()) {
                currentX++;
            } else {
                currentX--;
            }
            path.add(new GridPoint(currentX, currentY));
        }

        // Move vertically towards the target
        while (currentY != dest.getY()) {
            if (currentY < dest.getY()) {
                currentY++;
            } else {
                currentY--;
            }
            path.add(new GridPoint(currentX, currentY));
        }

        return path;
    }
    
	public void get_shortest_path_from_EnergyStation() {
		List<EnergyStation> stations = new ArrayList<>();
		EnergyStation station = null;
		
		Context<Object> context = ContextUtils.getContext(this);
		double distance_from_EnergyStation = 0.0;
		
        for (Object obj : context) {
            if (obj instanceof EnergyStation) {
            	station = (EnergyStation) obj;
            			stations.add(station);
            			System.out.println(station);
            }
        }
        
        NdPoint mypoint = space.getLocation(this);
        NdPoint stationpoint = space.getLocation(station);
        System.out.println("Distance to energy station: "+space.getDistance(mypoint, stationpoint));
        distance_from_EnergyStation = space.getDistance(mypoint, stationpoint);
        
        //this.shortPathStation = findPath((GridPoint)mypoint, (GridPoint)stationpoint);
        
        
        //return distance_from_EnergyStation;
        
	}

	
	

}
