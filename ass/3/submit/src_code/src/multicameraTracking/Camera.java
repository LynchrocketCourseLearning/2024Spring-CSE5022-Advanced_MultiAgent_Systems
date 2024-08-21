package multicameraTracking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import multicameraTracking.AuctionManager.AuctionStrategy;
import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.query.space.grid.GridCell;
import repast.simphony.query.space.grid.GridCellNgh;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.graph.Network;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import repast.simphony.util.ContextUtils;
import repast.simphony.util.SimUtilities;

public class Camera {
    private ContinuousSpace<Object> space;
    private Grid<Object> grid;
    private int id;

    private int fovRadio; // Field of view radius for object tracking
    private Map<ObjectInterest, Double> trackedObjects;
    private Map<Camera, Double> pheromoneStrengths;
    private AuctionManager auctionManager;
    private boolean active;

    public double p;
    public double r;

    public Camera(ContinuousSpace<Object> space, Grid<Object> grid, int id, int auctionStrategy) {
        this.space = space;
        this.grid = grid;
        this.id = id;

        this.fovRadio = 5;
        this.trackedObjects = new HashMap<>();
        this.pheromoneStrengths = new HashMap<>();
        this.auctionManager = new AuctionManager(this, auctionStrategy);
        this.active = true;

        this.p = 0;
        this.r = 0;
    }

    // Periodic check for object tracking and potential handovers
    @ScheduledMethod(start = 1, interval = 1)
    public void step() {
        this.p = 0;
        // Track nearby objects of interest
        trackObject();
        // Check surroundings and determine if any tracked object is leaving the view
        List<ObjectInterest> currentObjects = new ArrayList<>(trackedObjects.keySet());
        for (ObjectInterest obj : currentObjects) {
            if (!isInCameraView(grid.getLocation(obj))) {
                System.out.println("Initiating Handover");
                initiateHandover(obj);
            }
        }
        System.out.print(numberOfActiveCameras());
    }

    // Track objects of interest within the camera's field of view
    public void trackObject() {
        Network<Object> net = getNetwork("camerasNetwork");

        List<GridCell<ObjectInterest>> gridCells = getThisGridNeighbour(ObjectInterest.class, true, fovRadio, fovRadio);
        for (GridCell<ObjectInterest> cell : gridCells) {
            for (ObjectInterest object : cell.items()) {
                if (!object.isTracked() && isInCameraView(grid.getLocation(object))) {
                    trackedObjects.put(object, 0.0);
                    object.setTracking(true);
                    net.addEdge(this, object, 0.0);
                }
            }
        }
    }

    // Initiates handover for objects leaving the view
    private void initiateHandover(ObjectInterest objectToHandover) {
        List<Camera> neighboringCameras = findNeighboringCameras().stream()
                .filter(camera -> camera.isInCameraView(grid.getLocation(objectToHandover)))
                .toList();

        if (neighboringCameras.isEmpty()) {
            return;
        }
        auctionManager.handoverEvent(neighboringCameras, objectToHandover);
    }

    // Find neighboring cameras within a certain distance
    private List<Camera> findNeighboringCameras() {
        List<Camera> neighbors = new ArrayList<>();

        List<GridCell<Camera>> gridCells = getThisGridNeighbour(Camera.class, false, 20, 20);
        for (GridCell<Camera> cell : gridCells) {
            for (Camera camera : cell.items()) {
                if (camera != this) {
                    neighbors.add(camera);
                }
            }
        }

        return neighbors;
    }

    // Finalize handover by transferring tracking responsibility
    public void finalizeHandover(Camera winner, ObjectInterest objectToHandover, double pricePaid) {
        System.out.println("Camera " + id + " hands over object to Camera " + winner.id + " at price " + pricePaid);

        Network<Object> net = getNetwork("camerasNetwork");
        // Remove the object from current camera's tracking list and network
        trackedObjects.remove(objectToHandover);
        net.removeEdge(net.getEdge(this, objectToHandover));
        objectToHandover.setTracking(false);
        this.r += pricePaid;

        // Add the object to the winner's tracking list and network with its bid value
        winner.trackedObjects.put(objectToHandover, pricePaid);
        objectToHandover.setTracking(true);
        winner.p += pricePaid;

        // utility
        double utility = calculateUtility(objectToHandover);
        net.addEdge(winner, objectToHandover, utility);
    }

    // Calculate bid based on camera's strategy
    public double calculateBid(Object objectToHandover, AuctionStrategy strategy) {
        return switch (strategy) {
            case Vickrey -> Math.random() * 100;
            case Dutch -> (Math.random() + 2) * 100;
        };
    }

    public double calculateUtility(ObjectInterest objectOfInterest) {
        // Example: Calculate utility based on object importance and threat level
        // double importance = objectOfInterest.getImportance(); // Example: 0 to 100
        // double threadLevel = objectOfInterest.getThreadLevel(); // Example: 0 to 100
        // double totalPheromoneStrength =
        // pheromoneStrengths.values().stream().mapToDouble(Double::doubleValue).sum();

        // Utility calculation
        double confidence = calculateConfidence(grid.getLocation(objectOfInterest));
        double visibility = calculateVisibility(grid.getLocation(objectOfInterest));
        double track = calculateTrack(objectOfInterest);

        double utility = confidence + visibility + track;

        return utility;
    }

