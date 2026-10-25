package localforest.visual;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/** Startup-only albedo preparation. Lighting remains the native scene light pass.
 * Both ground tiles and road verges sample these exact world-space pixels.
 */
public final class BiomeTerrainMaterials {
    public static final int PERIOD=512;
    private static final Map<String,Material> materials=new HashMap<>();
    private record Palette(int ground,int road,double detail) {}
    private static final Map<String,Palette> PALETTES=Map.of(
        "wetland",new Palette(0x667759,0x706651,.36),
        "mushroom",new Palette(0x715e70,0x5d525b,.26),
        "ruins",new Palette(0x858473,0x6e6f66,.30),
        "snowfield",new Palette(0xdae2e6,0xacbec6,.18),
        "desert",new Palette(0xc6a977,0xa98d63,.34),
        "volcano",new Palette(0x605757,0x786f6a,.30)
    );
    public static final class Material {
        public final String key;
        public final BufferedImage ground;
        private final int[] groundPixels,roadPixels;
        private Material(String key,BufferedImage ground,int[] pixels,int[] road){this.key=key;this.ground=ground;groundPixels=pixels;roadPixels=road;}
        public int ground(int worldX,int worldY){return groundPixels[index(worldX,worldY)];}
        public int road(int worldX,int worldY){return roadPixels[index(worldX,worldY)];}
        private int index(int x,int y){return Math.floorMod(x,PERIOD)+Math.floorMod(y,PERIOD)*PERIOD;}
    }
    public static String key(String biome){return "mosslume/biomes/"+biome+"/ground-runtime";}
    public static Material get(String key){
        Material result=materials.get(key);
        if(result==null)throw new IllegalStateException("Biome terrain material not prepared: "+key);
        return result;
    }
    public static Material prepare(String biome,BufferedImage source){
        Palette palette=PALETTES.get(biome);
        if(palette==null||source==null)throw new IllegalArgumentException("Unknown biome terrain "+biome);
        // Source files stay intact. Reduce the authored 32px motif's contrast, then
        // nearest-sample a continuous periodic warp to remove its obvious tile grid.
        double meanR=0,meanG=0,meanB=0;
        for(int y=0;y<source.getHeight();y++)for(int x=0;x<source.getWidth();x++){
            int pixel=source.getRGB(x,y);meanR+=(pixel>>>16)&255;meanG+=(pixel>>>8)&255;meanB+=pixel&255;
        }
        int count=source.getWidth()*source.getHeight();meanR/=count;meanG/=count;meanB/=count;
        int[] ground=new int[PERIOD*PERIOD],road=new int[ground.length];
        int seed=biome.hashCode();
        for(int y=0;y<PERIOD;y++)for(int x=0;x<PERIOD;x++){
            int warpX=(int)Math.round(noise(x,y,64,seed+3)*9),warpY=(int)Math.round(noise(x,y,64,seed+41)*9);
            int pixel=source.getRGB(Math.floorMod(x+warpX,source.getWidth()),Math.floorMod(y+warpY,source.getHeight()));
            double r=((pixel>>>16)&255)-meanR,g=((pixel>>>8)&255)-meanG,b=(pixel&255)-meanB;
            double luminance=r*.2126+g*.7152+b*.0722;
            double field=noise(x,y,128,seed+67)*4+noise(x,y,32,seed+97)*2;
            double grain=random(x,y,seed+131)*1.3;
            int index=x+y*PERIOD;
            ground[index]=color(palette.ground,r*.45+luminance*.55,g*.45+luminance*.55,b*.45+luminance*.55,palette.detail,field+grain);
            road[index]=color(palette.road,luminance,luminance,luminance,.13,field*.65+grain);
        }
        BufferedImage image=new BufferedImage(PERIOD,PERIOD,BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0,0,PERIOD,PERIOD,ground,0,PERIOD);
        Material material=new Material(key(biome),image,ground,road);materials.put(material.key,material);return material;
    }
    private static int color(int base,double r,double g,double b,double detail,double field){
        return 0xff000000|(clamp(((base>>>16)&255)+r*detail+field)<<16)
            |(clamp(((base>>>8)&255)+g*detail+field)<<8)|clamp((base&255)+b*detail+field);
    }
    private static int clamp(double value){return Math.max(0,Math.min(255,(int)Math.round(value)));}
    /** Periodic value noise affects albedo only; image sampling remains nearest. */
    private static double noise(int x,int y,int cell,int seed){
        int grid=PERIOD/cell,gx=Math.floorDiv(x,cell),gy=Math.floorDiv(y,cell);
        double u=Math.floorMod(x,cell)/(double)cell,v=Math.floorMod(y,cell)/(double)cell;
        u=u*u*(3-2*u);v=v*v*(3-2*v);
        double a=random(Math.floorMod(gx,grid),Math.floorMod(gy,grid),seed),b=random(Math.floorMod(gx+1,grid),Math.floorMod(gy,grid),seed);
        double c=random(Math.floorMod(gx,grid),Math.floorMod(gy+1,grid),seed),d=random(Math.floorMod(gx+1,grid),Math.floorMod(gy+1,grid),seed);
        return (a+(b-a)*u)*(1-v)+(c+(d-c)*u)*v;
    }
    private static double random(int x,int y,int seed){
        int h=x*374761393+y*668265263+seed*1442695041;h=(h^(h>>>13))*1274126177;h^=h>>>16;
        return (h&0xffff)/32767.5-1;
    }
    private BiomeTerrainMaterials(){}
}
