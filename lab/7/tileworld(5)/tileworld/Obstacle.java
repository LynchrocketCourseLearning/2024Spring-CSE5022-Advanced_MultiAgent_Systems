package tileworld;

import java.util.ArrayList;
import java.util.List;

import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import repast.simphony.util.ContextUtils;

public class Obstacle {
	private int lenght;
	private int orientation;
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	private int id;
	
	public Obstacle(ContinuousSpace<Object> space, Grid<Object> grid, int length, int orientation, int id) {
		this.space = space;
		this.grid = grid;		
		this.lenght = length;
		this.orientation = orientation;
		this.id = id;	
	}
	
	@ScheduledMethod(start=1, interval=1, priority=1)
	public void updateInitialObstacle() {
		double tick = RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
		double change_control = 5.0;
		double control = tick%change_control;
		/*System.out.println("tick: "+tick);
		System.out.println("control: "+control);		
		System.out.print(grid.getLocation(this));*/
		
		if (tick == 1.0) {			
			build_obstacle(this);
	    }
		
		if ((tick > 1.0) && (control==0.0)) {
			System.out.print("Relocating");
			relocate_obstacles();
	    }
	}
    
    public void build_obstacle(Obstacle obstacle) {
    	System.out.print("+++++++++++++++++++++++++++++      INITIALIZING OBSTACLE AT 10    ++++++++++++++++++++++++++++++++");
    	Context<Object> context = ContextUtils.getContext(obstacle);
    	GridPoint pt = grid.getLocation(obstacle);
    	
        for (int i = 0; i < obstacle.lenght-1; i++) {
        	Obstacle obs = new Obstacle(this.space,this.grid, obstacle.lenght,obstacle.orientation, obstacle.id); // Replace with actual duplication logic
        	context.add(obs);
        	if(obstacle.orientation ==0) {
        		grid.moveTo(obs, (int)pt.getX()+1 + i, (int)pt.getY()); // Move each horizontally
        	} else {
        		grid.moveTo(obs, (int)pt.getX(), (int)pt.getY()+1 + i); // Move each vertically
        	}
        	
        }
        
    }
    
    public void relocate_obstacles() {		
    	List<Obstacle> obstaclesList = new ArrayList<>();
    	Context<Object> context = ContextUtils.getContext(this);
    	ArrayList<Integer> ObstaclesIDs = new ArrayList<>();
    	
    	for (Object obj : context) {
            if (obj instanceof Obstacle) {
            	Obstacle obstacle = (Obstacle) obj;
            	obstaclesList.add(obstacle);
            	
            	if (!(ObstaclesIDs.contains(obstacle.id))) {
            		ObstaclesIDs.add(obstacle.id);
            	}
            }
        }
    	//Removing Obstacles
    	if (obstaclesList.size() > 0) {
    		for (Obstacle obstacle: obstaclesList ) {
    			context.remove(obstacle);
    		}
		}
    	
    	//Recreate Obstacles
    	for (int i =0; i < ObstaclesIDs.size(); i++) {
			int length = RandomHelper.nextIntFromTo(1, 5);
			int orientation = RandomHelper.nextIntFromTo(1, 2);
			
			int x_location = RandomHelper.nextIntFromTo(1, 100);
    		int y_location = RandomHelper.nextIntFromTo(1, 50);
    		
			Obstacle newObstacle = new Obstacle(this.space, this.grid, length, orientation-1, i+1);
			context.add(newObstacle);
			this.grid.moveTo(newObstacle, (int)x_location, (int)y_location);
			build_obstacle(newObstacle);
    		
    		
    	}
    	System.out.print(ObstaclesIDs);    	
    }
	
}
