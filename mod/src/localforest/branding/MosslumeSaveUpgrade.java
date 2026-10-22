package localforest.branding;
import java.util.List;
import necesse.engine.modLoader.LoadedMod;
import necesse.engine.modLoader.ModLoader;
import necesse.engine.modLoader.ModSaveInfo;
import necesse.engine.save.WorldSave;
import necesse.engine.world.WorldEntity;

/** Exact migration for our own art-only adapter; other mod warnings remain native. */
public final class MosslumeSaveUpgrade {
 public static void prepare(WorldSave save){
  if(!MosslumeBranding.enabled()||save==null||!save.filePath.getName().equals("ForestIsland.zip"))return;
  WorldEntity world=save.getWorldEntity();List<LoadedMod> current=ModLoader.getEnabledMods();
  if(world==null||world.lastMods==null||world.lastMods.size()!=1||current.size()!=1)return;
  ModSaveInfo previous=world.lastMods.get(0);LoadedMod now=current.get(0);
  boolean ownCompatibleUpgrade=(previous.version.equals("0.1.0")&&now.version.equals("0.2.0"))
   ||((previous.version.equals("0.1.0")||previous.version.equals("0.2.0"))&&now.version.equals("0.3.0"));
  if(previous.id.equals("localforest.island")&&now.id.equals(previous.id)&&ownCompatibleUpgrade){
   world.lastMods.set(0,now.getModSaveInfo());
   System.out.println("MOSSLUME_SAVE_ART_UPGRADE "+previous.version+"->"+now.version+" registry IDs unchanged; source profile copy retained");
  }
 }
 private MosslumeSaveUpgrade(){}
}
