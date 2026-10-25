package localforest.visual;
import java.util.Random;
import localforest.scene.TileBounds;

/** Match old un-cached shoreline samples, then edit coast/load/move state. */
public final class OceanGeometryCacheTest {
    private static float legacy(OceanGeometryCache.LandSource source,int vx,int vy){
        double nearest=2.5;
        for(int y=vy-3;y<=vy+2;y++)for(int x=vx-3;x<=vx+2;x++)if(source.land(x,y)){
            double dx=Math.max(x-vx,Math.max(0,vx-x-1)),dy=Math.max(y-vy,Math.max(0,vy-y-1));
            nearest=Math.min(nearest,Math.hypot(dx,dy));
        }
        float value=(float)Math.max(0,1-nearest/2.5);
        return value*value*(3-2*value);
    }
    private static void verify(OceanGeometryCache.Snapshot snapshot,OceanGeometryCache.LandSource source){
        TileBounds b=snapshot.vertices;
        for(int y=b.top;y<b.bottom;y++)for(int x=b.left;x<b.right;x++)
            if(Float.floatToIntBits(snapshot.at(x,y))!=Float.floatToIntBits(legacy(source,x,y)))
                throw new AssertionError("Shore depth changed at "+x+","+y);
        for(int y=b.top;y<b.bottom-1;y++)for(int x=b.left;x<b.right-1;x++){
            float[] colors=snapshot.tileColors(x,y);
            if(snapshot.tileColors(x,y)!=colors)throw new AssertionError("Unchanged water tile allocated new corner data");
            for(int i=0;i<4;i++){
                int vx=x+((i==1||i==2)?1:0),vy=y+(i>=2?1:0);
                for(int c=0;c<3;c++)if(colors[i*4+c]!=1)throw new AssertionError("Cached water data baked dynamic color");
                if(Float.floatToIntBits(colors[i*4+3])!=Float.floatToIntBits(legacy(source,vx,vy)))
                    throw new AssertionError("Water corner order/depth changed at "+x+","+y+" corner "+i);
            }
        }
        if(snapshot.containsTile(b.right-1,b.bottom-1))throw new AssertionError("Cached quad reads beyond depth bounds");
    }
    public static void main(String[] args){
        boolean[][] land=new boolean[80][80];Random random=new Random(721);
        for(int y=0;y<80;y++)for(int x=0;x<80;x++)land[y][x]=random.nextInt(15)==0;
        OceanGeometryCache.LandSource source=(x,y)->x>=0&&y>=0&&x<80&&y<80&&land[y][x];
        TileBounds b=new TileBounds(-8,-7,44,35);
        var first=OceanGeometryCache.refresh(null,b,source);verify(first,source);
        if(OceanGeometryCache.refresh(first,b,source)!=first)throw new AssertionError("Static geometry rebuilt");
        land[18][21]=!land[18][21];
        var edited=OceanGeometryCache.refresh(first,b,source);verify(edited,source);
        if(edited==first)throw new AssertionError("Terrain/region change retained old depths");
        if(edited.tileColors(0,0)==first.tileColors(0,0))throw new AssertionError("Changed shore retained stale corner array");
        b=new TileBounds(25,17,68,66);
        var moved=OceanGeometryCache.refresh(edited,b,source);verify(moved,source);
        if(moved==edited)throw new AssertionError("Camera movement retained wrong bounds");
        System.out.println("OceanGeometryCacheTest PASS: exact legacy depth/corner bits, reusable immutable colors, negative edges, edits/load state and camera movement");
    }
}