    private double calculateVisibility(GridPoint objectLocation) {
        if (!isInCameraView(objectLocation)) {
            return 0.0;
        }
        double distance = grid.getDistance(objectLocation, grid.getLocation(this));
        double maxDistance = this.fovRadio;
        return (distance <= maxDistance) ? 1.0 - (distance / maxDistance) : 0.0;
    }

    private double calculateConfidence(GridPoint objectLocation) {
        double visibility = calculateVisibility(objectLocation);
        double confidence = Math.sqrt(visibility);
        return confidence;
    }

    private double calculateTrack(ObjectInterest obj) {
        return (trackedObjects.containsKey(obj)) ? 1.0 : 0.0;
    }

    // Reduce surveillance effort for the object
    private void reduceSurveillanceEffort(ObjectInterest objectOfInterest) {
        // Example: Reduce surveillance frequency by adjusting update interval
        objectOfInterest.setUpdateInterval(objectOfInterest.getUpdateInterval() * 2);

        // Example: Log or take other actions when reducing effort
        System.out.println("Camera " + this.id + " reduces effort for object " + objectOfInterest.id);
    }

    // Increase surveillance effort for the object
    private void increaseSurveillanceEffort(ObjectInterest objectOfInterest) {
        // Example: Increase surveillance frequency by adjusting update interval
        objectOfInterest.setUpdateInterval(objectOfInterest.getUpdateInterval() / 2);

        // Example: Log or take other actions when increasing effort
        System.out.println("Camera " + this.id + " increases effort for object " + objectOfInterest.id);
    }

    // Periodic adjustment of surveillance efforts based on object utility
    @ScheduledMethod(start = 1, interval = 5) // Adjust every 5 ticks
    public void adjustSurveillanceEfforts() {
        for (ObjectInterest objectOfInterest : trackedObjects.keySet()) {
            List<Camera> neighboringCameras = findNeighboringCameras();
            adjustSurveillance(neighboringCameras, objectOfInterest);
        }
    }

    // Adjust surveillance based on utility comparison with neighboring cameras
    public void adjustSurveillance(List<Camera> neighboringCameras, ObjectInterest objectOfInterest) {
        double myUtility = calculateUtility(objectOfInterest);
        boolean shouldIncreaseEffort = true;

        // Get utility values from neighboring cameras for comparison
        for (Camera neighbor : neighboringCameras) {
            double neighborUtility = neighbor.calculateUtility(objectOfInterest);

            if (neighborUtility > myUtility) {
                // If any neighbor has a higher utility for the same object, reduce own
                // surveillance effort
                shouldIncreaseEffort = false;
                break; // No need to check further if we already decide to reduce effort
            }
        }

        // Adjust surveillance effort based on comparison
        if (shouldIncreaseEffort) {
            increaseSurveillanceEffort(objectOfInterest);
        } else {
            reduceSurveillanceEffort(objectOfInterest);
        }
    }

    // Vision Graph Generation
    // Update pheromone strengths based on trading activity
    public void updatePheromones(Camera other, boolean tradeOccurred) {
        double decayRate = 0.005; // Example decay rate
        double increase = 1.0; // Increase factor on successful trade
        double currentStrength = pheromoneStrengths.getOrDefault(other, 0.0);

        if (tradeOccurred) {
            pheromoneStrengths.put(other, (1 - decayRate) * currentStrength + increase);
        } else {
            pheromoneStrengths.put(other, (1 - decayRate) * currentStrength);
        }
    }

    public int getId() {
        return this.id;
    }

    public boolean isActive() {
        return this.active;
    }

    public double totalUtility() {
        double u = trackedObjects.keySet().stream().map(obj -> calculateUtility(obj)).reduce(0.0, (a, b) -> a + b);
        return u - p + r;
    }

    public double getRandomBid() {
        return 1 + Math.random() * 100;
    }

    // Method to get the current vision graph as a map
    public Map<Camera, Double> getPheromoneGraph() {
        return new HashMap<>(pheromoneStrengths);
    }

    // Determine if a point is within the camera's view
    private boolean isInCameraView(GridPoint pt) {
        GridPoint myLocation = grid.getLocation(this);
        return Math.abs(myLocation.getX() - pt.getX()) <= fovRadio
                && Math.abs(myLocation.getY() - pt.getY()) <= fovRadio;
    }

    public int numberOfActiveCameras() {
        Context<Object> context = ContextUtils.getContext(this);
        int activeCount = context.stream()
                .filter(obj -> obj instanceof Camera camera && camera.isActive())
                .mapToInt(e -> 1).sum();
        return activeCount;
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

    public Network<Object> getNetwork(String netName) {
        Context<Object> context = ContextUtils.getContext(this);
        return (Network<Object>) context.getProjection(netName);
    }
}
