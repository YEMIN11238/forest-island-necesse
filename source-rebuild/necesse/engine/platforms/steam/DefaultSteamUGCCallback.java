package necesse.engine.platforms.steam;

import com.codedisaster.steamworks.SteamPublishedFileID;
import com.codedisaster.steamworks.SteamResult;
import com.codedisaster.steamworks.SteamUGCCallback;
import com.codedisaster.steamworks.SteamUGCDetails;
import com.codedisaster.steamworks.SteamUGCQuery;

public class DefaultSteamUGCCallback implements SteamUGCCallback {
   @Override
   public void onUGCQueryCompleted(SteamUGCQuery steamUGCQuery, int returnedResults, int totalResults, boolean cached, SteamResult steamResult) {
   }

   @Override
   public void onSubscribeItem(SteamPublishedFileID steamPublishedFileID, SteamResult steamResult) {
   }

   @Override
   public void onUnsubscribeItem(SteamPublishedFileID steamPublishedFileID, SteamResult steamResult) {
   }

   @Override
   public void onRequestUGCDetails(SteamUGCDetails steamUGCDetails, SteamResult steamResult) {
   }

   @Override
   public void onCreateItem(SteamPublishedFileID steamPublishedFileID, boolean needToAcceptEULA, SteamResult steamResult) {
   }

   @Override
   public void onSubmitItemUpdate(SteamPublishedFileID steamPublishedFileID, boolean needToAcceptEULA, SteamResult steamResult) {
   }

   @Override
   public void onDownloadItemResult(int appID, SteamPublishedFileID steamPublishedFileID, SteamResult steamResult) {
   }

   @Override
   public void onUserFavoriteItemsListChanged(SteamPublishedFileID steamPublishedFileID, boolean addedOrRemoved, SteamResult steamResult) {
   }

   @Override
   public void onSetUserItemVote(SteamPublishedFileID steamPublishedFileID, boolean voteUp, SteamResult steamResult) {
   }

   @Override
   public void onGetUserItemVote(
      SteamPublishedFileID steamPublishedFileID, boolean hasVotedUp, boolean hasVotedDown, boolean hasSkipped, SteamResult steamResult
   ) {
   }

   @Override
   public void onStartPlaytimeTracking(SteamResult steamResult) {
   }

   @Override
   public void onStopPlaytimeTracking(SteamResult steamResult) {
   }

   @Override
   public void onStopPlaytimeTrackingForAllItems(SteamResult steamResult) {
   }

   @Override
   public void onDeleteItem(SteamPublishedFileID steamPublishedFileID, SteamResult steamResult) {
   }
}
