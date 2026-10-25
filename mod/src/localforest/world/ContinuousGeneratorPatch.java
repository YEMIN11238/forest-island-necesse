package localforest.world;

import necesse.engine.modLoader.annotations.ModMethodPatch;
import necesse.engine.world.WorldEntity;
import necesse.engine.world.biomeGenerator.BiomeGeneratorStack;
import net.bytebuddy.asm.Advice;

/** World presets, spawn queries, surface and cave must all read one native
 * world-owned stack. A private level-only stack would let presets use unrelated
 * vanilla biomes at the same coordinates. Existing v1-v3 stacks stay untouched. */
@ModMethodPatch(target=WorldEntity.class,name="getGeneratorStack",arguments={})
public final class ContinuousGeneratorPatch {
    @Advice.OnMethodEnter public static void before(@Advice.This WorldEntity world,
        @Advice.FieldValue(value="generatorStack",readOnly=false) BiomeGeneratorStack stack){
        if(!IslandWorld.enabled())return;
        WorldDefinition d=WorldDefinition.forWorld(world);
        if(d!=null&&d.isContinuous()&&!(stack instanceof MosslumeContinuousStack))stack=new MosslumeContinuousStack(d);
    }
    private ContinuousGeneratorPatch(){}
}
