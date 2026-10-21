package necesse.gfx.gameIcon;

import java.util.ArrayList;
import necesse.engine.registries.GameRegistry;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.registries.MobRegistry;

public class GameEmoteRegistry extends GameRegistry<GameEmote> {
   public static final ArrayList<GameEmoteRegistry.CustomGameEmoteHandler> customHandlers = new ArrayList<>();
   public static final GameEmoteRegistry instance = new GameEmoteRegistry();

   private GameEmoteRegistry() {
      super("GameEmote", 32762);
   }

   @Override
   public void registerCore() {
      registerEmote("checkmark", new TextureGameEmote("ui/mapicons/checkmark", false));
      registerEmote("cross", new TextureGameEmote("ui/mapicons/cross", false));
      registerEmote("heart", new UITextureEmote(ui -> ui.heart_fill));
      registerEmote("food", new UITextureEmote(ui -> ui.food_fill));
      registerEmote("resilience", new UITextureEmote(ui -> ui.resilience_fill));
      registerEmote("star", new ColoredUITextureEmote(ui -> ui.star_icon));
   }

   protected void onRegister(GameEmote object, int id, String stringID, boolean isReplace) {
   }

   @Override
   protected void onRegistryClose() {
   }

   public static <T extends GameEmote> T registerEmote(String stringID, T preset) {
      instance.register(stringID, preset);
      return preset;
   }

   public static GameEmote getIcon(int id) {
      return instance.getElement(id);
   }

   public static GameEmote getIcon(String stringID) {
      return instance.getElement(stringID);
   }

   public static Iterable<GameEmote> getAllIcons() {
      return instance.getElements();
   }

   static {
      customHandlers.add(stringID -> {
         if (stringID.startsWith("mob_")) {
            String mobStringID = stringID.substring("mob_".length());
            if (MobRegistry.getMobID(mobStringID) != -1) {
               return new MobIconGameEmote(mobStringID) {
                  @Override
                  public String getEmoteParseStringID() {
                     return "mob_" + this.mobStringID;
                  }
               };
            }
         }

         return null;
      });
      customHandlers.add(stringID -> {
         if (stringID.startsWith("item_")) {
            String itemStringID = stringID.substring("item_".length());
            if (ItemRegistry.getItemID(itemStringID) != -1) {
               return new ItemIconGameEmote(itemStringID) {
                  @Override
                  public String getEmoteParseStringID() {
                     return "item_" + this.itemStringID;
                  }
               };
            }
         }

         return null;
      });
   }

   public interface CustomGameEmoteHandler {
      GameEmote handle(String var1);
   }
}
