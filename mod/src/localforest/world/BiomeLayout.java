package localforest.world;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import localforest.world.noise.FastNoiseLite;

/** Finite expedition terrain. Version 2 remains frozen; version 3 uses scattered biome patches. */
public final class BiomeLayout {
    public static final String[] IDS={"wetland","mushroom","ruins","snowfield","desert","volcano"};
    private static final int[][] OFFSETS={{-40,-23},{-40,23},{0,40},{0,-40},{40,-23},{40,23}};
    private final WorldDefinition world;
    private final boolean[][] paths;
    private final List<Structure> structures=new ArrayList<>();
    private final List<Patch> patches=new ArrayList<>();
    private FastNoiseLite warpX,warpY,edges;
    private NativeFiniteTerrain finiteTerrain;
    private byte[][] biomeCells;
    private boolean[][] reservedCells;
    private record Patch(String id,int x,int y,double rx,double ry,double angle) {}
    public record Structure(String biome,int variant,int x,int y) {
        public String id(){return "mlbiome_"+biome+"_structure"+variant;}
        public boolean contains(int tx,int ty,int margin){return tx>=x-1-margin&&tx<=x+1+margin&&ty>=y-1-margin&&ty<=y+margin;}
    }
    public BiomeLayout(WorldDefinition world){
        if(world.isContinuous())throw new IllegalArgumentException("Continuous worlds must not allocate a finite biome layout");
        this.world=world;paths=new boolean[world.width][world.height];
        if(world.generatorVersion>=3){buildExpedition();return;}
        // Frozen v2 generator: old encoded saves retain their original region identities and roads.
        Point camp=new Point(world.campX,world.campY);
        for(String id:IDS)curve(camp,center(id),2.5);
        String[] ring={"snowfield","desert","volcano","ruins","mushroom","wetland","snowfield"};
        for(int i=1;i<ring.length;i++)curve(center(ring[i-1]),center(ring[i]),3.5);
        curve(camp,new Point(world.caveX,world.caveY),2);
        for(String id:IDS){
            Point c=center(id);
            for(int variant=0;variant<3;variant++){
                int[] desired=variant==0?new int[]{-6,-5}:variant==1?new int[]{6,-5}:new int[]{-4,7};
                boolean added=false;
                for(int radius=0;radius<10&&!added;radius++)for(int dx=-radius;dx<=radius&&!added;dx++)for(int dy=-radius;dy<=radius&&!added;dy++){
                    if(radius>0&&Math.max(Math.abs(dx),Math.abs(dy))!=radius)continue;
                    int x=c.x+desired[0]+dx,y=c.y+desired[1]+dy;
                    if(validStructure(id,x,y)){structures.add(new Structure(id,variant,x,y));added=true;}
                }
                if(!added)throw new IllegalStateException("No safe structure location: "+id+"/"+variant);
            }
        }
    }
    private static FastNoiseLite noise(long seed,float frequency){
        FastNoiseLite n=new FastNoiseLite((int)(seed^(seed>>>32)));
        n.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2);n.SetFrequency(frequency);
        n.SetFractalType(FastNoiseLite.FractalType.FBm);n.SetFractalOctaves(3);return n;
    }
    private void buildExpedition(){
        // Reuse the native finite island/vegetation algorithms before choosing habitats.
        // Six habitat patches and the safe camp remain Mosslume content rules.
        finiteTerrain=new NativeFiniteTerrain(world);
        warpX=noise(world.seed^0x5ad38b19L,.030f);warpY=noise(world.seed^0x774a2e53L,.027f);
        edges=noise(world.seed^0x1fc6a029L,.065f);
        // Rejection sampling gives different positions, sizes and rotations per seed. The forest
        // is real background terrain between finite patches, not an extra Voronoi wedge.
        boolean placed=false;
        for(int attempt=0;attempt<48&&!placed;attempt++){
            patches.clear();Random random=new Random(world.seed^(0x6a09e667f3bcc909L+attempt*0x9e3779b97f4a7c15L));
            for(String id:IDS){
                Patch chosen=null;
                for(int sample=0;sample<1800;sample++){
                    int x=28+random.nextInt(world.width-56),y=26+random.nextInt(world.height-52);
                    double nx=(x-world.width*.5)/(world.width*.5),ny=(y-world.height*.5)/(world.height*.5);
                    if(nx*nx+ny*ny>.78*.78||Math.hypot(x-world.campX,y-world.campY)<54||finiteTerrain.clearance(x,y)<15)continue;
                    double rx=22+random.nextDouble()*9,ry=19+random.nextDouble()*8,angle=random.nextDouble()*Math.PI;
                    boolean separated=true;
                    for(Patch other:patches)if(Math.hypot(x-other.x,y-other.y)<38){separated=false;break;}
                    if(separated){chosen=new Patch(id,x,y,rx,ry,angle);break;}
                }
                if(chosen==null)break;patches.add(chosen);
            }
            placed=patches.size()==IDS.length;
        }
        if(!placed)throw new IllegalStateException("Could not place six dispersed biome patches for "+world.userSeed);
        biomeCells=new byte[world.width][world.height];
        for(int x=0;x<world.width;x++)for(int y=0;y<world.height;y++)biomeCells[x][y]=sampleExpeditionBiome(x,y);
        // Only the lived-in homestead has a path. One winding exit disappears into the forest;
        // no road is drawn from camp to biome centers, the cave, or the map boundary.
        localCurve(world.houseX,world.houseY+1,world.campX+1,world.campY+7,world.campX-4,world.campY+4,1.35);
        localCurve(world.campX-4,world.campY+4,world.campX-13,world.campY+3,world.campX-22,world.campY-7,1.30);
        localCurve(world.campX-4,world.campY+4,world.campX-7,world.campY,world.campX-4,world.campY-2,1.15);
        Random buildings=new Random(world.seed^0x3c6ef372fe94f82bL);
        for(String id:IDS){
            Point c=center(id);
            for(int variant=0;variant<3;variant++){
                boolean added=false;
                for(int candidate=0;candidate<800&&!added;candidate++){
                    double angle=buildings.nextDouble()*Math.PI*2,radius=9+buildings.nextDouble()*13;
                    int x=c.x+(int)Math.round(Math.cos(angle)*radius),y=c.y+(int)Math.round(Math.sin(angle)*radius);
                    if(validExpeditionStructure(id,variant,x,y)){structures.add(new Structure(id,variant,x,y));added=true;}
                }
                // Deterministic fallback still searches the region, never substitutes a water tile.
                for(int radius=7;radius<28&&!added;radius++)for(int dx=-radius;dx<=radius&&!added;dx++)for(int dy=-radius;dy<=radius&&!added;dy++){
                    if(Math.max(Math.abs(dx),Math.abs(dy))!=radius)continue;
                    if(validExpeditionStructure(id,variant,c.x+dx,c.y+dy)){structures.add(new Structure(id,variant,c.x+dx,c.y+dy));added=true;}
                }
                if(!added)throw new IllegalStateException("No safe v3 outpost location: "+id+"/"+variant);
            }
        }
        reservedCells=new boolean[world.width][world.height];
        for(Structure s:structures){
            for(int x=s.x-2;x<=s.x+2;x++)for(int y=s.y-2;y<=s.y+1;y++)reservedCells[x][y]=true;
            // Native utilities are at (x +/- 3, y + 3). Keep their front yard, both
            // interaction approaches and the connection to the doorway free of flora.
            if(s.variant==0)for(int x=s.x-4;x<=s.x+4;x++)for(int y=s.y+1;y<=s.y+4;y++)reservedCells[x][y]=true;
        }
        for(Patch p:patches)for(int x=p.x-3;x<=p.x+3;x++)for(int y=p.y-3;y<=p.y+3;y++)reservedCells[x][y]=true;
    }
    private byte sampleExpeditionBiome(int x,int y){
        if(Math.hypot(x-world.campX,y-world.campY)<38||Math.hypot(x-world.spawnX,y-world.spawnY)<36)return 0;
        // A compact core protects native animal/monster seed positions and each expedition bench.
        for(int i=0;i<patches.size();i++){Patch p=patches.get(i);if(Math.hypot(x-p.x,y-p.y)<10)return (byte)(i+1);}
        double wx=x+warpX.GetNoise(x,y)*5.5,wy=y+warpY.GetNoise(x,y)*5.5;
        int selected=-1;double score=1;
        for(int i=0;i<patches.size();i++){
            Patch p=patches.get(i);double dx=wx-p.x,dy=wy-p.y,c=Math.cos(p.angle),s=Math.sin(p.angle);
            double px=(dx*c+dy*s)/p.rx,py=(-dx*s+dy*c)/p.ry;
            double candidate=Math.sqrt(px*px+py*py)+edges.GetNoise(x+i*131,y-i*97)*.17;
            if(candidate<score){score=candidate;selected=i;}
        }
        return (byte)(selected+1);
    }
    /** Version 3's immutable native island plan is sampled; no Level/storage is modified. */
    public boolean land(int x,int y){
        return finiteTerrain.land(x,y);
    }
    private void localCurve(double ax,double ay,double bx,double by,double cx,double cy,double width){
        for(int step=0;step<=160;step++){
            double t=step/160.0,u=1-t,x=u*u*ax+2*u*t*bx+t*t*cx,y=u*u*ay+2*u*t*by+t*t*cy;
            for(int ix=(int)Math.floor(x-width);ix<=Math.ceil(x+width);ix++)for(int iy=(int)Math.floor(y-width);iy<=Math.ceil(y+width);iy++)
                if(ix>=3&&iy>=3&&ix<world.width-3&&iy<world.height-3&&Math.hypot(ix-x,iy-y)<width)paths[ix][iy]=true;
        }
    }
    public Point center(String id){
        if(world.generatorVersion>=3){for(Patch p:patches)if(p.id.equals(id))return new Point(p.x,p.y);}
        else for(int i=0;i<IDS.length;i++)if(IDS[i].equals(id))return new Point(world.campX+OFFSETS[i][0],world.campY+OFFSETS[i][1]);
        throw new IllegalArgumentException(id);
    }
    public List<Structure> structures(){return List.copyOf(structures);}
    public String biome(int x,int y){
        if(world.generatorVersion>=3){if(x<0||y<0||x>=world.width||y>=world.height)return "forest";int index=biomeCells[x][y];return index==0?"forest":IDS[index-1];}
        if(Math.hypot(x-world.campX,y-world.campY)<17)return "forest";
        String selected=IDS[0];double distance=Double.MAX_VALUE;
        // Frozen v2 broad boundaries, independent of chunk load order or frame time.
        double nx=x+Math.sin(y*.13+world.seed%19)*2.0,ny=y+Math.sin(x*.11+world.seed%23)*2.0;
        for(int i=0;i<IDS.length;i++){double dx=nx-world.campX-OFFSETS[i][0],dy=ny-world.campY-OFFSETS[i][1];double d=dx*dx+dy*dy;if(d<distance){distance=d;selected=IDS[i];}}
        return selected;
    }
    private void curve(Point from,Point to,double bend){
        double dx=to.x-from.x,dy=to.y-from.y,len=Math.hypot(dx,dy);
        for(double t=0;t<=1.001;t+=1/100.0){
            double wave=Math.sin(t*Math.PI*2)*bend;
            double x=from.x+dx*t-dy/len*wave,y=from.y+dy*t+dx/len*wave;
            for(int ix=(int)Math.floor(x-1.8);ix<=Math.ceil(x+1.8);ix++)for(int iy=(int)Math.floor(y-1.8);iy<=Math.ceil(y+1.8);iy++)
                if(ix>=3&&iy>=3&&ix<world.width-3&&iy<world.height-3&&Math.hypot(ix-x,iy-y)<1.8)paths[ix][iy]=true;
        }
    }
    public boolean path(int x,int y){return x>=0&&y>=0&&x<world.width&&y<world.height&&paths[x][y];}
    private boolean validExpeditionStructure(String id,int variant,int x,int y){
        if(!validStructure(id,x,y))return false;
        if(variant==0)for(int tx=x-4;tx<=x+4;tx++)for(int ty=y+1;ty<=y+4;ty++){
            if(!land(tx,ty)||!biome(tx,ty).equals(id)||path(tx,ty))return false;
            if(Math.hypot(tx-world.caveX,ty-world.caveY)<4)return false;
            for(Structure other:structures)if(other.contains(tx,ty,1))return false;
        }
        // Later buildings cannot consume an already-selected outpost's utility yard.
        for(Structure other:structures)if(other.variant==0&&x+2>=other.x-4&&x-2<=other.x+4&&y+1>=other.y+1&&y-2<=other.y+4)return false;
        return true;
    }
    private boolean validStructure(String id,int x,int y){
        if(x<25||y<20||x>=world.width-25||y>=world.height-20||Math.hypot(x-world.campX,y-world.campY)<22)return false;
        for(int tx=x-2;tx<=x+2;tx++)for(int ty=y-2;ty<=y+1;ty++){
            if(path(tx,ty)||!biome(tx,ty).equals(id))return false;
            if(world.generatorVersion>=3&&!land(tx,ty))return false;
        }
        if(world.generatorVersion>=3){
            Point c=center(id);if(Math.abs(x-c.x)<6&&Math.abs(y-c.y)<6)return false;
            if(Math.hypot(x-world.caveX,y-world.caveY)<6)return false;
        }
        for(Structure s:structures)if(Math.abs(s.x-x)<(world.generatorVersion>=3?9:6)&&Math.abs(s.y-y)<(world.generatorVersion>=3?8:5))return false;
        return true;
    }
    public boolean reserved(int x,int y){
        if(world.generatorVersion>=3)return x>=0&&y>=0&&x<world.width&&y<world.height&&reservedCells[x][y];
        for(Structure s:structures)if(s.contains(x,y,1))return true;
        for(String id:IDS){Point c=center(id);if(Math.abs(x-c.x)<=3&&Math.abs(y-c.y)<=3)return true;}
        return false;
    }
    public String object(int x,int y,GeneratedLayout g){
        if(reserved(x,y))return null;
        String id=biome(x,y);double n=g.random(x,y,113);boolean v3=world.generatorVersion>=3;
        if(id.equals("forest")){
            if(v3?finiteTerrain.plant(x,y):n<.3&&x%2==0&&y%2==0)return g.pick(new String[]{"oaktree","birchtree"},x,y,114);
            if(v3&&n>.82&&n<.90)return "grass";
            if(v3&&n>.97)return g.pick(new String[]{"yellowflowerpatch","redflowerpatch"},x,y,116);
            return null;
        }
        // Native cellular vegetation has explicit solid-object spacing in v3; v2
        // retains its original lattice. Neither requires a global road network.
        if(v3?finiteTerrain.plant(x,y):x%2==0&&y%2==0&&n<.55){
            if(!id.equals("ruins"))return "mlbiome_"+id+"_tree";
            int[] ruins={0,1,4,5};return "mlbiome_ruins_flora"+ruins[(int)(g.random(x,y,114)*ruins.length)];
        }
        if(n>.79&&n<.92){
            int[] variants=id.equals("desert")?new int[]{0,1,2,5,6,7,8}:id.equals("ruins")?new int[]{2,3,6,7,8}:id.equals("wetland")?new int[]{2,4,5,6,7,8}:new int[]{2,3,4,5,6,7,8};
            int variant=variants[(int)(g.random(x,y,115)*variants.length)];
            if(v3&&id.equals("ruins")&&variant<6&&!finiteTerrain.secondarySolid(x,y))return null;
            return "mlbiome_"+id+"_flora"+variant;
        }
        if(n>.975&&(!v3||finiteTerrain.secondarySolid(x,y)))return "surfacerocksmall";
        return null;
    }
}
