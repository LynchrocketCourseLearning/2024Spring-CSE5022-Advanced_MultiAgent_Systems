package tileWorld;

import java.util.ArrayList;
import java.util.List;

import repast.simphony.context.Context;
import repast.simphony.context.space.continuous.ContinuousSpaceFactory;
import repast.simphony.context.space.continuous.ContinuousSpaceFactoryFinder;
import repast.simphony.context.space.grid.GridFactory;
import repast.simphony.context.space.grid.GridFactoryFinder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.environment.RunListener;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.parameter.Parameters;
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
		context.setId("TileWorld");
		
		int height = 50;
		int width = 100;
		
		this.context = context;

		ContinuousSpaceFactory spaceFactory = ContinuousSpaceFactoryFinder.createContinuousSpaceFactory(null);
		
		this.space =  spaceFactory.createContinuousSpace(
				"space", 
				context,
				new RandomCartesianAdder<Object>(),
				new repast.simphony.space.continuous.WrapAroundBorders(), 
				width, 
				height
				);

		GridFactory gridFactory = GridFactoryFinder.createGridFactory(null);
		this.grid = gridFactory.createGrid(
				"grid", 
				context,
				new GridBuilderParameters<Object>(
						new WrapAroundBorders(),
						new SimpleGridAdder<Object>(),
						true, width, height
						)
				);

		Parameters params = RunEnvironment.getInstance().getParameters();
		String simName = params.getString("simName");
		DataCollector.INSTANCE.insertGlobalData("simName", simName);
		DataCollector.INSTANCE.insertGlobalData("height", height);
		DataCollector.INSTANCE.insertGlobalData("width", width);
		
		initialSetup();
		RunEnvironment.getInstance().addRunListener(new RunListener() {
			
			@Override
			public void stopped() {
				// TODO Auto-generated method stub
				DataCollector.INSTANCE.outputData();
			}
			
			@Override
			public void started() {
				// TODO Auto-generated method stub
				DataCollector.INSTANCE.init(simName);
			}
			
			@Override
			public void restarted() {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void paused() {
				// TODO Auto-generated method stub
				
			}
		});
		
		return context;
	}
	
	public void initialSetup() {
		System.out.print("Initial");
		// Populate the context
		Parameters params = RunEnvironment.getInstance().getParameters();
		int agents = params.getInteger("agentCount");
		int holes = params.getInteger("holeCount");
		int tiles = params.getInteger("tileCount");
		int homes = params.getInteger("homeCount");
		int obstacles = params.getInteger("obstacleCount");
		int energy = params.getInteger("energy");
		int changeInterval = params.getInteger("changeInterval");
		
		DataCollector.INSTANCE.insertGlobalData("agentCount", agents);
		DataCollector.INSTANCE.insertGlobalData("holeCount", holes);
		DataCollector.INSTANCE.insertGlobalData("tileCount", tiles);
		DataCollector.INSTANCE.insertGlobalData("homeCount", homes);
		DataCollector.INSTANCE.insertGlobalData("obstacleCount", obstacles);
		DataCollector.INSTANCE.insertGlobalData("energy", energy);
		DataCollector.INSTANCE.insertGlobalData("changeInterval", changeInterval);
		
		// Add agents
		for (int i = 0; i < homes; i++) {
			int homeEnergy = RandomHelper.nextIntFromTo(4, 10);
			this.context.add(new Home(this.space, this.grid, homeEnergy));
			}
		
		// Add agents
		for (int i = 0; i < agents; i++) {
			this.context.add(new Agent(this.space, this.grid, i+1, energy, changeInterval));
		}
		
		// Add holes
		for (int i = 0; i < holes; i++) {			
			int score = RandomHelper.nextIntFromTo(0, 10);
			this.context.add(new Hole(this.space, this.grid, score));
		}
		
		 
		// Add Tiles
		for (int i = 0; i < tiles; i++) {
			this.context.add(new Tile(this.space, this.grid, changeInterval));
		}
		
		// Add obstacles
		for (int i = 0; i < obstacles; i++) {			
			// 0 horizontal, 1 vertical
			int length = RandomHelper.nextIntFromTo(1, 5);
			int orientation = RandomHelper.nextIntFromTo(1, 2);
			Obstacle origialObstacle = new Obstacle(this.space, this.grid, changeInterval, length, orientation-1, i+1);
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

	@ScheduledMethod(start = 1, interval = 100) // Assuming 'n' is 10
    public void updateContext() {
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