package necesse.engine.loading;

import java.util.Objects;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;

public class GameNoticeException extends RuntimeException {
   public GameMessage message;

   public GameNoticeException(GameMessage message) {
      super(message.translate());
      Objects.requireNonNull(message);
      this.message = message;
   }

   public GameNoticeException(String category, String key) {
      this(new LocalMessage(category, key));
   }
}
