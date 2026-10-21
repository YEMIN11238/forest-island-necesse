package necesse.engine.util.pathfinding;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;

public class PathResult<T, C extends Pathfinding<T>> {
   public final C finder;
   public final T start;
   public final T target;
   public final LinkedList<Pathfinding<T>.Node> path;
   public final boolean foundTarget;
   public final ArrayList<Pathfinding<T>.Node> openNodes;
   public final ArrayList<Pathfinding<T>.Node> closedNodes;
   public final HashSet<T> invalidChecked;
   public final int iterations;
   public final int maxIterations;
   public final long nsTaken;

   public PathResult(
      C finder,
      T start,
      T target,
      LinkedList<Pathfinding<T>.Node> path,
      boolean foundTarget,
      ArrayList<Pathfinding<T>.Node> openNodes,
      ArrayList<Pathfinding<T>.Node> closedNodes,
      HashSet<T> invalidChecked,
      int iterations,
      int maxIterations,
      long nsTaken
   ) {
      this.finder = finder;
      this.start = start;
      this.target = target;
      this.path = path;
      this.foundTarget = foundTarget;
      this.openNodes = openNodes;
      this.closedNodes = closedNodes;
      this.invalidChecked = invalidChecked;
      this.iterations = iterations;
      this.maxIterations = maxIterations;
      this.nsTaken = nsTaken;
   }

   public Pathfinding<T>.Node getLastPathNode() {
      return this.path.isEmpty() ? null : this.path.get(this.path.size() - 1);
   }

   public T getLastPathResult() {
      return this.path.isEmpty() ? null : this.path.get(this.path.size() - 1).item;
   }
}
