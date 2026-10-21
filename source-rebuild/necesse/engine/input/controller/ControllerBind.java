package necesse.engine.input.controller;

import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.gfx.gameTexture.GameTexture;

public abstract class ControllerBind {
   @Override
   public abstract int hashCode();

   @Override
   public abstract boolean equals(Object var1);

   public abstract boolean isBound();

   public abstract void saveBind(SaveData var1);

   public abstract void loadBind(LoadData var1);

   public abstract GameTexture getGlyph(ControllerHandle var1);
}
