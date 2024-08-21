package marsExplorers;

import java.util.ArrayList;
import java.util.List;

import marsExplorers.Message.MessageType;
import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.query.space.grid.GridCell;
import repast.simphony.query.space.grid.GridCellNgh;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import repast.simphony.util.ContextUtils;
import repast.simphony.util.SimUtilities;

public class Explorer {
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	private int id;

	/**
	 * the ratio in sensing the environment
	 */
	private int senseRatio;

	/**
	 * the points of the destinations
	 * 
	 * An agent has only one destination, but if multiple agents are associated into
	 * a group, they will share their destinations.
	 * 
	 * It can also used to check the size of a group.
	 */
	private List<GridPoint> destSpots;
	private List<GridPoint> materialSpots;
	private boolean inGroup;
	private boolean proceedingGroup;
	private int groupId;

	private List<Material> materialsCarried;

	private double surviveLevel;
	private double dyingThreshold;

	private MessageManager messageManager;
	private MoveManager moveManager;

	// Extra Features
	private double maxCapacity;
	private double extraResources;
	private double utility;

	public Explorer(ContinuousSpace<Object> space, Grid<Object> grid, int id, int groupId) {
		this.space = space;
		this.grid = grid;
		this.id = id;

		this.senseRatio = 5;

		this.destSpots = new ArrayList<>();
		this.materialSpots = new ArrayList<>();
		this.inGroup = this.proceedingGroup = false;
		this.groupId = groupId;

		this.messageManager = new MessageManager(this, 10);
		this.moveManager = new MoveManager(this, this.senseRatio, 1);

		this.materialsCarried = new ArrayList<>();

		this.surviveLevel = 100.0;
		this.dyingThreshold = this.surviveLevel / 2;

		this.maxCapacity = RandomHelper.nextDoubleFromTo(100.0, 200.0);
		this.extraResources = 0.0;
		this.utility = 0;
	}

	@ScheduledMethod(start = 0, interval = 1)
	public void step() {
		// always sense the destinations if the agent has not found its destination
		if (this.destSpots.isEmpty()) {
			senseDestination();
		}

		// An agent is able to make an alliance with other Agents if they are not in
		// group
		if (!inGroup) {
			broadcastMessage(new Message(this, getGridLocation(), MessageType.FINDING_GROUP), false);
		}

		if (this.surviveLevel > 0) {
			proceedAtCurrentSpot();
			if (this.surviveLevel < this.dyingThreshold) {
				GridPoint refillSpot = findWayToRefill();
				moveManager.setTarget(refillSpot);
				System.out.println("To refill at: " + refillSpot);
			} else {
				double totalWeight = this.materialsCarried.stream()
						.mapToDouble(Material::getWeight)
						.sum();
				// find traders or destinations if carried at least half capacity
				if (totalWeight >= this.maxCapacity / 2) {
					GridPoint traderSpot = findTraders();
					moveManager.setTarget(traderSpot);
					System.out.println("Find trader at: " + traderSpot);
				} else {
					GridPoint materialSpot = findMaterials();
					moveManager.setTarget(materialSpot);
					broadcastMessage(new Message(this, materialSpot, MessageType.MATERIAL_FOUND), true);
					System.out.println("Find material at: " + materialSpot);
				}
			}
			moveManager.moveTowardsTarget();
			// if (moveManager.hasReached()) {
			// System.out.println("Reached at: " + getGridLocation());
			// proceedAtCurrentSpot();
			// moveManager.setReached(false);
			// } else if (moveManager.hasTarget()) {
			// moveManager.moveTowardsTarget();
			// } else if (moveManager.hasNextTarget()) {
			// moveManager.switchNextTarget();
			// moveManager.moveTowardsTarget();
			// } else {
			// // find traders or return to destination
			// if (this.surviveLevel < this.dyingThreshold) {
			// GridPoint refillSpot = findWayToRefill();
			// moveManager.setTarget(refillSpot);
			// moveManager.moveTowardsTarget();
			// System.out.println("To refill at: " + refillSpot);
			// } else {
			// double totalWeight = this.materialsCarried.stream()
			// .mapToDouble(Material::getWeight)
			// .sum();
			// // find traders or destinations if carried at least half capacity
			// if (totalWeight >= this.maxCapacity / 2) {
			// GridPoint traderSpot = findTraders();
			// moveManager.setTarget(traderSpot);
			// System.out.println("Find trader at: " + traderSpot);
			// } else {
			// GridPoint materialSpot = findMaterials();
			// moveManager.setTarget(materialSpot);
			// broadcastMessage(new Message(this, materialSpot, MessageType.MATERIAL_FOUND),
			// true);
			// System.out.println("Find material at: " + materialSpot);
			// }
			// moveManager.moveTowardsTarget();
			// }
			// }
		} else {
			broadcastMessage(new Message(this, grid.getLocation(this), MessageType.NEED_HELP), true);
		}

		processMessages();

		// decrease even though no moving
		this.surviveLevel -= 0.1;

		double totalWeight = this.materialsCarried.stream()
				.mapToDouble(Material::getWeight)
				.sum();

		System.out.println("Agent: " + id +
				", group: " + groupId +
				", current load: " + totalWeight +
				", current life: " + surviveLevel +
				", utility: " + utility);
	}

