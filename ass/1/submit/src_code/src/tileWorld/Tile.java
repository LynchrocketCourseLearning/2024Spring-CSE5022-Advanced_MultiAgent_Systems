package tileWorld;

import java.util.ArrayList;
import java.util.List;

import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;
import repast.simphony.util.ContextUtils;

public class Tile {
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	private int changeInterval;

	public Tile(ContinuousSpace<Object> space, Grid<Object> grid, int changeInterval) {
		this.space = space;
		this.grid = grid;
		this.changeInterval = changeInterval;
	}
	
	@ScheduledMethod(start=1, interval=1, priority=1)
	public void updateTile() {
		double tick = RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
		double control = tick % this.changeInterval;
		
		if (control == 0.0) {
			System.out.println("Tile tick: "+tick+"\nRelocating.");
			relocateTiles();
	    }
	}
	
    public void relocateTiles() {		
    	List<Tile> TilesList = new ArrayList<>();
    	Context<Object> context = ContextUtils.getContext(this);
    	int tilesNumber = 0;
    	
    	for (Object obj : context) {
            if (obj instanceof Tile tile) {
            	TilesList.add(tile);
            }
        }   
    	
    	tilesNumber = TilesList.size();
    	if (TilesList.size() > 0) {
    		for (Tile tile: TilesList) {
    			context.remove(tile);
    		}
		}
    	
    	// Relocate Tiles
    	for(int i = 0; i < TilesList.size(); i++) {
    		int xLocation = RandomHelper.nextIntFromTo(1, 100);
    		int yLocation = RandomHelper.nextIntFromTo(1, 50);
    		Tile newTile = new Tile(this.space, this.grid, this.changeInterval);
    		context.add(newTile);
    		this.grid.moveTo(newTile, (int)xLocation, (int)yLocation);
    	}
    	System.out.println("Tiles number: "+ tilesNumber);
    }
}
