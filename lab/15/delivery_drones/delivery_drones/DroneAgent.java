package delivery_drones;

import java.util.ArrayList;

import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.grid.Grid;

public class DroneAgent {
    private ContinuousSpace<Object> space;
    private Grid<Object> grid;
    private Context<Object> context;
    private boolean busy;
    private Parcel currentParcel;
    private boolean carryingParcel;
    
    public DroneAgent(ContinuousSpace<Object> space, Grid<Object> grid, Context<Object> context) {
        this.space = space;
        this.grid = grid;
        this.context = context;
        this.busy = false;
        this.carryingParcel = false;
    }

    @ScheduledMethod(start = 1, interval = 1)
    public void step() {

     }

    public void receiveParcelAnnouncement(Parcel parcel) {

    }

    private void submitBid(Parcel parcel) {
        
    }

    public void pickupParcel(Parcel parcel) {

    }
    
    
 }


