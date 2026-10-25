package localforest.visual;

import java.awt.image.BufferedImage;
import localforest.scene.TileBounds;

/** Offline asset/clock-independent invariants. Does not create an OpenGL context or load a save. */
public final class VisualMaterialWindTest {
    public static void main(String[] args){
        BufferedImage image=new BufferedImage(16,20,BufferedImage.TYPE_INT_ARGB);
        for(int y=4;y<18;y++)for(int x=3;x<13;x++)image.setRGB(x,y,0xff8ba35a);
        VisualMaterialDefinition flexible=new VisualMaterialDefinition("tree",image,8,18,112,108,.12f,true);
        VisualMaterialDefinition rigid=new VisualMaterialDefinition("ice",image,8,18,0,24,1,false);
        check(flexible.minX==3&&flexible.minY==4&&flexible.maxX==13&&flexible.maxY==18,"Visible bounds include transparent padding");
        check(flexible.scale(1)==8,"Visible height, not canvas height, defines scale");
        check(flexible.left(200,2)+8*2==200&&flexible.top(200,2)+18*2==200,"Source foot does not map to world foot");
        for(String id:new String[]{"mosslume-house-shadow","mosslume/world/house-layers/roof","mosslume/world/house-layers/walls"}){
            VisualMaterialDefinition house=new VisualMaterialDefinition(id,image,8,18,0,100,1,false);
            check(Math.abs(house.effectiveShadowHeight(2)-100)<.001,"Base house height changed");
            check(Math.abs(house.effectiveShadowHeight(2.8f)-140)<.001,"Seven-tile residence kept five-tile shadow/relief height");
            check(Math.abs(house.effectiveShadowHeight(1)-50)<.001,"Small residence retained full height");
        }
        check(flexible.effectiveShadowHeight(flexible.scale(1))==108&&flexible.effectiveShadowHeight(1)==108,
            "Fixed-target tree height was scaled twice");
        byte[] normal=flexible.bakeNormals(image);
        int center=(8+10*16)*4;
        check(Math.abs((normal[center]&255)-128)<=1&&Math.abs((normal[center+1]&255)-128)<=1&&(normal[center+2]&255)>=254,"Flat painted surface must keep a flat normal");
        check((normal[(3+10*16)*4]&255)<128&&(normal[(12+10*16)*4]&255)>128,"Silhouette normals point outward");
        check((normal[3]&255)==0&&(normal[center+3]&255)==255,"Normal coverage differs from original art");
        double variance=0,maxStep=0;float last=WindField.offset(0,4,9,0,100,flexible,true,true);
        for(int i=1;i<=1200;i++){
            float wind=WindField.offset(i/60.0,4,9,0,100,flexible,true,true);
            maxStep=Math.max(maxStep,Math.abs(wind-last));last=wind;variance+=wind*wind;
            check(WindField.offset(i/60.0,4,9,1,100,flexible,true,true)==0,"Root drifts");
            check(WindField.offset(i/60.0,4,9,0,100,rigid,false,true)==0,"Rigid object sways");
        }
        check(variance>10&&maxStep<.12,"Wind is frozen or discontinuous");
        float left=Float.POSITIVE_INFINITY,right=Float.NEGATIVE_INFINITY;
        for(int i=0;i<=720;i++){
            float offset=WindField.offset(i/60.0,4,9,.15f,134,flexible,true,true);
            left=Math.min(left,offset);right=Math.max(right,offset);
        }
        check(right-left>=3&&right-left<=9,"Mature crown must visibly sway without sweeping across adjacent trunks");
        check(WindField.phase(4,9)!=WindField.phase(5,9),"Neighbours share the same phase");
        check(WindField.phase(-12345,98765)==WindField.phase(-12345,98765),"Phase changes across draws");
        TileColorCache cache=new TileColorCache(new TileBounds(-1,-1,2,2));cache.begin(1);
        float[] color=cache.storage(0,0,16);cache.put(0,0,color);check(cache.get(0,0)==color,"Same-frame cache miss");
        cache.begin(2);check(cache.get(0,0)==null,"Dynamic light reused stale frame color");
        check(cache.storage(0,0,16)==color,"Frame refresh allocates instead of reusing color storage");
        System.out.println("VisualMaterialWindTest PASS: visible bounds, feet, Sobel normals, wind roots/rigidity/phase/continuity, dynamic-light cache reuse");
    }
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
