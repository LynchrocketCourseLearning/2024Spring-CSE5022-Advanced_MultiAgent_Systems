package marsExplorers;

import java.util.ArrayList;
import java.util.List;

import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;

public class Trader {
    private ContinuousSpace<Object> space;
    private Grid<Object> grid;
    private int id;

    private boolean active;
    private int lifetime;

    private double survivalResources;
    private List<Material> tradedMaterial;

    private double lastBid;

    public Trader(ContinuousSpace<Object> space, Grid<Object> grid, int id) {
        this.space = space;
        this.grid = grid;
        this.id = id;

        this.active = true;
        this.lifetime = 100;

        this.survivalResources = RandomHelper.nextDoubleFromTo(100.0, 200.0);
        this.tradedMaterial = new ArrayList<>();
    }

    /**
     * return the survival resources of the material
     * if trading with trader, the utility is the same as the survival resources
     */
    public double trade(Material material, double price) {
        if (price > survivalResources) {
            return -1;
        }
        lastBid = RandomHelper.nextDoubleFromTo(price, survivalResources);
        return lastBid;
    }

    public List<Material> acceptTrade(double resources) {
        List<Material> materials = new ArrayList<>();
        double totalPrice = 0.0;
        while (!tradedMaterial.isEmpty()) {
            Material material = tradedMaterial.remove(0);
            double price = Math.log(1 + material.getWeight());
            if (totalPrice + price <= resources) {
                totalPrice += price;
                materials.add(material);
            } else {
                tradedMaterial.add(0, material);
                break;
            }
        }
        return materials;
    }

    public double acceptTrade(Material material) {
        double randomBonus = RandomHelper.nextDoubleFromTo(0.0, 5.0);
        this.survivalResources -= (lastBid + randomBonus);
        this.tradedMaterial.add(material);
        tradedMaterial.sort((m1, m2) -> (int) (m1.getWeight() - m2.getWeight()));
        return lastBid + randomBonus;
    }

    public boolean isActive() {
        return active;
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

    @ScheduledMethod(start = 0, interval = 1)
    public void step() {
        if (active) {
            if (lifetime > 0) {
                if (lifetime % 5 == 0) {
                    moveRandomly();
                }
                System.out.println("Trader " + id + " is active at " + grid.getLocation(this));
            } else {
                // Trader 生存时间耗尽时，标记为不活跃
                active = false;
                lifetime = 100;
                System.out.println("Trader " + id + " is inactive.");
            }
            lifetime--;
        } else {
            if (lifetime <= 0) {
                active = true;
                lifetime = 100;
                System.out.println("Trader " + id + " is active again.");
            }
            lifetime--;
        }
    }

    private void moveRandomly() {
        int newX = (int) (space.getLocation(this).getX() + RandomHelper.nextIntFromTo(1, 3));
        int newY = (int) (space.getLocation(this).getY() + RandomHelper.nextIntFromTo(1, 3));
        space.moveTo(this, newX, newY);
        grid.moveTo(this, newX, newY);
    }
}
