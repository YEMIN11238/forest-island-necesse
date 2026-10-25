package localforest.visual;

import java.awt.Color;
import java.lang.reflect.Field;
import necesse.level.gameTile.*;
import necesse.level.maps.Level;
import necesse.engine.util.LevelIdentifier;
import necesse.level.maps.regionSystem.RegionManager;
import necesse.level.maps.regionSystem.managers.TileLayerManager;

/** Actual native tile flags with a sparse Level fixture; no GL or game process. */
public final class GroundDecorationPolicyTest {
    private static final sun.misc.Unsafe U;
    static{try{Field f=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);U=(sun.misc.Unsafe)f.get(null);}catch(Exception e){throw new ExceptionInInitializerError(e);}}
    private static final class TestLevel extends Level {
        GameTile tile;int object;
        private TestLevel(){super(null,0,0,null);}
        @Override public GameTile getTile(int x,int y){return tile;}
        @Override public int getObjectID(int x,int y){return object;}
        @Override public LevelIdentifier getIdentifier(){return new LevelIdentifier("ground-decoration-fixture");}
    }
    private static final class Regions extends RegionManager {
        boolean loaded=true;
        private Regions(){super(null);}
        @Override public boolean isTileLoaded(int x,int y){return loaded;}
    }
    private static final class Tiles extends TileLayerManager {
        boolean placed;
        Tiles(){super(null);}
        @Override public boolean isPlayerPlaced(int x,int y){return placed;}
    }
    public static void main(String[] args)throws Exception{
        TestLevel level=(TestLevel)U.allocateInstance(TestLevel.class);Regions regions=(Regions)U.allocateInstance(Regions.class);regions.loaded=true;
        Tiles tiles=new Tiles();set(level,"regionManager",regions);set(level,"tileLayer",tiles);
        GameTile grass=new GrassTile();grass.idData.setData(234,"grasstile");
        GameTile floor=new SimpleFloorTile("woodfloor",Color.DARK_GRAY);floor.idData.setData(235,"woodfloor");
        level.tile=grass;GroundDecorationPolicy.refresh(level);
        check(GroundDecorationPolicy.canDecorate(level,3,4),"Natural empty grass lost decoration");
        // Simulate a prepared grass draw followed by construction before it is rendered.
        level.tile=floor;
        check(floor.isFloor&&!GroundDecorationPolicy.canDecorate(level,3,4),"Native wood floor retained procedural grass");
        check(!GroundDecorationPolicy.canShowWildPlant(level,3,4),"Native plant/shadow survived floor construction");
        level.tile=grass;tiles.placed=true;
        check(!GroundDecorationPolicy.canDecorate(level,3,4),"Player landscaping received random grass");
        tiles.placed=false;level.object=9;
        check(!GroundDecorationPolicy.canDecorate(level,3,4),"Bed/structure occupancy received random grass");
        level.object=0;
        check(GroundDecorationPolicy.canDecorate(level,3,4),"Demolished floor failed to restore natural ground eligibility");
        GameTile dirt=new DirtTile();dirt.idData.setData(236,"dirttile");level.tile=dirt;
        check(NaturalPathRenderer.isPath(level,-3,4)&&NaturalPathRenderer.isPath(level,30,-40),
            "Width-zero continuous world rejected loaded signed-coordinate roads");
        check(NaturalPathShape.coverage(0,0,-96,128,1|4)<64&&NaturalPathShape.coverage(16,16,-96,128,1|4)>192,
            "Continuous path retained square corner fill rather than rounded centerline coverage");
        regions.loaded=false;
        check(!NaturalPathRenderer.isPath(level,-3,4),"Road query generated/read an unloaded region");
        check(!GroundDecorationPolicy.canDecorate(level,3,4),"Unloaded region received phantom decoration");
        System.out.println("GroundDecorationPolicyTest PASS: actual GrassTile/SimpleFloorTile, live build-after-prepare, plant/shadow floor veto, landscaping, occupancy, demolition, unloading");
    }
    private static void set(Level level,String name,Object value)throws Exception{Field f=Level.class.getDeclaredField(name);f.setAccessible(true);f.set(level,value);}
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
