package hu.rycus.tweetwear.ui.ril;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.ListView;

import hu.rycus.tweetwear.R;
import hu.rycus.tweetwear.common.model.Tweet;
import hu.rycus.tweetwear.common.util.Constants;
import hu.rycus.tweetwear.ril.ReadItLater;
import hu.rycus.tweetwear.ril.SavedPage;

public class ListFragment extends android.app.ListFragment {

    private static final String TAG = "ril." + ListFragment.class.getSimpleName();

    private static final String KEY_PREFIX = ListFragment.class.getCanonicalName();
    private static final String KEY_STATE_FIRST_POS = KEY_PREFIX + ".firstPos";
    private static final String KEY_STATE_TOP = KEY_PREFIX + ".top";

    private static final long ANIMATION_DURATION = 500;
    private static final long ANIMATION_OFFSET = 150;

    private Activity activity;
    private ListAdapter listAdapter;

    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);
        this.activity = activity;
    }

    @Override
    public void onResume() {
        super.onResume();

        final IntentFilter filter = new IntentFilter(Constants.ACTION_BROADCAST_READ_IT_LATER);
        LocalBroadcastManager.getInstance(activity).registerReceiver(readLaterBroadcastReceiver, filter);
    }

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(activity).unregisterReceiver(readLaterBroadcastReceiver);

        super.onPause();
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        listAdapter = new ListAdapter();
        setListAdapter(listAdapter);
    }

    @Override
    @SuppressLint("InflateParams")
    public View onCreateView(@NonNull final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_read_it_later, null);
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final ListView listView = getListView();
        listView.setDividerHeight(0);

        final SwipeDismissListViewTouchListener swipeDismissListener;
        swipeDismissListener = createSwipeDismissListener(listView);
        listView.setOnTouchListener(swipeDismissListener);
        listView.setOnScrollListener(swipeDismissListener.makeScrollListener());

        if (!listAdapter.onRestoreInstanceState(savedInstanceState)) {
            listAdapter.loadContents(activity);
        }

        if (savedInstanceState != null) {
            final int firstPosition = savedInstanceState.getInt(KEY_STATE_FIRST_POS, 0);
            final int top = savedInstanceState.getInt(KEY_STATE_TOP, 0);
            listView.setSelectionFromTop(firstPosition, top);
        }
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);

        listAdapter.onSaveInstanceState(outState);

        final ListView listView = getListView();
        final int firstPosition = listView.getFirstVisiblePosition();
        final View firstView = listView.getChildAt(firstPosition);
        final int top = firstView != null ? firstView.getTop() : 0;

        outState.putInt(KEY_STATE_FIRST_POS, firstPosition);
        outState.putInt(KEY_STATE_TOP, top);
    }

    public void animateActivityFinishing(final Activity activity) {
        Log.d(TAG, "Animating activity finish");

        final ListView listView = getListView();
        final int visibleChildCount = listView.getChildCount();

        Animation lastAnimation = null;

        for (int index = 0; index < visibleChildCount; index++) {
            final View view = listView.getChildAt(index);
            final Animation animation = createAnimation(index);

            view.startAnimation(animation);

            lastAnimation = animation;
        }

        if (lastAnimation != null) {
            lastAnimation.setAnimationListener(createFinishingListener(activity));
        }
    }

    private Animation createAnimation(final int offset) {
        final TranslateAnimation slideOutAnimation = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 1f,
                Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f);
        slideOutAnimation.setDuration(ANIMATION_DURATION);
        slideOutAnimation.setInterpolator(new AccelerateInterpolator());

        final AlphaAnimation fadeOutAnimation = new AlphaAnimation(1f, 0f);
        fadeOutAnimation.setDuration(ANIMATION_DURATION);
        fadeOutAnimation.setInterpolator(new LinearInterpolator());

        final AnimationSet animationSet = new AnimationSet(false);
        animationSet.addAnimation(slideOutAnimation);
        animationSet.addAnimation(fadeOutAnimation);
        animationSet.setStartOffset(offset * ANIMATION_OFFSET);
        animationSet.setFillAfter(true);
        return animationSet;
    }

    private Animation.AnimationListener createFinishingListener(final Activity activity) {
        return new Animation.AnimationListener() {
            @Override
            public void onAnimationEnd(final Animation animation) {
                Log.d(TAG, "Finishing parent activity");
                activity.finish();
            }

            @Override
            public void onAnimationStart(final Animation animation) { }
            @Override
            public void onAnimationRepeat(final Animation animation) { }
        };
    }

    private SwipeDismissListViewTouchListener createSwipeDismissListener(final ListView listView) {
        return new SwipeDismissListViewTouchListener(listView, createDismissCallback());
    }

    private SwipeDismissListViewTouchListener.DismissCallbacks createDismissCallback() {
        return new SwipeDismissListViewTouchListener.DismissCallbacks() {
            @Override
            public boolean canDismiss(final int position) {
                return true;
            }

            @Override
            public void onDismiss(final ListView listView, final int[] reverseSortedPositions) {
                if (reverseSortedPositions == null || reverseSortedPositions.length < 1) {
                    return;
                }

                for (final int position : reverseSortedPositions) {
                    final SavedPage page = (SavedPage) listAdapter.getItem(position);
                    final long pageId = page.getId();
                    final Tweet tweet = page.getTweet();

                    listAdapter.deletePage(position);

                    AsyncTask.execute(new Runnable() {
                        @Override
                        public void run() {
                            ReadItLater.delete(activity, pageId, tweet);
                        }
                    });
                }

                if (listAdapter.getCount() == 0) {
                    activity.finish();
                }
            }
        };
    }

    private final BroadcastReceiver readLaterBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (Constants.ACTION_BROADCAST_READ_IT_LATER.equals(intent.getAction())) {
                listAdapter.loadContents(context);
            }
        }
    };

}
