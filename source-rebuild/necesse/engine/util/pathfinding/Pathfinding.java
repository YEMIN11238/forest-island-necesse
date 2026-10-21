package necesse.engine.util.pathfinding;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;
import necesse.engine.util.GameLinkedList;
import necesse.engine.util.HashProxyLinkedList;

public abstract class Pathfinding<T> {
   public boolean useBestOfConnected = false;
   public boolean biDirectional = false;

   public <C extends Pathfinding<T>> Pathfinding<T>.Process<C> getProcess(T from, T target, int maxIterations) {
      return new Process<>((C)this, from, target, maxIterations);
   }

   public <C extends Pathfinding<T>> PathResult<T, C> findPath(T from, T target, int maxIterations) {
      Pathfinding<T>.Process<C> process = this.getProcess(from, target, maxIterations);
      return process.findPath();
   }

   protected abstract boolean isAtTarget(T var1, T var2);

   protected boolean handleNewNode(
      T node, Pathfinding<T>.Node current, HashProxyLinkedList<Pathfinding<T>.Node, T> openNodes, HashProxyLinkedList<Pathfinding<T>.Node, T> closedNodes
   ) {
      double newPathCost = current.pathCost + this.getNodePathCost(node, current.item);
      boolean foundInNodes = false;
      Pathfinding<T>.Node closedNode = closedNodes.getObject(node);
      if (closedNode != null) {
         if (closedNode.pathCost > newPathCost) {
            closedNode.setCameFrom(current, newPathCost);
         }

         foundInNodes = true;
      }

      Pathfinding<T>.Node openNode = openNodes.getObject(node);
      if (openNode != null) {
         if (openNode.pathCost > newPathCost) {
            openNode.setCameFrom(current, newPathCost);
         }

         foundInNodes = true;
      }

      return foundInNodes;
   }

   protected abstract void handleConnectedNodes(
      Pathfinding<T>.Node var1,
      HashProxyLinkedList<Pathfinding<T>.Node, T> var2,
      HashProxyLinkedList<Pathfinding<T>.Node, T> var3,
      HashSet<T> var4,
      Function<T, Boolean> var5,
      BiConsumer<Pathfinding<T>.Node, Pathfinding<T>.Node> var6,
      Runnable var7
   );

   public LinkedList<Pathfinding<T>.Node> constructPath(Pathfinding<T>.Node node) {
      LinkedList<Pathfinding<T>.Node> list = new LinkedList<>();
      list.add(node);

      while (node.cameFrom != null) {
         list.addFirst(node.cameFrom);
         node = node.cameFrom;
      }

      return list;
   }

   protected Pathfinding<T>.Node removeAndReturnNextNode(HashProxyLinkedList<Pathfinding<T>.Node, T> openNodes) {
      GameLinkedList<Pathfinding<T>.Node>.Element best = openNodes.streamElements()
         .min(Comparator.comparingDouble(e -> this.getNodeComparable((Pathfinding.Node)e.object)))
         .orElse(null);
      if (best == null) {
         return null;
      }

      best.remove();
      return (Pathfinding.Node)best.object;
   }

   protected double getNodeComparable(Pathfinding<T>.Node node) {
      return node.heuristicCost + node.pathCost;
   }

   protected abstract double getNodeHeuristicCost(T var1, T var2);

   protected abstract double getNodeCost(T var1);

   protected abstract double getNodePathCost(T var1, T var2);

   public class Node {
      public final T item;
      public final boolean reverseDirection;
      public Pathfinding<T>.Node cameFrom;
      public LinkedList<Pathfinding<T>.Node> goesTo;
      public double heuristicCost;
      public double nodeCost;
      public double pathCost;
      public int pathCount;

      public Node(T item, T target, Pathfinding<T>.Node cameFrom, boolean reverseDirection) {
         this.item = item;
         this.reverseDirection = reverseDirection;
         this.setCameFrom(cameFrom);
         this.goesTo = new LinkedList<>();
         this.heuristicCost = Pathfinding.this.getNodeHeuristicCost(item, target);
      }

      public void setCameFrom(Pathfinding<T>.Node cameFrom, Double newPathCost) {
         this.cameFrom = cameFrom;
         if (cameFrom != null) {
            this.pathCost = newPathCost != null ? newPathCost : Pathfinding.this.getNodePathCost(this.item, cameFrom.item) + cameFrom.pathCost;
            this.pathCount = cameFrom.pathCount + 1;
            this.nodeCost = Pathfinding.this.getNodeCost(this.item) + cameFrom.nodeCost;
         } else {
            this.pathCost = 0.0;
            this.pathCount = 0;
            this.nodeCost = Pathfinding.this.getNodeCost(this.item);
         }
      }

