package necesse.engine.commands.serverCommands.setupCommand;

import java.util.ArrayList;
import java.util.List;
import necesse.engine.network.server.ServerClient;

public class CharacterBuildConcat extends CharacterBuild {
   public final ArrayList<CharacterBuild> builds;
   public final ArrayList<String> buildIDs = new ArrayList<>();

   public CharacterBuildConcat(Object... builds) {
      this.builds = new ArrayList<>();

      for (Object build : builds) {
         if (build instanceof CharacterBuild) {
            this.builds.add((CharacterBuild)build);
         } else {
            if (!(build instanceof String)) {
               throw new IllegalArgumentException("Build parameters must either be CharacterBuild or String");
            }

            this.buildIDs.add((String)build);
         }
      }
   }

   @Override
   public void apply(ServerClient client) {
   }

   @Override
   public void addApplies(List<CharacterBuild> applies) {
      for (CharacterBuild build : this.builds) {
         build.addApplies(applies);
      }

      for (String buildID : this.buildIDs) {
         CharacterBuild build = DemoServerCommand.builds.get(buildID);
         if (build != null) {
            build.addApplies(applies);
         }
      }
   }
}
