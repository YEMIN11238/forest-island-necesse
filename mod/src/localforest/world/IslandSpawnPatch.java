package localforest.world;
import necesse.engine.modLoader.annotations.ModMethodPatch;
import necesse.engine.world.WorldEntity;
import net.bytebuddy.asm.Advice;
@ModMethodPatch(target=WorldEntity.class,name="initServer",arguments={boolean.class})
public class IslandSpawnPatch {
 @Advice.OnMethodEnter(skipOn=Advice.OnNonDefaultValue.class)
 public static boolean enter(@Advice.This WorldEntity world,@Advice.FieldValue(value="hasFoundSpawn",readOnly=false) boolean found){
  if(!IslandWorld.enabled())return false;
  IslandWorld.initializeNewWorld(world);found=true;return true;
 }
}
