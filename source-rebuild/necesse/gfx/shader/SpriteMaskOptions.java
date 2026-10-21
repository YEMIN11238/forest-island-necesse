package necesse.gfx.shader;

import necesse.gfx.drawOptions.texture.TextureDrawOptionsEnd;

public interface SpriteMaskOptions {
   TextureDrawOptionsEnd apply(TextureDrawOptionsEnd var1, int var2);

   void use(SceneShader var1, int var2);
}
