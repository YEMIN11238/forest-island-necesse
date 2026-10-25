package localforest.world;

/** A small, authored starting POI sampled in world coordinates. WATER is real
 * native liquid terrain, not a drawn backdrop. Outside the coast and its one
 * bounded connecting trail the original streaming generator remains in charge. */
public final class CoastalCampLayout {
    public enum Terrain { NATIVE, LAND, WATER }
    @FunctionalInterface public interface NativeWater { boolean water(int x,int y); }
    private static final int[][] SHORE={
        {-15,-11},{-8,-12},{-6,-16},{7,-15},{12,-11},{15,-5},
        {13,-1},{16,3},{13,7},{13,10},{8,12},{1,13},{-6,12},
        {-7,9},{-12,10},{-15,6},{-13,2},{-16,-2},{-14,-6}
    };
    private static final int[][] HUTS={{-11,-13},{4,-22},{-17,-26}};
    private static final int[][] CORE_STARTS={{-2,-2},{1,-2},{3,-4},{-7,2},{-5,4}};
    private static final int[][] ANIMAL_STARTS={{7,-12},{9,-12},{11,-12},{7,-10},{9,-10},{11,-10}};
    private static final double[][][] TRAILS={
        {{-10,2},{-6,1},{-3,0},{0,3},{3,6},{6,7},{6,5}},
        {{-8,7},{-7,5},{-4,3},{0,3}},
        {{-3,0},{-2,-5},{-4,-10},{-2,-16},{-2,-23},{-1,-29},{-2,-35}},
        {{-8,-8},{-8,-6},{-5,-5},{-2,-5}},
        {{7,-17},{7,-15},{3,-15},{-2,-16}},
        {{-14,-21},{-14,-19},{-9,-19},{-6,-21},{-2,-23}},
        {{-1,-29},{5,-29},{10,-27},{16,-25},{16,-27}}
    };
    private final ContinuousWorldLayout layout;
    public final int exitX,exitY;
    public final boolean nativeExitVerified;
    CoastalCampLayout(ContinuousWorldLayout layout){this(layout,-2,-42,false);}
    private CoastalCampLayout(ContinuousWorldLayout layout,int exitX,int exitY,boolean verified){
        this.layout=layout;this.exitX=exitX;this.exitY=exitY;nativeExitVerified=verified;
    }
    /** Choose native dry ground before generating any regions. Most seeds join
     * within 48 tiles. A narrow trail may extend to 160 on an ocean-heavy seed;
     * this never loads regions or changes the remaining native terrain. */
    public CoastalCampLayout connectToNative(NativeWater nativeWater){
        for(int distance=40;distance<=160;distance+=8){
            for(int step=0;step<=12;step++){
                int dx=step==0?-2:(step%2==1?1:-1)*((step+1)/2)*4;
                int dy=-distance;
                if(nativeDry(nativeWater,dx,dy))return new CoastalCampLayout(layout,dx,dy,true);
            }
        }
        // There is still a walkable northern exit. Native ocean exploration is
        // allowed beyond it; do not carve an unbounded causeway to find a continent.
        return new CoastalCampLayout(layout,-2,-48,false);
    }
    private boolean nativeDry(NativeWater water,int dx,int dy){
        for(int y=-2;y<=2;y++)for(int x=-2;x<=2;x++)
            if(water.water(layout.world.campX+dx+x,layout.world.campY+dy+y))return false;
        return true;
    }
    public Terrain terrain(int x,int y){
        double dx=(double)x-layout.world.campX,dy=(double)y-layout.world.campY;
        if(Math.abs(dx)>52||dy>30||dy<Math.min(-49,exitY-3))return Terrain.NATIVE;
        if(baseLand(dx,dy)||distance(dx,dy,-2,-34,exitX,exitY)<2.2)return Terrain.LAND;
        // A curved apron supplies the visible west/south sea and the two bays.
        // Its outer edge meets the existing native ocean/river classification.
        if(ellipse(dx,dy,0,2,27,24))return Terrain.WATER;
        return Terrain.NATIVE;
    }
    private boolean baseLand(double dx,double dy){
        boolean mainland=polygon(dx,dy)||ellipse(dx,dy,-2,-24,23,16);
        // Small inlets leave the south-west crop/work area connected on foot.
        if(ellipse(dx,dy,-16,0,4.0,2.8)||ellipse(dx,dy,-4,13,2.0,2.8))mainland=false;
        // Rooms/entrance are protected explicitly, never placed on fake water.
        if(dx>=3&&dx<=9&&dy>=0&&dy<=8)mainland=true;
        if(dx>=-10&&dx<=-5&&dy>=4&&dy<=8)mainland=true;
        if(dx>=13&&dx<=19&&dy>=-30&&dy<=-24)mainland=true;
        return mainland;
    }
    public boolean owned(int x,int y){return terrain(x,y)!=Terrain.NATIVE;}
    public int animalX(int index){return layout.world.campX+ANIMAL_STARTS[index][0];}
    public int animalY(int index){return layout.world.campY+ANIMAL_STARTS[index][1];}
    public boolean path(int x,int y){
        if(terrain(x,y)!=Terrain.LAND)return false;
        double dx=(double)x-layout.world.campX,dy=(double)y-layout.world.campY;
        for(double[][] trail:TRAILS)for(int i=1;i<trail.length;i++)
            if(distance(dx,dy,trail[i-1][0],trail[i-1][1],trail[i][0],trail[i][1])<1.02)return true;
        return distance(dx,dy,-2,-34,exitX,exitY)<.95;
    }
    public boolean reserved(int x,int y){
        if(!owned(x,y))return false;
        WorldDefinition d=layout.world;double dx=(double)x-d.campX,dy=(double)y-d.campY;
        // The population service owns the three houses themselves. Keep only
        // their buffered footprints clear, rather than mowing the entire camp.
        for(int[] hut:HUTS)
            if(dx>=hut[0]-1&&dx<=hut[0]+7&&dy>=hut[1]-1&&dy<=hut[1]+6)return true;
        for(int[] point:CORE_STARTS)if(Math.hypot(dx-point[0],dy-point[1])<1.2)return true;
        for(int[] point:ANIMAL_STARTS)if(Math.hypot(dx-point[0],dy-point[1])<1.2)return true;
        if(path(x,y)||layout.house(x,y)||layout.farmland(x,y))return true;
        if(Math.hypot(dx,dy-1)<2.1||Math.hypot((double)x-d.spawnX,(double)y-d.spawnY)<1.8)return true;
        if(Math.hypot((double)x-d.caveX,(double)y-d.caveY)<3.2)return true;
        for(var object:d.content.campObjects){
            if(object.role().equals("resource")||object.role().equals("crop")||object.anchor().equals("cave"))continue;
            if(Math.abs(x-object.x(d))<=1&&Math.abs(y-object.y(d))<=1)return true;
        }
        return false;
    }
    /** A fixed coordinate plan makes vegetation independent of region visit order.
     * Tree minima leave walking gaps while overlapping crowns form dense groves. */
    public String object(int x,int y){
        if(terrain(x,y)!=Terrain.LAND||reserved(x,y))return null;
        if(treeCandidate(x,y)){
            double priority=layout.random(x,y,97);boolean first=true;
            for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++)if((dx!=0||dy!=0)&&treeCandidate(x+dx,y+dy)&&layout.random(x+dx,y+dy,97)<priority)first=false;
            if(first)return layout.random(x,y,98)<.23?"birchtree":"oaktree";
        }
        double value=layout.random(x,y,99),cluster=layout.random(Math.floorDiv(x,4),Math.floorDiv(y,4),95);
        if(value<(cluster>.42?.24:.10))return layout.random(Math.floorDiv(x,3),Math.floorDiv(y,3),96)<.5?"blueberrybush":"raspberrybush";
        if(value<.36)return cluster>.48?"yellowflowerpatch":"redflowerpatch";
        if(value<.60)return "grass";
        if(value<.63)return "surfacerocksmall";
        return null;
    }
    private boolean treeCandidate(int x,int y){
        if(terrain(x,y)!=Terrain.LAND||reserved(x,y))return false;
        double cluster=layout.random(Math.floorDiv(x,5),Math.floorDiv(y,5),93);
        return layout.random(x,y,94)<(cluster>.32?.22:.075);
    }
    private static boolean ellipse(double x,double y,double cx,double cy,double rx,double ry){double dx=(x-cx)/rx,dy=(y-cy)/ry;return dx*dx+dy*dy<=1;}
    private static boolean polygon(double x,double y){
        boolean inside=false;for(int i=0,j=SHORE.length-1;i<SHORE.length;j=i++){
            int[] a=SHORE[i],b=SHORE[j];if((a[1]>y)!=(b[1]>y)&&x<(double)(b[0]-a[0])*(y-a[1])/(b[1]-a[1])+a[0])inside=!inside;
        }return inside;
    }
    private static double distance(double x,double y,double ax,double ay,double bx,double by){
        double vx=bx-ax,vy=by-ay,t=Math.max(0,Math.min(1,((x-ax)*vx+(y-ay)*vy)/(vx*vx+vy*vy)));
        return Math.hypot(x-ax-t*vx,y-ay-t*vy);
    }
}