      public void setCameFrom(Pathfinding<T>.Node cameFrom) {
         this.setCameFrom(cameFrom, null);
      }
   }

   public class Process<C extends Pathfinding<T>> {
      public final C finder;
      public final Object from;
      public final Object target;
      public final int maxIterations;
      private final HashProxyLinkedList<Pathfinding<T>.Node, T> openNodes = new HashProxyLinkedList<Pathfinding<T>.Node, T>(n -> n.item) {
         @Override
         public void onAdded(GameLinkedList<Pathfinding<T>.Node>.Element element) {
            super.onAdded(element);
            if (((Pathfinding.Node)element.object).reverseDirection) {
               Process.this.openBiDirectionalNodes.add((Pathfinding.Node)element.object);
            }
         }

         @Override
         public void onRemoved(GameLinkedList<Pathfinding<T>.Node>.Element element) {
            super.onRemoved(element);
            if (((Pathfinding.Node)element.object).reverseDirection && !Process.this.openBiDirectionalNodes.remove(((Pathfinding.Node)element.object).item)) {
            }
         }
      };
      private final HashProxyLinkedList<Pathfinding<T>.Node, T> openBiDirectionalNodes = new HashProxyLinkedList<>(n -> n.item);
      private final HashProxyLinkedList<Pathfinding<T>.Node, T> closedNodes = new HashProxyLinkedList<>(n -> n.item);
      private final HashSet<T> invalidChecked = new HashSet<>();
      private int iterations;
      private long time;
      private PathResult<T, C> result;
      public final Object nodeLock = new Object();

      public Process(C finder, T from, T target, int maxIterations) {
         this.finder = finder;
         this.from = from;
         this.target = target;
         this.maxIterations = maxIterations;
         long time = System.nanoTime();
         if (from != null && target != null) {
            Pathfinding<T>.Node firstPathNode = Pathfinding.this.new Node(from, target, null, false);
            if (Pathfinding.this.isAtTarget(from, target)) {
               this.result = new PathResult<>(
                  finder,
                  from,
                  target,
                  Pathfinding.this.constructPath(firstPathNode),
                  true,
                  new ArrayList<>(this.openNodes),
                  new ArrayList<>(this.closedNodes),
                  this.invalidChecked,
                  this.iterations,
                  this.maxIterations,
                  this.time + System.nanoTime() - time
               );
            } else {
               this.openNodes.add(firstPathNode);
               if (Pathfinding.this.biDirectional) {
                  this.openNodes.add(Pathfinding.this.new Node(target, from, null, true));
               }
            }
         } else {
            this.result = new PathResult<>(
               finder,
               from,
               target,
               new LinkedList<>(),
               false,
               new ArrayList<>(this.openNodes),
               new ArrayList<>(this.closedNodes),
               this.invalidChecked,
               this.iterations,
               this.maxIterations,
               this.time + System.nanoTime() - time
            );
         }
      }

