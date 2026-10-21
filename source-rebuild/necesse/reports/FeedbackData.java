package necesse.reports;

public class FeedbackData extends BasicsData {
   public FeedbackData(String state) {
      super(state);
   }

   public String generateFullFeedback(String message) {
      String fullFeedback = "";
      fullFeedback = fullFeedback + "Game version: " + this.data.get("game_version") + "\n";
      fullFeedback = fullFeedback + "Steam build: " + this.data.get("steam_build") + "\n";
      fullFeedback = fullFeedback + "Steam name: " + this.data.get("steam_name") + "\n";
      fullFeedback = fullFeedback + "Authentication: " + this.data.get("authentication") + "\n";
      fullFeedback = fullFeedback + "Launch parameters: " + this.data.get("launch_parameters") + "\n";
      fullFeedback = fullFeedback + "Current state: " + this.data.get("game_state") + "\n";
      fullFeedback = fullFeedback + "Current language: " + this.data.get("game_language") + "\n";
      fullFeedback = fullFeedback + "\n";
      fullFeedback = fullFeedback + "Message:\n";
      return fullFeedback + message;
   }
}
