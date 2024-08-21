package delivery_drones;

import repast.simphony.space.continuous.NdPoint;

public class Parcel {
    private String name;
    private NdPoint pickupLocation;
    private NdPoint deliveryLocation;

    public Parcel(String name, NdPoint pickupLocation, NdPoint deliveryLocation) {
        this.name = name;
        this.pickupLocation = pickupLocation;
        this.deliveryLocation = deliveryLocation;
    }

    public String getName() {
        return name;
    }

    public NdPoint getPickupLocation() {
        return pickupLocation;
    }

    public NdPoint getDeliveryLocation() {
        return deliveryLocation;
    }
}