package tileWorld;

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
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;

	private int id;
	private int length;
	private int orientation;
	private int changeInterval;
	
	public Obstacle(ContinuousSpace<Object> space, Grid<Object> grid, int changeInterval, int id, int length, int orientation) {
		this.space = space;
		this.grid = grid;
		this.changeInterval = changeInterval;
		this.id = id;
		this.length = length;
		this.orientation = orientation;
	}
	
	@ScheduledMethod(start=1, interval=1, priority=1)
	public void updateObstacle() {
		double tick = RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
		double control = tick % this.changeInterval;
		
		if (tick == 1.0) {			
			buildObstacle(this);
	    }
		
		if (tick > 1.0 && control == 0.0) {
			System.out.print("Relocating");
			relocateObstacles();
	    }
	}
	
	public void buildObstacle(Obstacle obstacle) {
	   System.out.print("+++++++++++++++++++++++++++++      BUILDING OBSTACLE   ++++++++++++++++++++++++++++++++");
	   Context<Object> context = ContextUtils.getContext(obstacle);
	   GridPoint pt = grid.getLocation(obstacle);
	    	
	   for (int i = 0; i < obstacle.length-1; i++) { 
		   // Replace with actual duplication logic
	       Obstacle obs = new Obstacle(this.space, this.grid, this.changeInterval, obstacle.id, obstacle.length, obstacle.orientation);
	       context.add(obs);
	       if(obstacle.orientation == 0) {
	    	   // Move each horizontally
	           grid.moveTo(obs, (int)pt.getX()+1+i, (int)pt.getY()); 
	       } else {
	    	   // Move each vertically
	           grid.moveTo(obs, (int)pt.getX(), (int)pt.getY()+1 + i); 
	       }
	        	
	   }   
	}
	    
	public void relocateObstacles() {
	    Context<Object> context = ContextUtils.getContext(this);
	    List<Obstacle> obstaclesList = new ArrayList<>();
	    List<Integer> ObstaclesIDs = new ArrayList<>();
	    	
	    for (Object obj : context) {
	        if (obj instanceof Obstacle obstacle) {
	            obstaclesList.add(obstacle);
	            	
	            if (!ObstaclesIDs.contains(obstacle.id)) {
	            	ObstaclesIDs.add(obstacle.id);
	            }
	        }
	    }
	    
	    // Removing Obstacles
	    if (obstaclesList.size() > 0) {
	    	for (Obstacle obstacle: obstaclesList) {
	    		context.remove(obstacle);
	    	}
		}
	    	
	    // Recreate Obstacles
	    for (int i = 0; i < ObstaclesIDs.size(); i++) {
			int length = RandomHelper.nextIntFromTo(1, 5);
			int orientation = RandomHelper.nextIntFromTo(1, 2);
				
			int xLocation = RandomHelper.nextIntFromTo(1, 100);
	    	int yLocation = RandomHelper.nextIntFromTo(1, 50);
	    		
			Obstacle newObstacle = new Obstacle(this.space, this.grid, this.changeInterval, i+1, length, orientation-1);
			context.add(newObstacle);
			this.grid.moveTo(newObstacle, (int)xLocation, (int)yLocation);
			buildObstacle(newObstacle);
	    }
	    System.out.print(ObstaclesIDs);    	
	}
}
