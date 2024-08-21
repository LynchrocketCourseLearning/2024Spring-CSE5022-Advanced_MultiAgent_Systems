package tileworld;

import java.util.ArrayList;
import java.util.List;

import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.grid.Grid;
import repast.simphony.util.ContextUtils;

public class Tile {
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	
	public Tile(ContinuousSpace<Object> space, Grid<Object> grid) {
		this.space = space;
		this.grid = grid;
	}
	
	@ScheduledMethod(start=1, interval=1, priority=1)
	public void monitorTilesLocation() {
		double tick = RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
		double change_control = 5.0;
		double control = tick%change_control;
		System.out.print("Tile tick: "+tick);
		if (control==0.0) {
			System.out.print("Relocating");
			relocate_tiles();
	    }
	}
    public void relocate_tiles() {		
    	List<Tile> TilesList = new ArrayList<>();
    	Context<Object> context = ContextUtils.getContext(this);
    	int tilesNumber = 0;
    	
    	for (Object obj : context) {
            if (obj instanceof Tile) {
            	Tile tile = (Tile) obj;
            	TilesList.add(tile);
            }
        }    	
    	tilesNumber = TilesList.size();
    	if (TilesList.size() > 0) {
    		for (Tile tile: TilesList ) {
    			context.remove(tile);
    		}
		}
    	
    	//Relocate Tiles
    	for(int i = 0; i < TilesList.size(); i++) {
    		int x_location = RandomHelper.nextIntFromTo(1, 100);
    		int y_location = RandomHelper.nextIntFromTo(1, 50);
    		Tile tilenew = new Tile(this.space, this.grid);
    		context.add(tilenew);
    		this.grid.moveTo(tilenew, (int)x_location, (int)y_location);
    	}
    	System.out.println("Tiles number: "+ tilesNumber);
 	
    }

}
