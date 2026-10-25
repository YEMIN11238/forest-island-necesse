package localforest.visual;

import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import necesse.engine.modLoader.annotations.ModMethodPatch;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.DrawOptionsList;
import necesse.gfx.drawOptions.human.HumanDrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.gameTexture.GameTexture;
import necesse.level.maps.levelData.settlementData.settler.Settler;

/** Exact current head pixels and real native face-render geometry/signatures, no window. */
public final class ResidentPortraitsTest {
    private static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
    private static Object field(Object object,String name)throws Exception{
        for(Class<?> type=object.getClass();type!=null;type=type.getSuperclass())try{Field f=type.getDeclaredField(name);f.setAccessible(true);return f.get(object);}catch(NoSuchFieldException missing){}
        throw new NoSuchFieldException(name);
    }
    private static void collect(DrawOptions options,List<TextureDrawOptions> textures){
        if(options instanceof TextureDrawOptions texture)textures.add(texture);
        if(options instanceof DrawOptionsList list)for(DrawOptions child:list)collect(child,textures);
    }
    public static void main(String[] args)throws Exception{
        int signatures=0;
        for(Class<?> patch:ResidentPortraitPatches.class.getDeclaredClasses()){
            ModMethodPatch spec=patch.getAnnotation(ModMethodPatch.class);if(spec==null)continue;
            var method=spec.target().getDeclaredMethod(spec.name(),spec.arguments());check(method!=null,"Native method signature changed "+patch.getName());signatures++;
        }
        check(signatures==5,"Five native portrait entry points required");
        Path output=Path.of("mosslume/art/hand-grips-r8/qa");Files.createDirectories(output);
        BufferedImage contact=new BufferedImage(5*128,128,BufferedImage.TYPE_INT_ARGB);int index=0,checks=0;
        for(String role:new String[]{"farmer","carpenter","merchant","ashuo","luan"}){
            BufferedImage idle=ImageIO.read(Path.of("mod/resources/mosslume/residents/"+role+"-idle-down.png").toFile());
            BufferedImage square=ResidentPortraits.headSquare(idle),atlas=ResidentPortraits.nativeAtlas(square);
            int nonempty=0;
            for(int y=0;y<32;y++)for(int x=0;x<32;x++){
                int pixel=square.getRGB(x,y);if((pixel>>>24)>0){nonempty++;boolean same=false;
                    for(int sy=0;sy<40&&!same;sy++)for(int sx=0;sx<64;sx++)if(idle.getRGB(sx,sy)==pixel){same=true;break;}
                    check(same,"Portrait contains pixels absent from current head "+role);
                }
                for(int row=0;row<4;row++)check(atlas.getRGB(x+16,y+row*64+12)==pixel,"Native head sprite row mismatch "+role);
                for(int oy=0;oy<4;oy++)for(int ox=0;ox<4;ox++)contact.setRGB(index*128+x*4+ox,y*4+oy,pixel);
            }
            check(nonempty>100,"Current head empty "+role);index++;
            GameTexture texture=new GameTexture("offline current head "+role,atlas);
            for(int size:new int[]{16,24,32,48}){
                HumanDrawOptions head=new HumanDrawOptions(null).headTexture(texture).drawEyes(false);
                DrawOptions options=Settler.getHumanFaceDrawOptions(head,size,123,45);List<TextureDrawOptions> textures=new ArrayList<>();collect(options,textures);
                check(textures.size()==1&&textures.get(0).texture==texture,"Native face path added stock head/eyes/body "+role);
                Object quad=field(textures.get(0),"opts");float x=((Number)field(quad,"x1")).floatValue(),y=((Number)field(quad,"y1")).floatValue();
                check(Math.abs(x+size*.5f-123)<.01&&Math.abs(y+size*.375f-45)<.01,"Native face position changed "+role);
                check((int)field(head,"spriteY")==2&&!(boolean)field(head,"drawEyes"),"Native slot uses wrong row or stock eyes");checks++;
            }
        }
        ImageIO.write(contact,"png",output.resolve("current-resident-heads.png").toFile());
        System.out.println("ResidentPortraitsTest PASS: five exact current head crops; "+signatures+" actual kernel method signatures; "+checks+" native face geometries, single custom texture and no original eyes/body");
    }
}
