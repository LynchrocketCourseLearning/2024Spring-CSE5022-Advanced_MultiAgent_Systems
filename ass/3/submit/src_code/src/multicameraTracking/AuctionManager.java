package multicameraTracking;

import java.util.List;

import repast.simphony.random.RandomHelper;
import repast.simphony.space.graph.Network;

public class AuctionManager {
    private Camera auctioneer;
    private AuctionStrategy auctionStrategy;

    public AuctionManager(Camera auctioneer, int auctionStrategy) {
        this.auctioneer = auctioneer;
        auctionStrategy = (auctionStrategy < 0 || auctionStrategy >= AuctionStrategy.values().length)
                ? 0
                : auctionStrategy;
        this.auctionStrategy = AuctionStrategy.values()[auctionStrategy];
    }

    public void handoverEvent(List<Camera> neighboringCameras, ObjectInterest objectToHandover) {
        switch (auctionStrategy) {
            case Vickrey -> handoverEventVickrey(neighboringCameras, objectToHandover);
            case Dutch -> handoverEventDutch(neighboringCameras, objectToHandover);
        }
    }

    // Conduct Vickrey auction among neighboring cameras
    private void handoverEventVickrey(List<Camera> neighboringCameras, ObjectInterest objectToHandover) {
        double myBid = auctioneer.calculateBid(objectToHandover, AuctionStrategy.Vickrey);
        double highestBid = myBid;
        double secondHighestBid = 0;
        Camera winner = auctioneer;

        for (Camera neighbor : neighboringCameras) {
            double neighborBid = neighbor.calculateBid(objectToHandover, AuctionStrategy.Vickrey);
            if (neighborBid > highestBid) {
                secondHighestBid = highestBid;
                highestBid = neighborBid;
                winner = neighbor;
            } else if (neighborBid > secondHighestBid) {
                secondHighestBid = neighborBid;
            }
        }

        // Update Pheromones in Vision Graph
        for (Camera neighbor : neighboringCameras) {
            auctioneer.updatePheromones(neighbor, winner == neighbor);
        }

        // Handover if this camera is not the winner
        if (winner != auctioneer) {
            auctioneer.finalizeHandover(winner, objectToHandover, secondHighestBid);
            Network<Object> net = auctioneer.getNetwork("pheromoneNetwork");
            net.addEdge(auctioneer, winner, 0.0);
            System.out.println("A connection between " + auctioneer.getId() + " and " + winner.getId() + " ocurred");
        }
        System.out.printf("My bid: %.2f - Bid winner: %.2f\n", myBid, highestBid);
    }

    // Conduct Dutch auction among neighboring cameras
    private void handoverEventDutch(List<Camera> neighboringCameras, ObjectInterest objectToHandover) {
        double myBid = auctioneer.calculateBid(objectToHandover, AuctionStrategy.Dutch);
        double curBid = myBid;
        double decayRate = 0.05;
        Camera winner = auctioneer;

        while (winner == auctioneer) {
            int idx = RandomHelper.nextIntFromTo(0, neighboringCameras.size() - 1);
            Camera randomCamera = neighboringCameras.get(idx);
            if (randomCamera.getRandomBid() > curBid) {
                winner = randomCamera;
                break;
            }
            curBid = curBid * (1 - decayRate);
        }

        // Update Pheromones in Vision Graph
        for (Camera neighbor : neighboringCameras) {
            auctioneer.updatePheromones(neighbor, winner == neighbor);
        }

        // Handover if this camera is not the winner
        if (winner != auctioneer) {
            auctioneer.finalizeHandover(winner, objectToHandover, curBid);
            Network<Object> net = auctioneer.getNetwork("pheromoneNetwork");
            net.addEdge(auctioneer, winner, 0.0);
            System.out.println("A connection between " + auctioneer.getId() + " and " + winner.getId() + " ocurred");
        }
        System.out.printf("My bid: %.2f - Bid winner: %.2f\n", myBid, curBid);
    }

    public enum AuctionStrategy {
        Vickrey,
        Dutch
    }
}
