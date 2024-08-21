package delivery_drones;

public class Bid {
    private DroneAgent contractor;
    private Parcel parcel;
    private int bidValue;

    public Bid(DroneAgent contractor, Parcel parcel, int bidValue) {
        this.contractor = contractor;
        this.parcel = parcel;
        this.bidValue = bidValue;
    }

    public DroneAgent getContractor() {
        return contractor;
    }

    public Parcel getParcel() {
        return parcel;
    }

    public int getBidValue() {
        return bidValue;
    }
}