	/**
	 * find destination around
	 */
	public void senseDestination() {
		List<GridCell<Destination>> nghDests = getThisGridNeighbour(Destination.class, false, senseRatio, senseRatio);
		GridPoint destPt = null;
		// find the destination of the same group
		for (GridCell<Destination> ngh : nghDests) {
			if (ngh.size() > 0) {
				for (Destination dest : ngh.items()) {
					if (dest.groupId == this.groupId) {
						destPt = ngh.getPoint();
						break;
					}
				}
			}
			if (destPt != null) {
				break;
			}
		}
		if (destPt != null) {
			this.destSpots.add(destPt);
			broadcastMessage(new Message(this, destPt, MessageType.DEST_FOUND), true);
		}
	}

	/**
	 * find the nearest way to refill the survival level
	 * 1. return to destination or,
	 * 2. find a trader.
	 */
	public GridPoint findWayToRefill() {
		GridPoint thisPt = grid.getLocation(this);

		GridPoint spot = null;
		// double minDist = 1000000.0;
		int maxCnt = -1;

		if (!destSpots.isEmpty()) {
			if (destSpots.size() > 1) {
				destSpots.sort((pt1, pt2) -> (int) (grid.getDistance(pt1, thisPt) - grid.getDistance(pt2, thisPt)));
			}
			spot = destSpots.get(0);
			// minDist = grid.getDistance(spot, thisPt);
		}

		List<GridCell<Trader>> nghTraders = getThisGridNeighbour(Trader.class, true, senseRatio, senseRatio);
		for (GridCell<Trader> cell : nghTraders) {
			// double dist = grid.getDistance(cell.getPoint(), thisPt);
			// if (dist < minDist) {
			// spot = cell.getPoint();
			// minDist = dist;
			// }
			if (maxCnt < cell.size()) {
				spot = cell.getPoint();
				maxCnt = cell.size();
			}
		}

		return spot;
	}

	/**
	 * find the nearest trader to trade
	 */
	public GridPoint findTraders() {
		GridPoint thisPt = grid.getLocation(this);

		GridPoint spot = null;
		// double minDist = 1000000.0;
		int maxCnt = -1;

		List<GridCell<Trader>> nghTraders = getThisGridNeighbour(Trader.class, true, senseRatio, senseRatio);
		for (GridCell<Trader> cell : nghTraders) {
			// double dist = grid.getDistance(cell.getPoint(), thisPt);
			// if (dist < minDist) {
			// spot = cell.getPoint();
			// minDist = dist;
			// }
			if (maxCnt < cell.size()) {
				spot = cell.getPoint();
				maxCnt = cell.size();
			}
		}

		return spot;
	}

