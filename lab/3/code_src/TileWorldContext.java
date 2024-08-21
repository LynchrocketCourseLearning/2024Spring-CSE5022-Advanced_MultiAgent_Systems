package tileWorld;

import repast.simphony.context.Context;
import repast.simphony.context.space.continuous.ContinuousSpaceFactory;
import repast.simphony.context.space.continuous.ContinuousSpaceFactoryFinder;
import repast.simphony.context.space.grid.GridFactory;
import repast.simphony.context.space.grid.GridFactoryFinder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.parameter.Parameters;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.continuous.RandomCartesianAdder;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridBuilderParameters;
import repast.simphony.space.grid.SimpleGridAdder;
import repast.simphony.space.grid.WrapAroundBorders;

public class TileWorldContext implements ContextBuilder<Object> {
	@Override
	public Context build(Context<Object> context) {
		
		context.setId("TileWorld");
		
		ContinuousSpaceFactory spaceFactory = ContinuousSpaceFactoryFinder.createContinuousSpaceFactory(null);
		ContinuousSpace<Object> space =  spaceFactory.createContinuousSpace("space", context,
				new RandomCartesianAdder<Object>(),
				new repast.simphony.space.continuous.WrapAroundBorders(), 100, 50);

		GridFactory gridFactory = GridFactoryFinder.createGridFactory(null);
		Grid<Object> grid = gridFactory.createGrid("grid", context,
				new GridBuilderParameters<Object>( new WrapAroundBorders(),
				new SimpleGridAdder<Object>(),
				true, 100, 50));
		
		//Populate the context
		Parameters params = RunEnvironment.getInstance().getParameters();
		int agents = params.getInteger("agentCount");
		int holes = params.getInteger("holeCount");
		int tiles = params.getInteger("tileCount");
		int homes = params.getInteger("homeCount");
		int obstacles = params.getInteger("obstacleCount");
		
		int energy = params.getInteger("energy");
		//Add agents
		for (int i = 0; i < agents; i++) {
			context.add(new Agent(space, grid, energy, i+1));
		}
		
		//Add holes
		for (int i = 0; i < holes; i++) {
			context.add(new Hole(space, grid, 0));
		}
		
		//Add Tiles
		for (int i = 0; i < tiles; i++) {
			context.add(new Tile(space, grid));
		}
		
		//Add Homes
		for (int i = 0; i < homes; i++) {
			context.add(new Home(space, grid));
		}
		
		//Add Obstacles
		for (int i = 0; i < obstacles; i++) {
			context.add(new Obstacle(space, grid));
		}
		
		for(Object obj: context) {
			NdPoint pt = space.getLocation(obj);
			grid.moveTo(obj, (int)pt.getX(), (int)pt.getY());			
		}
				
		return context;
	}

}