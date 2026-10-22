package localforest.world;
import java.util.function.Function;
import necesse.engine.modLoader.annotations.ModMethodPatch;
import necesse.engine.network.server.ServerClient;
import necesse.engine.util.LevelIdentifier;
import net.bytebuddy.asm.Advice;
@ModMethodPatch(target=ServerClient.class,name="changeLevelCheck",arguments={LevelIdentifier.class,Function.class,Function.class,boolean.class})
public class IslandTravelPatch {
 @Advice.OnMethodEnter(skipOn=Advice.OnNonDefaultValue.class)
 public static boolean enter(@Advice.This ServerClient client,@Advice.Argument(0) LevelIdentifier target){
  if(!IslandWorld.enabled()||LevelIdentifier.SURFACE_IDENTIFIER.equals(target))return false;
  if(!IslandWorld.isIsland(client.getLevel()))return false;
  if(WorldDefinition.forLevel(client.getLevel())!=null&&LevelIdentifier.CAVE_IDENTIFIER.equals(target))return false;
  System.out.println("LOCALFOREST_MAP_EXIT_BLOCKED "+target);return true;
 }
}
