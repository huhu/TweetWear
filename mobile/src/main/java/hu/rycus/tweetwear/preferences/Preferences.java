package hu.rycus.tweetwear.preferences;

import android.app.AlarmManager;
import android.content.Context;
import android.content.SharedPreferences;

import org.scribe.model.Token;

import hu.rycus.tweetwear.twitter.client.ITwitterClient;

public class Preferences {

    public static final String PREFERENCES_NAME = "tweetwear";

    private static final String KEY_API_TOKEN = "apiToken";
    private static final String KEY_API_SECRET = "apiSecret";

    private static final String KEY_USER_TOKEN = "userToken.0";
    private static final String KEY_USER_SECRET = "userSecret.0";

    private static final String KEY_ACCESS_LEVEL = "accessLevel";

    private static final String KEY_USER_ID = "userId.0";
    private static final String KEY_USER_NAME = "userName.0";

    private static final String KEY_REFRESH_INTERVAL = "interval";

    private static final String KEY_LIST_SETTINGS = "listSettings.0";

    public static boolean saveRequestToken(final Context context, final Token token) {
        return saveToken(context, token, KEY_API_TOKEN, KEY_API_SECRET);
    }

    public static boolean saveUserToken(final Context context, final Token token) {
        return saveToken(context, token, KEY_USER_TOKEN, KEY_USER_SECRET);
    }

    private static boolean saveToken(final Context context, final Token token,
                                     final String tokenKey, final String secretKey) {
        final SharedPreferences preferences = getPreferences(context);
        return preferences.edit()
                .putString(tokenKey, token.getToken())
                .putString(secretKey, token.getSecret())
                .commit();
    }

    public static boolean saveAccessLevel(final Context context, final String level) {
        final SharedPreferences preferences = getPreferences(context);
        return preferences.edit()
                .putString(KEY_ACCESS_LEVEL, level)
                .commit();
    }

    public static boolean saveUserId(final Context context, final long id) {
        final SharedPreferences preferences = getPreferences(context);
        return preferences.edit()
                .putLong(KEY_USER_ID, id)
                .commit();
    }

    public static boolean saveUserName(final Context context, final String name) {
        final SharedPreferences preferences = getPreferences(context);
        return preferences.edit()
                .putString(KEY_USER_NAME, name)
                .commit();
    }

    public static boolean setRefreshInterval(final Context context, final long interval) {
        // the actual value has to be a string so it plays nice with PreferenceActivity
        final SharedPreferences preferences = getPreferences(context);
        return preferences.edit()
                .putString(KEY_REFRESH_INTERVAL, Long.toString(interval))
                .commit();
    }

    public static boolean saveListSettings(final Context context, final ListSettings settings) {
        final SharedPreferences preferences = getPreferences(context);
        return preferences.edit()
                .putString(KEY_LIST_SETTINGS, settings.serialize())
                .commit();
    }

    public static Token getRequestToken(final Context context) {
        return getToken(context, KEY_API_TOKEN, KEY_API_SECRET);
    }

    public static Token getUserToken(final Context context) {
        return getToken(context, KEY_USER_TOKEN, KEY_USER_SECRET);
    }

    private static Token getToken(final Context context,
                                  final String tokenKey, final String secretKey) {
        final SharedPreferences preferences = getPreferences(context);
        final String token = preferences.getString(tokenKey, null);
        final String secret = preferences.getString(secretKey, null);
        if (token != null && secret != null) {
            return new Token(token, secret);
        } else {
            return null;
        }
    }

    public static String getAccessLevel(final Context context) {
        final SharedPreferences preferences = getPreferences(context);
        return preferences.getString(KEY_ACCESS_LEVEL, null);
    }

    public static boolean hasDesiredAccessLevel(final Context context) {
        final String savedValue = getAccessLevel(context);
        return ITwitterClient.AccessLevel.desired().matches(savedValue);
    }

    public static long getUserId(final Context context) {
        final SharedPreferences preferences = getPreferences(context);
        return preferences.getLong(KEY_USER_ID, -1L);
    }

    public static String getUserName(final Context context) {
        final SharedPreferences preferences = getPreferences(context);
        return preferences.getString(KEY_USER_NAME, null);
    }

    public static long getRefreshInterval(final Context context) {
        // the actual value has to be a string so it plays nice with PreferenceActivity
        final SharedPreferences preferences = getPreferences(context);
        final String stringValue = preferences.getString(
                KEY_REFRESH_INTERVAL,
                Long.toString(AlarmManager.INTERVAL_HALF_HOUR));
        return Long.parseLong(stringValue);
    }

    public static ListSettings getListSettings(final Context context) {
        final SharedPreferences preferences = getPreferences(context);
        final String stringValue = preferences.getString(KEY_LIST_SETTINGS, null);
        if (stringValue != null) {
            return ListSettings.create(stringValue);
        } else {
            return new ListSettings();
        }
    }

    private static SharedPreferences getPreferences(final Context context) {
        return context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

}
