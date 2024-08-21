package tileworld;


import java.util.ArrayList;
import java.util.List;

import repast.simphony.context.Context;
import repast.simphony.context.space.continuous.ContinuousSpaceFactory;
import repast.simphony.context.space.continuous.ContinuousSpaceFactoryFinder;
import repast.simphony.context.space.grid.GridFactory;
import repast.simphony.context.space.grid.GridFactoryFinder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.continuous.RandomCartesianAdder;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridBuilderParameters;
import repast.simphony.space.grid.SimpleGridAdder;
import repast.simphony.space.grid.WrapAroundBorders;

public class TileWorldContext implements ContextBuilder<Object> {
	private Context<Object> context;
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	
	@Override
	public Context build(Context<Object> context) {
		context.setId("tileworld");
		
		int height = 50;
		int width = 100;
		
		this.context = context;
		//this.space = 		
		
		ContinuousSpaceFactory spaceFactory = ContinuousSpaceFactoryFinder.createContinuousSpaceFactory(null);
		
		this.space =  spaceFactory.createContinuousSpace("space", context,
				new RandomCartesianAdder<Object>(),
				new repast.simphony.space.continuous.WrapAroundBorders(), width, height);

		GridFactory gridFactory = GridFactoryFinder.createGridFactory(null);
		
		this.grid = gridFactory.createGrid("grid", context,
				new GridBuilderParameters<Object>( new WrapAroundBorders(),
				new SimpleGridAdder<Object>(),
				true, width, height));				
		initial_setup();
		return context;
	}
	
	public void initial_setup() {
		System.out.print("Initial");
		//Populate the context
		int agents = 1;
		int holes = 30;
		int tiles = 30;
		int home = 1;
		int obstacles = 20;
		
		//Add agents
		for (int i = 0; i < home; i++) {
			int energy = RandomHelper.nextIntFromTo(4, 10);
			this.context.add(new EnergyStation(this.space, this.grid, energy));
			}
		
		//Add agents
		for (int i = 0; i < agents; i++) {
			this.context.add(new Agent(this.space, this.grid,500,i+1));
		}
		
		//Add holes
		for (int i = 0; i < holes; i++) {			
			int score = RandomHelper.nextIntFromTo(1, 10);
			this.context.add(new Hole(this.space, this.grid, score));
		}
		
		 
		//Add Tiles
		for (int i = 0; i < tiles; i++) {
			this.context.add(new Tile(this.space, this.grid));
		}
		
		// Add obstacles
		
		for (int i = 0; i < obstacles; i++) {			
			// 0 horizontal, 1 vertical
			int length = RandomHelper.nextIntFromTo(1, 5);
			int orientation = RandomHelper.nextIntFromTo(1, 2);
			Obstacle origialObstacle = new Obstacle(space, grid, length, orientation-1, i+1);
			context.add(origialObstacle);
		}
		
		for(Object obj: this.context) {
			NdPoint pt = space.getLocation(obj);
			//if(!(obj instanceof Obstacle)) {
				this.grid.moveTo(obj, (int)pt.getX(), (int)pt.getY());
			//}	
		}
		
		//replicateObjectHorizontally(this.context, this.grid, 0, 0, 1);
	}
	
	
	 @ScheduledMethod(start = 1, interval = 10) // Assuming 'n' is 10
	    public void updateObstacles() {
		 System.out.print("++++++++++++++++++++++++++++++      CHANGING    CONTEXT   ++++++++++++++++++++++++++++++++");
		 	int tiles = 100;
	        // Collect current obstacles
	        List<Object> obstacles = new ArrayList<>();
	        for (Object obj : this.context) {
	            if (obj instanceof Tile) {
	                obstacles.add(obj);
	            }
	        }

	        // Remove obstacles from grid
	        obstacles.forEach(obstacle -> {
	            this.context.remove(obstacle);
	        });
	        
	        /*for (int i = 0; i < tiles; i++) {
				this.context.add(new Tile(this.space, this.grid));
			}
	        
			for(Object obj: this.context) {
				NdPoint pt = space.getLocation(obj);
				this.grid.moveTo(obj, (int)pt.getX(), (int)pt.getY());
				//}
				
			}*/

	        // Re-add obstacles to random locations
	        ///initial_setup(); // This function is reused for simplicity
	    }
	    
	    public void findObstacle() {
	    	for (Object obj : context) {
	            if (obj instanceof Obstacle) {
	            	System.out.print("Obstacle found");
	            	/*station = (EnergyStation) obj;
	            			stations.add(station);
	            			System.out.println(station);*/
	            }
	        }
	    }
	    
	    /*private void replicateObjectHorizontally(Context<Object> context, Grid<Object> grid, int startX, int startY, int n) {
	        for (int i = 0; i < n; i++) {
	            // Create a new instance or clone of the original object
	            Obstacle obstacle = new Obstacle(this.space, grid); // Replace this with the correct instantiation or cloning
	            context.add(obstacle); // Add to context
	            grid.moveTo(obstacle, 1, 1); // Position on the grid
	            
	        }
	    }*/

}
