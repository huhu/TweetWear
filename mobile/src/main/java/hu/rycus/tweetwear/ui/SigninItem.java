package hu.rycus.tweetwear.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import hu.rycus.tweetwear.R;
import hu.rycus.tweetwear.twitter.TwitterFactory;

public class SigninItem extends SettingsItem<Void> {

    @Override
    public Void getItem() {
        return null;
    }

    @Override
    protected View createView(final Context context, final ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.item_signin, parent, false);
    }

    @Override
    protected void onClick(final Context context) {
        TwitterFactory.createClient().authorize(context);
    }

}
