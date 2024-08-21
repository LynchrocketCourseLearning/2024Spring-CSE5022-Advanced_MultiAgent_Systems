package delivery_drones;

import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.random.RandomHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.grid.Grid;

public class ManagerAgent {
    private ContinuousSpace<Object> space;
    private Grid<Object> grid;
    private Context<Object> context;
    private List<Parcel> parcels;
    private Parcel currentParcel;
    
    public ManagerAgent(ContinuousSpace<Object> space, Grid<Object> grid, Context<Object> context) {
    	this.space = space;
        this.grid = grid;
        this.context = context;
        this.parcels = new ArrayList<>();
        createParcels();
    }
    
    
    private void createParcels() {
        //TO DO
    }

    private NdPoint generateRandomLocation() {
        // TO DO
    }

    @ScheduledMethod(start = 1, interval = 20)
    public void step() {
        if (!parcels.isEmpty()) {
            currentParcel = parcels.remove(0);
            announceParcel(currentParcel);
        }
    }

    private void announceParcel(Parcel parcel) {
        // TO DO
    }

    public void receiveBid(Bid bid) {
        // TO DO
    }
    
    private void awardParcel(Bid bid) {
        // TO DO
    }
    
    
    
    
}