	/**
	 * find the nearest material mine to dig
	 */
	private GridPoint findMaterials() {
		GridPoint thisPt = grid.getLocation(this);

		GridPoint spot = null;
		// double minDist = 1000000.0;
		int maxCnt = -1;

		if (!materialSpots.isEmpty()) {
			if (materialSpots.size() > 1) {
				materialSpots.sort((pt1, pt2) -> (int) (grid.getDistance(pt1, thisPt) - grid.getDistance(pt2, thisPt)));
			}
			spot = materialSpots.get(0);
			// minDist = grid.getDistance(spot, thisPt);
		}

		List<GridCell<Material>> nghMaterials = getThisGridNeighbour(Material.class, true, senseRatio, senseRatio);
		for (GridCell<Material> cell : nghMaterials) {
			// double dist = grid.getDistance(cell.getPoint(), thisPt);
			// if (dist < minDist) {
			// spot = cell.getPoint();
			// minDist = dist;
			// }
			if (maxCnt < cell.size()) {
				spot = cell.getPoint();
				maxCnt = cell.size();
			}
		}

		return spot;
	}

	/**
	 * mine for materials
	 */
	public void mineForMaterials(Material material) {
		System.out.println("Mining!");
		double totalWeight = this.materialsCarried.stream()
				.mapToDouble(Material::getWeight)
				.sum();
		if (totalWeight + material.getWeight() < maxCapacity) {
			this.materialsCarried.add(material);
			removeFromContext(material);
		}
	}

	/**
	 * download resources
	 */
	public void downloadMaterials(Destination destination) {
		System.out.println("Downloading!");
		if (this.materialsCarried.size() > 0) {
			this.surviveLevel = 100.0;
			this.materialsCarried.sort((m1, m2) -> (int) (m1.getWeight() - m2.getWeight()));
			Material material = this.materialsCarried.remove(0);
			this.utility += destination.exchangeMaterial(material);
		}
	}

	/**
	 * trade reources
	 */
	public void tradeMaterials(Trader trader) {
		System.out.println("Trading!");
		List<Material> materialsToDelete = new ArrayList<>();
		if (!materialsCarried.isEmpty()) {
			for (Material material : materialsCarried) {
				double priceLowerBound = (surviveLevel < dyingThreshold) ? 0 : Math.log(1 + material.getWeight());
				double bid = trader.trade(material, priceLowerBound);
				if (bid > 0.0) {
					double finalBid = trader.acceptTrade(material);
					this.utility += bid;
					this.surviveLevel += bid;
					this.extraResources += (finalBid - bid);
					materialsToDelete.add(material);
				} else {
					break;
				}
			}
		}
		for (Material material : materialsToDelete) {
			materialsCarried.remove(material);
		}
		if (extraResources > 0.0) {
			List<Material> materials = trader.acceptTrade(extraResources);
			materialsCarried.addAll(materials);
			this.extraResources = 0.0;
		}
	}

	public void proceedAtCurrentSpot() {
		GridPoint thisPt = getGridLocation();
		Object obj = grid.getRandomObjectAt(thisPt.getX(), thisPt.getY());
		if (obj instanceof Material material) {
			mineForMaterials(material);
			if (!grid.getObjectsAt(thisPt.getX(), thisPt.getY()).iterator().hasNext()) {
				broadcastMessage(new Message(this, thisPt, MessageType.NORMAL_EXPIRED), true);
			}
		} else if (obj instanceof Trader trader) {
			tradeMaterials(trader);
		} else if (obj instanceof Destination destination) {
			downloadMaterials(destination);
		}
	}

	// Communication Methods
	public void sendMessage(Explorer recipient, Message message) {
		messageManager.sendMessage(recipient, message);
	}

