package localforest.visual;
import localforest.scene.SceneStructures;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.modLoader.annotations.ModMethodPatch;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawables.LevelDrawUtils;
import necesse.level.maps.Level;
import net.bytebuddy.asm.Advice;
@ModMethodPatch(target=LevelDrawUtils.class,name="setupNextLogic",
    arguments={GameCamera.class,PlayerMob.class,TickManager.class,boolean.class})
public class ScenePrepareScopePatch {
    @Advice.OnMethodEnter public static void before(@Advice.FieldValue("level") Level level,@Advice.Argument(0) GameCamera camera){
        if(IslandVisuals.active(level)){
            NaturalPathRenderer.beginFrame();
            SceneStructures.refreshForDraw(level);
            GroundDecorationPolicy.refresh(level);
            OceanGeometryCache.prepare(level,camera);
        }
    }
}
