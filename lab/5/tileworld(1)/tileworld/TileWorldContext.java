package tileworld;


import repast.simphony.context.Context;
import repast.simphony.context.space.continuous.ContinuousSpaceFactory;
import repast.simphony.context.space.continuous.ContinuousSpaceFactoryFinder;
import repast.simphony.context.space.grid.GridFactory;
import repast.simphony.context.space.grid.GridFactoryFinder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.random.RandomHelper;
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
		int height = 50;
		int width = 100;
		
		context.setId("tileworld");
		
		ContinuousSpaceFactory spaceFactory = ContinuousSpaceFactoryFinder.createContinuousSpaceFactory(null);
		ContinuousSpace<Object> space =  spaceFactory.createContinuousSpace("space", context,
				new RandomCartesianAdder<Object>(),
				new repast.simphony.space.continuous.WrapAroundBorders(), width, height);

		GridFactory gridFactory = GridFactoryFinder.createGridFactory(null);
		Grid<Object> grid = gridFactory.createGrid("grid", context,
				new GridBuilderParameters<Object>( new WrapAroundBorders(),
				new SimpleGridAdder<Object>(),
				true, width, height));
		
		//Populate the context
		int agents = 1;
		int holes = 50;
		int tiles = 20;
		int home = 1;
		//int obstacles = 1;
		
		//Add agents
		for (int i = 0; i < home; i++) {
			int energy = RandomHelper.nextIntFromTo(4, 10);
			context.add(new Home(space, grid,energy));
			}
		
		//Add agents
		for (int i = 0; i < agents; i++) {
			context.add(new Agent(space, grid,500,i+1));
		}
		
		//Add holes
		for (int i = 0; i < holes; i++) {			
			int score = RandomHelper.nextIntFromTo(1, 10);
			context.add(new Hole(space, grid, score));
		}
		
		//Add Tiles
		for (int i = 0; i < tiles; i++) {
			context.add(new Tile(space, grid));
		}
		
		// Add obstacles
		/*for (int i = 0; i < obstacles; i++) {
			// 0 horizontal, 1 vertical
			int length = RandomHelper.nextIntFromTo(1, 5);
			context.add(new Obstacle(space, grid, length, 0));
		}*/
		
		for(Object obj: context) {
			NdPoint pt = space.getLocation(obj);
			
			/*if (obj instanceof Home) {
				//grid.moveTo(obj, 1, 1);
			} else {*/
				grid.moveTo(obj, (int)pt.getX(), (int)pt.getY());
			//}
			
		}
				
		return context;
	}

}
