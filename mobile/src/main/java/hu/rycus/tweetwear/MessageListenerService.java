package hu.rycus.tweetwear;

import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import hu.rycus.tweetwear.common.api.ApiClientHelper;
import hu.rycus.tweetwear.common.util.Constants;
import hu.rycus.tweetwear.preferences.Preferences;
import hu.rycus.tweetwear.tasks.ClearExistingTweetsTask;
import hu.rycus.tweetwear.tasks.FavoriteTask;
import hu.rycus.tweetwear.tasks.PostTweetTask;
import hu.rycus.tweetwear.tasks.RetweetTask;

public class MessageListenerService extends WearableListenerService {

    private static final String TAG = MessageListenerService.class.getSimpleName();

    @Override
    public void onMessageReceived(final MessageEvent messageEvent) {
        final String path = messageEvent.getPath();
        if (Constants.DataPath.RETWEET.matches(path)) {
            final String strTweetId = Constants.DataPath.RETWEET.replace(path, "$1");
            final long tweetId = Long.parseLong(strTweetId);
            Log.d(TAG, String.format("Received retweet request: %d", tweetId));

            final RetweetTask task = new RetweetTask(tweetId);
            ApiClientHelper.runAsynchronously(this, task);
        } else if (Constants.DataPath.FAVORITE.matches(path)) {
            final String strTweetId = Constants.DataPath.FAVORITE.replace(path, "$1");
            final long tweetId = Long.parseLong(strTweetId);
            Log.d(TAG, String.format("Received favorite request: %d", tweetId));

            final FavoriteTask task = new FavoriteTask(tweetId);
            ApiClientHelper.runAsynchronously(this, task);
        } else if (Constants.DataPath.POST_NEW_TWEET.matches(path)) {
            final String text = new String(messageEvent.getData());
            Log.d(TAG, String.format("Received request to post a new tweet saying: %s", text));

            ApiClientHelper.runAsynchronously(this, PostTweetTask.newTweet(text));
        } else if (Constants.DataPath.POST_REPLY.matches(path)) {
            final String strTweetId = Constants.DataPath.POST_REPLY.replace(path, "$1");
            final long tweetId = Long.parseLong(strTweetId);
            final String text = new String(messageEvent.getData());
            Log.d(TAG, String.format("Received request to post a reply saying: %s", text));

            ApiClientHelper.runAsynchronously(this, PostTweetTask.reply(text, tweetId));
        } else if (Constants.DataPath.MARK_AS_READ.matches(path)) {
            Log.d(TAG, "Received mark-as-read request");

            if (Preferences.isMarkAsReadOnDeleteEnabled(this)) {
                ApiClientHelper.runAsynchronously(this, new ClearExistingTweetsTask(true));
            } else {
                Log.d(TAG, "Mark-as-read is disabled, not doing anything");
            }
        }
    }

}
