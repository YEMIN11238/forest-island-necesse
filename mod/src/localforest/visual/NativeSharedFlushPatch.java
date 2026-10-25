package localforest.visual;
import necesse.engine.modLoader.annotations.ModMethodPatch;
import necesse.gfx.drawOptions.texture.SharedTextureDrawOptions;
import net.bytebuddy.asm.Advice;
@ModMethodPatch(target=SharedTextureDrawOptions.class,name="draw",arguments={int.class})
public class NativeSharedFlushPatch {
    @Advice.OnMethodEnter public static void before(){CrownSpriteBatch.flush();}
}
