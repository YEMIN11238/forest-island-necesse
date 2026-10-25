package localforest.companions;

import java.awt.Point;
import java.util.HashSet;
import java.util.Set;

public final class PopulationSpawnSearchTest {
    private static void check(boolean condition,String why){if(!condition)throw new AssertionError(why);}
    public static void main(String[] args){
        Set<Point> loadedDry=new HashSet<>();
        for(int y=-3;y<=3;y++)for(int x=-3;x<=3;x++)loadedDry.add(new Point(x,y));
        loadedDry.add(new Point(6,0)); // Visually nearest, but separated from camp by water/unloaded ground.
        Point result=PopulationSpawnSearch.connectedNear(new Point(0,0),new Point(6,0),12,
            (x,y)->loadedDry.contains(new Point(x,y)),(x,y)->true);
        check(result.equals(new Point(3,0)),"Spawn selected disconnected dry land");
        result=PopulationSpawnSearch.connectedNear(new Point(0,0),new Point(3,0),12,
            (x,y)->loadedDry.contains(new Point(x,y)),(x,y)->!(x==3&&y==0));
        check(!result.equals(new Point(3,0))&&loadedDry.contains(result),"Spawn overlaps an occupied/colliding body");
        check(PopulationSpawnSearch.connectedNear(new Point(0,0),new Point(2,2),12,(x,y)->false,(x,y)->true)==null,"Failed spawn fabricated land");
        loadedDry.add(new Point(0,4));loadedDry.add(new Point(0,5));
        var expected=PopulationSpawnSearch.connectedNear(new Point(0,0),new Point(0,5),12,(x,y)->loadedDry.contains(new Point(x,y)),(x,y)->true);
        check(expected.equals(new Point(0,5)),"Retried loaded connection did not unlock the true desired spawn");
        for(int i=0;i<20;i++)check(expected.equals(PopulationSpawnSearch.connectedNear(new Point(0,0),new Point(0,5),12,
            (x,y)->loadedDry.contains(new Point(x,y)),(x,y)->true)),"Retry selection is nondeterministic");
        check(PopulationSpawnSearch.connectedNear(new Point(-20,-20),new Point(-19,-20),3,
            (x,y)->y==-20&&x>=-20&&x<=-18,(x,y)->true).equals(new Point(-19,-20)),"Negative world coordinates rejected");
        System.out.println("PopulationSpawnSearchTest PASS connectedDryLand=true collisionRejected=true retryAfterLoad=true stableChoice=true boundedLoadedSearch=true negativeCoordinates=true");
    }
}
