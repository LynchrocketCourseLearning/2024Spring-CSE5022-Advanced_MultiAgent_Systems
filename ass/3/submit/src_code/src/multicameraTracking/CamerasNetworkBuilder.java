package multicameraTracking;

import repast.simphony.context.Context;
import repast.simphony.context.space.continuous.ContinuousSpaceFactory;
import repast.simphony.context.space.continuous.ContinuousSpaceFactoryFinder;
import repast.simphony.context.space.graph.NetworkBuilder;
import repast.simphony.context.space.grid.GridFactory;
import repast.simphony.context.space.grid.GridFactoryFinder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.parameter.Parameters;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.continuous.RandomCartesianAdder;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridBuilderParameters;
import repast.simphony.space.grid.GridDimensions;
import repast.simphony.space.grid.SimpleGridAdder;
import repast.simphony.space.grid.WrapAroundBorders;

public class CamerasNetworkBuilder implements ContextBuilder<Object> {

	@Override
	public Context build(Context<Object> context) {
		context.setId("multicameraTracking");
		int height = 64;
		int width = 64;

		NetworkBuilder<Object> netBuilder = new NetworkBuilder<Object>("camerasNetwork", context, true);
		netBuilder.buildNetwork();

		NetworkBuilder<Object> pheromoneNetwork = new NetworkBuilder<Object>("pheromoneNetwork", context, false);
		pheromoneNetwork.buildNetwork();

		ContinuousSpaceFactory spaceFactory = ContinuousSpaceFactoryFinder.createContinuousSpaceFactory(null);
		ContinuousSpace<Object> space = spaceFactory.createContinuousSpace("space", context,
				new RandomCartesianAdder<Object>(),
				new repast.simphony.space.continuous.WrapAroundBorders(), height, width);

		GridFactory gridFactory = GridFactoryFinder.createGridFactory(null);
		Grid<Object> grid = gridFactory.createGrid("grid", context,
				new GridBuilderParameters<Object>(new WrapAroundBorders(),
						new SimpleGridAdder<Object>(),
						true, height, width));

		Parameters params = RunEnvironment.getInstance().getParameters();
		int cameras = 65;
		int objects = 10;
		int auctionStrategy = params.getInteger("auctionStrategy");
		boolean isRandom = params.getBoolean("isRandom");

		for (int i = 0; i < cameras; i++) {
			context.add(new Camera(space, grid, i, auctionStrategy));
		}

		initializeCameras(context, space, grid, isRandom);

		for (int i = 0; i < objects; i++) {
			context.add(new ObjectInterest(space, grid, i));
		}

		for (Object obj : context) {
			NdPoint pt = space.getLocation(obj);
			grid.moveTo(obj, (int) pt.getX(), (int) pt.getY());
		}

		return context;
	}

	private void initializeCameras(Context<Object> context, ContinuousSpace<Object> space, Grid<Object> grid,
			boolean isRandom) {
		GridDimensions dimensions = grid.getDimensions();
		int boardWidth = dimensions.getWidth();
		int boardHeight = dimensions.getHeight();

		int gridWidth = 8;
		int gridHeight = 8;

		int cellWidth = boardWidth / gridWidth;
		int cellHeight = boardHeight / gridHeight;

		int x, y;
		for (Object object : context) {
			if (object instanceof Camera camera) {
				if (isRandom) {
					x = RandomHelper.nextIntFromTo(cellHeight / 2, boardHeight + cellHeight / 2);
					y = RandomHelper.nextIntFromTo(cellWidth / 2, boardWidth + cellWidth / 2);
				} else {
					int row = (camera.getId() - 1) / cellWidth;
					int col = (camera.getId() - 1) % cellWidth;

					x = col * cellWidth + cellWidth / 2;
					y = row * cellHeight + cellHeight / 2;
				}

				space.moveTo(camera, x, y);
				grid.moveTo(camera, x, y);
			}
		}
	}
}
