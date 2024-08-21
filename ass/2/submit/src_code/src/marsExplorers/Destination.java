package marsExplorers;

import java.util.ArrayList;
import java.util.List;

import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;

public class Destination {
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	private int id;

	public int groupId;
	private List<Material> tradedMaterial;

	public Destination(ContinuousSpace<Object> space, Grid<Object> grid, int id, int groupId) {
		this.space = space;
		this.grid = grid;
		this.id = id;
		this.groupId = groupId;
		this.tradedMaterial = new ArrayList<>();
	}

	/**
	 * return the utility of the material
	 */
	public double exchangeMaterial(Material material) {
		tradedMaterial.add(material);
		return Math.log(1 + material.getWeight());
	}
}