package delivery_drones;

import repast.simphony.space.continuous.NdPoint;

public class PickupPoint {
    private NdPoint location;
    private Parcel parcel;

    public PickupPoint(NdPoint location, Parcel parcel) {
        this.location = location;
        this.parcel = parcel;
    }

    public NdPoint getLocation() {
        return location;
    }

    public Parcel getParcel() {
        return parcel;
    }
}