      public boolean iterate(int iterations) {
         if (this.result != null) {
            return true;
         }

         synchronized (this.nodeLock) {
            AtomicBoolean interrupted = new AtomicBoolean();
            long time = System.nanoTime();
            boolean done = false;

            for (int i = 0; i < iterations; i++) {
               if (this.openNodes.isEmpty()
                  || this.iterations >= this.maxIterations
                  || this.result != null
                  || Pathfinding.this.biDirectional && this.openBiDirectionalNodes.isEmpty()) {
                  done = true;
                  break;
               }

               this.iterations++;
               Pathfinding<T>.Node current = Pathfinding.this.removeAndReturnNextNode(this.openNodes);
               this.closedNodes.add(current);
               LinkedList<Pathfinding<T>.Node> resultNodes = new LinkedList<>();
               Pathfinding.this.handleConnectedNodes(
                  current,
                  this.openNodes,
                  this.closedNodes,
                  this.invalidChecked,
                  connected -> {
                     this.invalidChecked.remove(connected);
                     if (Pathfinding.this.handleNewNode(connected, current, this.openNodes, this.closedNodes)) {
                        return false;
                     }

                     Pathfinding<T>.Node connectedNode;
                     if (current.reverseDirection) {
                        connectedNode = Pathfinding.this.new Node(connected, (T)this.from, current, true);
                     } else {
                        connectedNode = Pathfinding.this.new Node(connected, (T)this.target, current, false);
                     }

                     current.goesTo.add(connectedNode);
                     if (Pathfinding.this.isAtTarget(connectedNode.item, (T)this.target)) {
                        resultNodes.add(connectedNode);
                        return !Pathfinding.this.useBestOfConnected;
                     } else {
                        this.openNodes.add(connectedNode);
                        return false;
                     }
                  },
                  (n1, n2) -> {
                     if (n1.reverseDirection != n2.reverseDirection) {
                        Pathfinding<T>.Node last = n1.reverseDirection ? n2 : n1;
                        Pathfinding<T>.Node next = n1.reverseDirection ? n1 : n2;

                        while (next != null) {
                           Pathfinding<T>.Node temp = next.cameFrom;
                           next.cameFrom = last;
                           last = next;
                           next = temp;
                        }

                        this.result = new PathResult<>(
                           this.finder,
                           (T)this.from,
                           (T)this.target,
                           Pathfinding.this.constructPath(last),
                           true,
                           new ArrayList<>(this.openNodes),
                           new ArrayList<>(this.closedNodes),
                           this.invalidChecked,
                           this.iterations,
                           this.maxIterations,
                           this.time + System.nanoTime() - time
                        );
                     }
                  },
                  () -> interrupted.set(true)
               );
               if (!resultNodes.isEmpty()) {
                  Pathfinding<T>.Node best = resultNodes.stream().min(Comparator.comparingDouble(Pathfinding.this::getNodeComparable)).orElse(null);
                  this.result = new PathResult<>(
                     this.finder,
                     (T)this.from,
                     (T)this.target,
                     Pathfinding.this.constructPath(best),
                     true,
                     new ArrayList<>(this.openNodes),
                     new ArrayList<>(this.closedNodes),
                     this.invalidChecked,
                     this.iterations,
                     this.maxIterations,
                     this.time + System.nanoTime() - time
                  );
                  break;
               }

               if (interrupted.get()) {
                  break;
               }
            }

            if (this.result == null && (done || iterations >= this.maxIterations || interrupted.get())) {
               Pathfinding<T>.Node best = Stream.concat(this.openNodes.stream(), this.closedNodes.stream())
                  .min(Comparator.comparingDouble(n -> n.heuristicCost))
                  .orElse(null);
               if (best == null) {
                  this.result = new PathResult<>(
                     this.finder,
                     (T)this.from,
                     (T)this.target,
                     new LinkedList<>(),
                     false,
                     new ArrayList<>(this.openNodes),
                     new ArrayList<>(this.closedNodes),
                     this.invalidChecked,
                     this.iterations,
                     this.maxIterations,
                     this.time + System.nanoTime() - time
                  );
               } else {
                  this.result = new PathResult<>(
                     this.finder,
                     (T)this.from,
                     (T)this.target,
                     Pathfinding.this.constructPath(best),
                     false,
                     new ArrayList<>(this.openNodes),
                     new ArrayList<>(this.closedNodes),
                     this.invalidChecked,
                     this.iterations,
                     this.maxIterations,
                     this.time + System.nanoTime() - time
                  );
               }
            }

            this.time = this.time + (System.nanoTime() - time);
            return this.result != null;
         }
      }

      public PathResult<T, C> findPath() {
         this.iterate(this.maxIterations);
         return this.result;
      }

      public boolean isDone() {
         return this.result != null;
      }

      public PathResult<T, C> getResult() {
         return this.result;
      }

      public Iterable<Pathfinding<T>.Node> getOpenNodes() {
         return this.openNodes;
      }

      public Iterable<Pathfinding<T>.Node> getClosedNodes() {
         return this.closedNodes;
      }

      public Iterable<T> getInvalidChecked() {
         return this.invalidChecked;
      }

      public Stream<Pathfinding<T>.Node> streamOpenNodes() {
         return this.openNodes.stream();
      }

      public Stream<Pathfinding<T>.Node> streamClosedNodes() {
         return this.closedNodes.stream();
      }

      public Stream<T> streamInvalidChecked() {
         return this.invalidChecked.stream();
      }

      public int getIterations() {
         return this.iterations;
      }

      public long getTime() {
         return this.time;
      }
   }
}
