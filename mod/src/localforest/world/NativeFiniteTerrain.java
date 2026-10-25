package localforest.world;

import java.awt.Point;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.level.maps.generationModules.GenerationTools;

/** Pure finite planning adapter of the installed Necesse 1.3.3 island algorithms.
 * Sources: source-rebuild/necesse/level/maps/generationModules/IslandGeneration.java,
 * generateShapedIsland(), generateRandomCellIsland(), generateCellMapObjects().
 * The random cell construction below preserves those algorithms and uses the native
 * GameRandom and GenerationTools cellular automaton. No Level is created or mutated.
 * Finite border, largest-component selection, safe camp and species spacing are
 * Mosslume constraints, not the original infinite SurfaceLevel generation pipeline.
 */
public final class NativeFiniteTerrain {
    private final int width,height;
    private final boolean[] land,plants,secondary;
    private final int[] clearance;
    public final int selectedAttempt;

    public NativeFiniteTerrain(WorldDefinition world){
        width=world.width;height=world.height;
        boolean[] selected=null;int[] distances=null;int accepted=-1;
        for(int attempt=0;attempt<32;attempt++){
            GameRandom random=new GameRandom(world.seed^(0x243f6a8885a308d3L+attempt*0x9e3779b97f4a7c15L));
            boolean[] candidate=shapedIsland(random,(int)(Math.min(width,height)*.58));
            candidate=largestComponent(candidate);
            // This safety overlay is restricted to the home forest, never to a biome center.
            for(int y=3;y<height-3;y++)for(int x=3;x<width-3;x++)
                if(Point.distance(x,y,world.spawnX,world.spawnY)<=32||Point.distance(x,y,world.campX,world.campY)<=32)candidate[index(x,y)]=true;
            candidate=largestComponent(candidate);int[] depth=clearance(candidate);
            int total=0,interior=0;
            for(int y=0;y<height;y++)for(int x=0;x<width;x++)if(candidate[index(x,y)]){
                total++;if(depth[index(x,y)]>=15&&Point.distance(x,y,world.campX,world.campY)>=54)interior++;
            }
            boolean safe=true;
            for(int y=world.spawnY-32;y<=world.spawnY+32;y++)for(int x=world.spawnX-32;x<=world.spawnX+32;x++)
                if(Point.distance(x,y,world.spawnX,world.spawnY)<=32&&!candidate[index(x,y)])safe=false;
            // Require a real mainland cave approach rather than stamping a separate land disc.
            if(safe&&depth[index(world.caveX,world.caveY)]>=7&&total>=width*height*.38&&interior>=3600){
                selected=candidate;distances=depth;accepted=attempt;break;
            }
        }
        if(selected==null)throw new IllegalStateException("Native finite island could not fit expedition constraints: "+world.userSeed);
        land=selected;clearance=distances;selectedAttempt=accepted;
        GameRandom vegetation=new GameRandom(world.seed^0xa4093822299f31d0L);
        // Same cluster planner used by native IslandGeneration.generateCellMapObjects.
        boolean[] clusters=GenerationTools.generateRandomCellMap(vegetation,width,height,.35f);
        clusters=GenerationTools.doCellularAutomaton(clusters,width,height,4,3,false,4);
        plants=new boolean[width*height];secondary=new boolean[plants.length];
        for(int y=3;y<height-3;y++)for(int x=3;x<width-3;x++)
            if(clearance[index(x,y)]>=3&&vegetation.getChance(clusters[index(x,y)]?.32:.025)&&!near(plants,x,y,1))plants[index(x,y)]=true;
        // A separate sparse object plan shares spacing with trees, so rocks/ruins cannot
        // fill the walking gaps. This is not a visual grid or a road painted on the map.
        for(int y=3;y<height-3;y++)for(int x=3;x<width-3;x++)
            if(clearance[index(x,y)]>=3&&vegetation.getChance(.035)&&!near(plants,x,y,1)&&!near(secondary,x,y,1))secondary[index(x,y)]=true;
    }
    private boolean[] shapedIsland(GameRandom random,int islandSize){
        boolean[] cells=new boolean[width*height];
        // Original IslandGeneration.generateShapedIsland construction, adapted only
        // from Level tile bounds to immutable width/height and a returned cell array.
        for(int i=0;i<islandSize/5;i++){
            int thisSize=random.getIntBetween(Math.max(islandSize/10,10),(int)(islandSize/2.0f));
            float distanceX=random.getIntBetween(0,thisSize*2),distanceY=random.getIntBetween(0,thisSize*2);
            var direction=GameMath.getAngleDir(random.nextInt(360));
            int originX=(int)(width/2+direction.x*distanceX),originY=(int)(height/2+direction.y*distanceY);
            originX=GameMath.limit(originX,thisSize+20,width-thisSize-20);
            originY=GameMath.limit(originY,thisSize+20,height-thisSize-20);
            randomCellIsland(cells,random,thisSize,originX,originY);
        }
        cells=GenerationTools.doCellularAutomaton(cells,width,height,5,4,false,4);
        for(int y=0;y<height;y++)for(int x=0;x<width;x++)if(x<3||y<3||x>=width-3||y>=height-3)cells[index(x,y)]=false;
        return cells;
    }
    private void randomCellIsland(boolean[] cells,GameRandom random,int maxSize,int originX,int originY){
        Point origin=new Point(originX,originY);
        for(int x=originX-maxSize;x<originX+maxSize;x++)for(int y=originY-maxSize;y<originY+maxSize;y++){
            if(x<0||y<0||x>=width||y>=height)continue;
            double distance=origin.distance(x,y);
            if(distance<maxSize){
                if(random.nextFloat()<.8f)cells[index(x,y)]=true;
                // Bitwise '&' deliberately preserves native random consumption order.
                if(distance>maxSize-5 & random.nextFloat()<.4f)cells[index(x,y)]=false;
            }
        }
    }
    private boolean[] largestComponent(boolean[] source){
        boolean[] visited=new boolean[source.length],largest=new boolean[source.length];int[] queue=new int[source.length];int best=0;
        for(int start=0;start<source.length;start++)if(source[start]&&!visited[start]){
            int head=0,tail=0;queue[tail++]=start;visited[start]=true;
            while(head<tail){int at=queue[head++],x=at%width,y=at/width;
                if(x>0)tail=visit(source,visited,queue,tail,at-1);
                if(x+1<width)tail=visit(source,visited,queue,tail,at+1);
                if(y>0)tail=visit(source,visited,queue,tail,at-width);
                if(y+1<height)tail=visit(source,visited,queue,tail,at+width);
            }
            if(tail>best){java.util.Arrays.fill(largest,false);for(int i=0;i<tail;i++)largest[queue[i]]=true;best=tail;}
        }
        return largest;
    }
    private static int visit(boolean[] land,boolean[] seen,int[] queue,int tail,int at){if(land[at]&&!seen[at]){seen[at]=true;queue[tail++]=at;}return tail;}
    private int[] clearance(boolean[] cells){
        int[] distance=new int[cells.length],queue=new int[cells.length];java.util.Arrays.fill(distance,Integer.MAX_VALUE);int head=0,tail=0;
        for(int i=0;i<cells.length;i++)if(!cells[i]){distance[i]=0;queue[tail++]=i;}
        while(head<tail){int at=queue[head++],x=at%width,y=at/width;
            if(x>0)tail=distance(distance,queue,tail,at,at-1);
            if(x+1<width)tail=distance(distance,queue,tail,at,at+1);
            if(y>0)tail=distance(distance,queue,tail,at,at-width);
            if(y+1<height)tail=distance(distance,queue,tail,at,at+width);
        }
        return distance;
    }
    private static int distance(int[] distances,int[] queue,int tail,int from,int to){if(distances[to]==Integer.MAX_VALUE){distances[to]=distances[from]+1;queue[tail++]=to;}return tail;}
    private boolean near(boolean[] cells,int x,int y,int radius){for(int dy=-radius;dy<=radius;dy++)for(int dx=-radius;dx<=radius;dx++)if(cells[index(x+dx,y+dy)])return true;return false;}
    private int index(int x,int y){return x+y*width;}
    public boolean land(int x,int y){return x>=0&&y>=0&&x<width&&y<height&&land[index(x,y)];}
    public int clearance(int x,int y){return x>=0&&y>=0&&x<width&&y<height?clearance[index(x,y)]:0;}
    public boolean plant(int x,int y){return land(x,y)&&plants[index(x,y)];}
    public boolean secondarySolid(int x,int y){return land(x,y)&&secondary[index(x,y)];}
}
