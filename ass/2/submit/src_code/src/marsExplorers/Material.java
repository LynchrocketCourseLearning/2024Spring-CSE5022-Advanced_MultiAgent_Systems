package marsExplorers;

import java.util.ArrayList;
import java.util.List;

import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import repast.simphony.util.ContextUtils;

public class Material {
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	private int id;

	private double weight;

	public Material(ContinuousSpace<Object> space, Grid<Object> grid, int id) {
		this.space = space;
		this.grid = grid;
		this.id = id;
		this.weight = RandomHelper.nextDoubleFromTo(1.0, 100.0);
	}

	@ScheduledMethod(start = 0, interval = 1)
	public void updateMineSpots() {
		double tick = RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
		// Resources will be rebuild every 30 events
		double change_control = 30.0;
		double control = tick % change_control;

		if (tick == 0.0) {
			populateMine(this);
		}

		if ((tick > 1.0) && (control == 0.0)) {
			System.out.print("Rebuilding");
			rebuildMines();
		}
	}

	public boolean isCellOccupied(Grid<Object> grid, int x, int y) {
		return grid.getObjectsAt(x, y).iterator().hasNext();
	}

	public void populateMine(Material preciousMaterial) {
		int mineSize = RandomHelper.nextIntFromTo(3, 15);
		double squareRoot = Math.sqrt(mineSize);
		int squareRootInt = (int) squareRoot;
		Context<Object> context = ContextUtils.getContext(preciousMaterial);
		GridPoint pt = grid.getLocation(preciousMaterial);

		while (mineSize > 0) {
			int X_dist = RandomHelper.nextIntFromTo(-squareRootInt, squareRootInt);
			int Y_dist = RandomHelper.nextIntFromTo(-squareRootInt, squareRootInt);

			if (!isCellOccupied(grid, (int) pt.getX() + X_dist, (int) pt.getY() + Y_dist)) {
				Material sub_mine = new Material(this.space, this.grid, preciousMaterial.id);
				context.add(sub_mine);
				grid.moveTo(sub_mine, (int) pt.getX() + X_dist, (int) pt.getY() + Y_dist); // Move each horizontally
				mineSize--;
			}

		}
	}

	public int verifyResources() {
		int existing_spots = 0;
		Context<Object> context = ContextUtils.getContext(this);
		for (Object obj : context) {
			if (obj instanceof Material) {
				existing_spots++;
			}
		}
		return existing_spots;
	}

	public void rebuildMines() {
		Context<Object> context = ContextUtils.getContext(this);

		List<Material> minesList = new ArrayList<>();
		for (Object obj : context) {
			if (obj instanceof Material mineSpot) {
				minesList.add(mineSpot);
			}
		}

		if (minesList.size() > 0) {
			for (Material mineSpot : minesList) {
				context.remove(mineSpot);
			}
		}

		int mines = RandomHelper.nextIntFromTo(1, 10);
		for (int i = 0; i < mines; i++) {
			Material preciousMaterial = new Material(space, grid, i + 1);
			context.add(preciousMaterial);

			int xLocation = RandomHelper.nextIntFromTo(1, 100);
			int yLocation = RandomHelper.nextIntFromTo(1, 50);
			this.grid.moveTo(preciousMaterial, (int) xLocation, (int) yLocation);
			populateMine(preciousMaterial);
		}
	}

	public double getWeight() {
		return this.weight;
	}
}
