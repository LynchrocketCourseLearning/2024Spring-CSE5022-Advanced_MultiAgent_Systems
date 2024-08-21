package marsExplorers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

import repast.simphony.query.space.grid.GridCell;
import repast.simphony.space.SpatialMath;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.grid.GridPoint;

public class MoveManager {
	private Explorer explorer;
	/**
	 * the ratio in sensing the environment
	 */
	private int senseRatio;
	/**
	 * the ratio of moving one time
	 */
	private int moveRatio;
	/**
	 * the target point of the agent
	 */
	private GridPoint targetPoint;
	private GridPoint nextTargetPoint;
	/**
	 * the passed point
	 */
	private GridPoint lastPt;
	private FixedSizeQueue passedPts;

	private boolean hasReached;

	public MoveManager(Explorer explorer, int senseRatio, int moveRatio) {
		this.explorer = explorer;
		this.senseRatio = senseRatio;
		this.moveRatio = moveRatio;
		this.targetPoint = null;
		this.nextTargetPoint = null;
		this.lastPt = explorer.getGridLocation();
		this.passedPts = new FixedSizeQueue(3, 2);
		this.hasReached = false;
	}

	public void setTarget(GridPoint targetPt) {
		this.targetPoint = targetPt;
	}

	public void setNextTarget(GridPoint nextTargetPt) {
		this.nextTargetPoint = nextTargetPt;
	}

	public boolean hasTarget() {
		return targetPoint != null;
	}

	public boolean hasNextTarget() {
		return nextTargetPoint != null;
	}

	public void moveTowardsTarget() {
		if (moveTowards(targetPoint)) {
			System.out.println("Reached! Clear target!");
			targetPoint = null;
			hasReached = true;
		} else {
			hasReached = false;
		}
	}

	public boolean hasReached() {
		return hasReached;
	}

	public void setReached(boolean hasReached) {
		this.hasReached = hasReached;
	}

	public GridPoint getTargetPoint() {
		return targetPoint;
	}

	public void switchNextTarget() {
		this.targetPoint = this.nextTargetPoint;
		this.nextTargetPoint = null;
	}

	/**
	 * move towards the point
	 * one distance a time
	 * return true if it has reached the point; otherwise, false.
	 */
	public boolean moveTowards(GridPoint targetPt) {
		// if no target, then stay still
		// return true to make it re-choose the target at next tick
		if (targetPt == null) {
			return true;
		}

		GridPoint thisPt = explorer.getGridLocation();
		// if no energy, it cannot move
		if (!explorer.isSurvival()) {
			return thisPt.equals(targetPt);
		}

		// not reached the target
		if (!thisPt.equals(targetPt)) {
			// // find the legal points around
			// List<GridCell<Object>> nghCells = explorer.getThisGridNeighbour(Object.class,
			// false, senseRatio,
			// senseRatio);
			// List<GridPoint> nghLegalPts = nghCells.stream()
			// .map(cell -> cell.getPoint())
			// .filter(point -> !point.equals(thisPt) && !point.equals(lastPt) &&
			// !passedPts.contains(point))
			// .toList();

			// boolean hasSensedTarget = nghCells.stream()
			// .map(cell -> cell.getPoint())
			// .filter(pt -> targetPt.equals(pt))
			// .toList().size() > 0;
			// // if target under the senseRatio, then the agent can directly move to it;
			// // otherwise, move to the around point that nearest to target
			// GridPoint nextPoint = hasSensedTarget ? shortestPath(thisPt, targetPt).get(1)
			// : Collections.min(nghLegalPts,
			// (p1, p2) -> (int) (explorer.getDistance(p1, targetPt)
			// - explorer.getDistance(p2, targetPt)));

			// // record passed point
			// lastPt = thisPt;
			// passedPts.offer(thisPt);

			// System.out.println("Next move to " + nextPoint);
			// moveTo(nextPoint);

			NdPoint nextPoint = new NdPoint(targetPt.getX(), targetPt.getY());
			double angle = SpatialMath.calcAngleFor2DMovement(explorer.getSpace(),
					explorer.getSpace().getLocation(explorer), nextPoint);
			explorer.getSpace().moveByVector(explorer, 1, angle, 0);
			NdPoint newPt = explorer.getSpace().getLocation(explorer);
			explorer.getGrid().moveTo(explorer, (int) newPt.getX(), (int) newPt.getY());

			return explorer.getGridLocation().equals(targetPt);
		}
		return true;
	}

	/**
	 * A* algorithm
	 * maybe using too much information
	 */
	public List<GridPoint> shortestPath(GridPoint startPt, GridPoint targePt) {
		PriorityQueue<PointWithCost> openList = new PriorityQueue<>();
		PriorityQueue<PointWithCost> closeList = new PriorityQueue<>();
		openList.add(new PointWithCost(startPt, 0));
		PointWithCost pt = null;
		while (!openList.isEmpty()) {
			pt = openList.poll();
			GridPoint ptGrid = pt.pt;
			if (ptGrid.equals(targePt)) {
				return calculatePath(pt);
			}

			List<GridCell<Object>> nghCells = explorer.getPtGridNeighbour(ptGrid, Object.class, false, moveRatio,
					moveRatio);
			List<GridPoint> nghLegalPts = nghCells.stream()
					.map(cell -> cell.getPoint())
					.filter(point -> !ptGrid.equals(point))
					.toList();
			for (GridPoint nextPtGrid : nghLegalPts) {
				PointWithCost nextPt = new PointWithCost(nextPtGrid, 0);
				if (!closeList.contains(nextPt)) {
					nextPt.cost = pt.cost + 1 + explorer.getDistance(ptGrid, nextPtGrid);
					nextPt.parentPt = pt;

					if (!openList.contains(nextPt)) {
						openList.add(nextPt);
					}
				}
			}
			closeList.add(pt);
		}

		return calculatePath(pt);
	}

	private List<GridPoint> calculatePath(PointWithCost pt) {
		Deque<GridPoint> path = new LinkedList<>();
		PointWithCost cur = pt;
		while (cur != null) {
			path.addFirst(cur.pt);
			cur = cur.parentPt;
		}
		return new ArrayList<>(path);
	}

	private void moveTo(GridPoint pt) {
		explorer.moveTo(pt);
	}

	public class PointWithCost implements Comparable<PointWithCost> {
		public GridPoint pt;
		public double cost;

		public PointWithCost parentPt;

		public PointWithCost(GridPoint pt, double cost) {
			this.pt = pt;
			this.cost = cost;
		}

		@Override
		public int compareTo(PointWithCost o) {
			return (this.cost >= o.cost) ? 1 : -1;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof PointWithCost o) {
				return pt.equals(o.pt);
			}
			return false;
		}
	}

	public class FixedSizeQueue {
		private int size;
		private int threshold;
		private List<PointWithCost> queue;

		public FixedSizeQueue(int size, int threshold) {
			this.size = size;
			this.threshold = threshold;
			this.queue = new ArrayList<>();
		}

		public void offer(GridPoint pt) {
			PointWithCost pwc = new PointWithCost(pt, 1);
			int idx = this.queue.indexOf(pwc);
			if (idx == -1) {
				this.queue.add(pwc);
			} else {
				this.queue.get(idx).cost++;
			}
			if (this.queue.size() > size) {
				this.queue.remove(0);
			}
		}

		public PointWithCost poll() {
			return this.queue.remove(0);
		}

		public boolean contains(GridPoint pt) {
			for (PointWithCost pwc : queue) {
				if (pwc.cost >= threshold && pwc.pt.equals(pt)) {
					return true;
				}
			}
			return false;
		}
	}
}
