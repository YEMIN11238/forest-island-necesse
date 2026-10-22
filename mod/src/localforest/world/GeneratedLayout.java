package localforest.world;

import localforest.world.noise.FastNoiseLite;

/** Coordinate-pure terrain sampling: exploration/region generation order cannot change a tile. */
public final class GeneratedLayout {
    public final WorldDefinition world;
    private final FastNoiseLite coast,vegetation;
    public GeneratedLayout(WorldDefinition world){
        this.world=world;
        coast=new FastNoiseLite((int)world.seed);coast.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2);
        coast.SetFrequency(.035f);coast.SetFractalType(FastNoiseLite.FractalType.FBm);coast.SetFractalOctaves(3);
        vegetation=new FastNoiseLite((int)(world.seed>>>32)^0x5389ac);vegetation.SetFrequency(.022f);
        vegetation.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2);
    }
    public boolean inside(int x,int y){return x>=3&&y>=3&&x<world.width-3&&y<world.height-3;}
    public boolean land(int x,int y){
        if(!inside(x,y))return false;
        if(campArea(x,y)||trail(x,y))return true;
        double nx=(x-world.campX)/(world.width*.5),ny=(y-world.campY)/(world.height*.5);
        double radius=world.content.decimal(world.preset+".landRadius");
        double field=1-Math.sqrt(nx*nx+ny*ny)/radius+coast.GetNoise(x,y)*.20;
        return field>.07;
    }
    public boolean campArea(int x,int y){return Math.hypot(x-world.campX,y-world.campY)<16;}
    public boolean trail(int x,int y){
        if(!inside(x,y)||x<world.campX-18||x>world.caveX+3)return false;
        double t=(x-world.campX)/(double)(world.caveX-world.campX);
        double py=world.campY-12*t+Math.sin(t*Math.PI*2)*2.8;
        return Math.abs(y-py)<world.content.decimal("camp.pathWidth");
    }
    public boolean path(int x,int y){return trail(x,y)||((Math.abs(x-world.campX)<1.4)&&(y>=world.campY-3&&y<=world.houseY+3));}
    public boolean coast(int x,int y){
        return !land(x,y)&&(land(x-1,y)||land(x+1,y)||land(x,y-1)||land(x,y+1));
    }
    public String biome(int x,int y){return world.content.biome(vegetation.GetNoise(x,y));}
    public boolean house(int x,int y){return x>=world.houseX-2&&x<=world.houseX+2&&y>=world.houseY-4&&y<=world.houseY-1;}
    public boolean farmland(int x,int y){return x>=world.campX-9&&x<=world.campX-6&&y>=world.campY+5&&y<=world.campY+7;}
    public long hash(int x,int y,int channel){long h=world.seed^((long)x*0x9e3779b97f4a7c15L)^((long)y*0xc2b2ae3d27d4eb4fL)^((long)channel*0x165667b19e3779f9L);h=(h^(h>>>30))*0xbf58476d1ce4e5b9L;h=(h^(h>>>27))*0x94d049bb133111ebL;return h^(h>>>31);}
    public double random(int x,int y,int channel){return (hash(x,y,channel)>>>11)*0x1.0p-53;}
    public String pick(String[] ids,int x,int y,int channel){return ids[(int)Math.floor(random(x,y,channel)*ids.length)];}
    public String object(int x,int y){
        if(!land(x,y)||path(x,y)||house(x,y)||farmland(x,y))return null;
        double distance=Math.hypot(x-world.campX,y-world.campY);
        if(distance<12)return null;
        if(Math.hypot(x-world.caveX,y-world.caveY)<4)return null;
        String biome=biome(x,y);double n=random(x,y,7);
        double density=world.content.decimal(world.preset+".treeDensity")*world.content.decimal(biome+".treeMultiplier");
        // One tree per staggered two-tile cell leaves traversable corridors within dense woodland.
        if(n<density&&Math.floorMod(x,2)==0&&Math.floorMod(y,2)==0)return pick(world.content.csv(biome+".trees"),x,y,11);
        if(n>.76&&n<.82)return pick(world.content.csv("bushes"),x,y,12);
        if(n>.85&&n<.88)return pick(world.content.csv("rocks"),x,y,13);
        if(n>.89&&n<.93)return pick(world.content.csv("flowers"),x,y,14);
        if(n>.94)return "grass";
        return null;
    }
    public boolean caveFloor(int x,int y){
        if(!inside(x,y))return false;
        if(Math.hypot(x-world.caveX,y-world.caveY)<8)return true;
        double line=world.caveY+Math.sin((x-world.caveX)*.07)*7;
        return Math.abs(y-line)<3||coast.GetNoise(x+900,y+900)>.05;
    }
}
