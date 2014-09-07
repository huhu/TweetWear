package hu.rycus.tweetwear.tasks;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;

import org.scribe.model.Token;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import hu.rycus.tweetwear.common.api.ApiClientHelper;
import hu.rycus.tweetwear.common.api.ApiClientRunnable;
import hu.rycus.tweetwear.common.model.Tweet;
import hu.rycus.tweetwear.common.util.Constants;
import hu.rycus.tweetwear.common.util.TweetData;
import hu.rycus.tweetwear.preferences.ListSettings;
import hu.rycus.tweetwear.twitter.TwitterFactory;
import hu.rycus.tweetwear.twitter.account.Account;
import hu.rycus.tweetwear.twitter.account.IAccountProvider;
import hu.rycus.tweetwear.twitter.client.ITwitterClient;

public class FetchTweetsTask extends ApiClientRunnable {

    private static final String TAG = FetchTweetsTask.class.getSimpleName();

    private static final int DEFAULT_TWEET_LIMIT = 10;

    private final IAccountProvider accountProvider;
    private final ITwitterClient client;

    private Collection<Tweet> existingTweets;
    private Collection<Tweet> newTweets;

    private Long sinceId;

    private int tweetCountLimit = DEFAULT_TWEET_LIMIT;

    public FetchTweetsTask(final IAccountProvider accountProvider, final ITwitterClient client) {
        this.accountProvider = accountProvider;
        this.client = client;
    }

    @Override
    protected void run(final Context context, final GoogleApiClient apiClient) throws Exception {
        loadExistingTweets(apiClient);
        loadNewTweets(context);

        if (!newTweets.isEmpty()) {
            for (final Tweet tweet : newTweets) {
                TweetData.of(tweet).sendBlocking(apiClient);
            }

            final Collection<Tweet> toRemove = getTweetsToRemove();
            if (!toRemove.isEmpty()) {
                Log.d(TAG, String.format("About to remove %d tweets, existing: %d, new: %d",
                        toRemove.size(), existingTweets.size(), newTweets.size()));

                for (final Tweet tweet : toRemove) {
                    TweetData.of(tweet).deleteBlocking(apiClient);
                }
            }

            ApiClientHelper.sendMessageToConnectedNode(
                    apiClient, Constants.DataPath.SYNC_COMPLETE.get(), null);
        } else {
            Log.d(TAG, "There are no tweets to send at this time");
        }
    }

    protected void loadNewTweets(final Context context) {
        final TreeSet<Tweet> tweets = new TreeSet<Tweet>();
        for (final Account account : accountProvider.getAccounts(context)) {
            final Token token = account.getAccessToken();
            final ListSettings listSettings = account.getListSettings();

            final long userId = TwitterFactory.getUserId(context, client, token);

            final Collection<Tweet> accountTweets = loadNewTweetsForAccount(token, listSettings);
            markOwnTweets(userId, accountTweets);

            Log.d(TAG, String.format("Tweets retrieved for account (%s): %d",
                    account.getUsername(), accountTweets.size()));

            tweets.addAll(accountTweets);
        }

        setNewTweets(tweets);
    }

    protected Collection<Tweet> loadNewTweetsForAccount(final Token token,
                                                        final ListSettings listSettings) {
        final Set<Tweet> tweets = new HashSet<Tweet>();

        if (listSettings.isTimelineSelected()) {
            final Tweet[] timelineTweets = client.getTimeline(
                    token, tweetCountLimit, sinceId, null, null, null, null, null);
            tweets.addAll(Arrays.asList(timelineTweets));

            Log.d(TAG, String.format("Loaded tweets for timeline: %d", timelineTweets.length));
        }

        for (final long listId : listSettings.getSelectedListIds()) {
            final Tweet[] listTweets = client.getListStatuses(
                    token, listId, tweetCountLimit, sinceId, null, null, null);
            tweets.addAll(Arrays.asList(listTweets));

            Log.d(TAG, String.format("Loaded tweets for list #%d: %d", listId, listTweets.length));
        }

        return tweets;
    }

    protected void markOwnTweets(final long userId, final Collection<Tweet> tweets) {
        for (final Tweet tweet : tweets) {
            tweet.setOwnTweet(tweet.getUser().getId() == userId);
        }
    }

    protected void loadExistingTweets(final GoogleApiClient apiClient) {
        final Collection<Tweet> tweets = TweetData.loadAll(apiClient);
        setExistingTweets(tweets);
    }

    protected Collection<Tweet> getTweetsToRemove() {
        if (newTweets.isEmpty()) {
            return Collections.emptyList();
        } else {
            final List<Tweet> candidates = new ArrayList<Tweet>(new TreeSet<Tweet>(existingTweets));
            Collections.reverse(candidates);
            final int toRemove = existingTweets.size() + newTweets.size() - tweetCountLimit;
            return candidates.subList(0, Math.max(0, toRemove));
        }
    }

    protected void setTweetCountLimit(final int tweetCountLimit) {
        this.tweetCountLimit = tweetCountLimit;
    }

    protected Collection<Tweet> getNewTweets() {
        return newTweets;
    }

    protected void setNewTweets(final Collection<Tweet> tweets) {
        this.newTweets = tweets;
    }

    protected Collection<Tweet> getExistingTweets() {
        return existingTweets;
    }

    protected void setExistingTweets(final Collection<Tweet> tweets) {
        this.existingTweets = tweets;
        checkSinceIdInTweets(tweets);
    }

    protected void checkSinceIdInTweets(final Collection<Tweet> tweets) {
        for (final Tweet tweet : tweets) {
            checkSinceId(tweet);
        }
    }

    protected void checkSinceId(final Tweet tweet) {
        if (sinceId == null || sinceId < tweet.getId()) {
            sinceId = tweet.getId();
        }
    }

}
