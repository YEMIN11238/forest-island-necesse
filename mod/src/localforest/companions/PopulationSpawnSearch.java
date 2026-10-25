package localforest.companions;

import java.awt.Point;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.function.BiPredicate;

/** Bounded loaded-land search. A nearby open tile alone can be an isolated island or a sealed room. */
final class PopulationSpawnSearch {
    private static final int[][] DIRECTIONS={{0,1},{1,0},{0,-1},{-1,0}};
    private PopulationSpawnSearch() {}
    static Point connectedNear(Point origin,Point wanted,int radius,BiPredicate<Integer,Integer> traversable,
        BiPredicate<Integer,Integer> standable) {
        Point start=null;
        for(int r=0;r<=3&&start==null;r++)for(int dy=-r;dy<=r&&start==null;dy++)for(int dx=-r;dx<=r;dx++){
            if(r>0&&Math.abs(dx)!=r&&Math.abs(dy)!=r)continue;
            if(traversable.test(origin.x+dx,origin.y+dy)){start=new Point(origin.x+dx,origin.y+dy);break;}
        }
        if(start==null)return null;
        var queue=new ArrayDeque<Point>();var seen=new HashSet<Point>();queue.add(start);seen.add(start);
        Point best=null;double score=Double.POSITIVE_INFINITY;
        while(!queue.isEmpty()) {
            Point p=queue.removeFirst();double distance=p.distanceSq(wanted);
            if(distance<score&&standable.test(p.x,p.y)){best=p;score=distance;}
            for(int[] direction:DIRECTIONS){
                Point next=new Point(p.x+direction[0],p.y+direction[1]);
                if(Math.abs((long)next.x-origin.x)>radius||Math.abs((long)next.y-origin.y)>radius||!seen.add(next))continue;
                if(traversable.test(next.x,next.y))queue.addLast(next);
            }
        }
        return best;
    }
}