	public void receiveMessage(Message message) {
		messageManager.receiveMessage(message);
	}

	public void processMessages() {
		messageManager.processMessages();
	}

	public void broadcastMessage(Message message, boolean onlyGroup) {
		messageManager.broadcastMessage(message, onlyGroup);
	}

	// utils
	public int getMaterialCount() {
		return this.materialsCarried.size();
	}

	public int getId() {
		return this.id;
	}

	public int getGroupId() {
		return this.groupId;
	}

	public void setGroupId(int groupId) {
		this.groupId = groupId;
	}

	public boolean isInGroup() {
		return this.inGroup;
	}

	public void setInGroup(boolean inGroup) {
		this.inGroup = inGroup;
	}

	public boolean isInProceedingGroup() {
		return this.proceedingGroup;
	}

	public void setProceedingGroup(boolean proceedingGroup) {
		this.proceedingGroup = proceedingGroup;
	}

	public GridPoint getGridLocation() {
		return grid.getLocation(this);
	}

	public double getDistance(GridPoint pt1, GridPoint pt2) {
		return grid.getDistance(pt1, pt2);
	}

	public boolean isSurvival() {
		return this.surviveLevel > 0;
	}

	public void addDestination(GridPoint destPt) {
		if (!this.destSpots.contains(destPt)) {
			this.destSpots.add(destPt);
		}
	}

	public void addMaterialSpot(GridPoint materialSpot) {
		if (!this.materialSpots.contains(materialSpot)) {
			this.materialSpots.add(materialSpot);
		}
	}

	public boolean nearThanDest(GridPoint pt) {
		GridPoint thisPt = getGridLocation();
		double dist = getDistance(thisPt, pt);
		for (GridPoint gridPoint : destSpots) {
			if (dist > getDistance(thisPt, gridPoint)) {
				return false;
			}
		}
		return true;
	}

	public void setNextTargetPoint(GridPoint pt) {
		moveManager.setNextTarget(pt);
	}

	public double getTick() {
		return RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
	}

	public double getSurvivalLevel() {
		return surviveLevel;
	}

	public double getUtility() {
		return utility;
	}

	/**
	 * get neighbor in extent units
	 */
	public <T> List<GridCell<T>> getThisGridNeighbour(Class<T> clazz, boolean needShuffle, int... extent) {
		return getObjGridNeighbour(this, clazz, needShuffle, extent);
	}

	public <T> List<GridCell<T>> getObjGridNeighbour(Object obj, Class<T> clazz, boolean needShuffle, int... extent) {
		GridPoint pt = grid.getLocation(obj);
		GridCellNgh<T> nghCreator = new GridCellNgh<>(grid, pt, clazz, extent);
		List<GridCell<T>> nghCells = nghCreator.getNeighborhood(false);
		if (needShuffle) {
			SimUtilities.shuffle(nghCells, RandomHelper.getUniform());
		}

		return nghCells;
	}

	/**
	 * indicating the point
	 */
	public <T> List<GridCell<T>> getPtGridNeighbour(GridPoint pt, Class<T> clazz, boolean needShuffle, int... extent) {
		GridCellNgh<T> nghCreator = new GridCellNgh<>(grid, pt, clazz, extent);
		List<GridCell<T>> nghCells = nghCreator.getNeighborhood(false);
		if (needShuffle) {
			SimUtilities.shuffle(nghCells, RandomHelper.getUniform());
		}

		return nghCells;
	}

	public void removeFromContext(Object obj) {
		Context<Object> context = ContextUtils.getContext(obj);
		context.remove(obj);
	}

	public void moveTo(GridPoint pt) {
		space.moveTo(this, pt.getX(), pt.getY());
		grid.moveTo(this, pt.getX(), pt.getY());
	}

	public ContinuousSpace<Object> getSpace() {
		return space;
	}

	public Grid<Object> getGrid() {
		return grid;
	}
}
