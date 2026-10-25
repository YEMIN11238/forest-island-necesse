package localforest.visual;

import necesse.engine.modLoader.annotations.ModMethodPatch;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import net.bytebuddy.asm.Advice;

/** A scene callback can draw an ordinary glow/tool between two plant sprites. */
@ModMethodPatch(target=TextureDrawOptions.class,name="draw",arguments={})
public class NativeSpriteFlushPatch {
    @Advice.OnMethodEnter public static void before(){CrownSpriteBatch.flush();}
}
