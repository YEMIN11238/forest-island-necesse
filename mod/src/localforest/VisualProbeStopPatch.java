package localforest;
import necesse.engine.modLoader.annotations.ModMethodPatch;
import necesse.engine.network.server.Server;
import net.bytebuddy.asm.Advice;
@ModMethodPatch(target=Server.class,name="tick",arguments={})
public class VisualProbeStopPatch {
 @Advice.OnMethodEnter public static void before(@Advice.This Server server){VisualSceneProbe.stopOnServerTick(server);}
}
