package marsExplorers;

import java.util.ArrayList;
import java.util.List;

import repast.simphony.context.Context;
import repast.simphony.context.space.continuous.ContinuousSpaceFactory;
import repast.simphony.context.space.continuous.ContinuousSpaceFactoryFinder;
import repast.simphony.context.space.graph.NetworkBuilder;
import repast.simphony.context.space.grid.GridFactory;
import repast.simphony.context.space.grid.GridFactoryFinder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.parameter.Parameters;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.continuous.RandomCartesianAdder;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridBuilderParameters;
import repast.simphony.space.grid.GridPoint;
import repast.simphony.space.grid.SimpleGridAdder;
import repast.simphony.space.grid.WrapAroundBorders;

public class MarsBuilder implements ContextBuilder<Object> {
	private Context<Object> context;
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;

	@Override
	public Context build(Context<Object> context) {
		this.context = context;
		context.setId("MarsExplorers");
		int height = 100;
		int width = 50;

		NetworkBuilder<Object> netBuilder = new NetworkBuilder<Object>("agents_network", context, true);
		netBuilder.buildNetwork();

		ContinuousSpaceFactory spaceFactory = ContinuousSpaceFactoryFinder.createContinuousSpaceFactory(null);
		this.space = spaceFactory.createContinuousSpace(
				"space",
				context,
				new RandomCartesianAdder<Object>(),
				new repast.simphony.space.continuous.WrapAroundBorders(),
				height,
				width);

		GridFactory gridFactory = GridFactoryFinder.createGridFactory(null);
		this.grid = gridFactory.createGrid(
				"grid",
				context,
				new GridBuilderParameters<Object>(
						new WrapAroundBorders(),
						new SimpleGridAdder<Object>(),
						true,
						height,
						width));

		this.initialSetup();

		return context;
	}

	private void initialSetup() {
		Parameters params = RunEnvironment.getInstance().getParameters();
		int explorerNum = params.getInteger("explorerNum");
		int traderNum = params.getInteger("traderNum");
		int materialNum = RandomHelper.nextIntFromTo(10, 50);

		// only one base station
		context.add(new BaseStation(space, grid));

		for (int i = 0; i < explorerNum; i++) {
			context.add(new Explorer(space, grid, i, i));
		}

		for (int i = 0; i < explorerNum; i++) {
			context.add(new Destination(space, grid, i, i));
		}

		for (int i = 0; i < materialNum; i++) {
			context.add(new Material(space, grid, i));
		}

		for (int i = 0; i < traderNum; i++) {
			context.add(new Trader(space, grid, i));
		}

		// allocate the explorers to the base station
		GridPoint baseGridPoint = null;
		NdPoint baseNdPoint = null;
		for (Object obj : context) {
			NdPoint pt = space.getLocation(obj);
			grid.moveTo(obj, (int) pt.getX(), (int) pt.getY());
			if (obj instanceof BaseStation bs) {
				baseGridPoint = grid.getLocation(bs);
				baseNdPoint = new NdPoint(baseGridPoint.getX(), baseGridPoint.getY());
			}
		}

		for (Object obj : context) {
			if (obj instanceof Explorer) {
				space.moveTo(obj, baseNdPoint.getX(), baseNdPoint.getY());
				grid.moveTo(obj, baseGridPoint.getX(), baseGridPoint.getY());
			}
		}
	}
}
