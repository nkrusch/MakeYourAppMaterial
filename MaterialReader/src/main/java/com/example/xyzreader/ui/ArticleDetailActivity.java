package com.example.xyzreader.ui;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.app.ShareCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.AlphaAnimation;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;

/**
 * An activity representing a single Article detail screen, letting you swipe between articles.
 */
public class ArticleDetailActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private Cursor mCursor;
    private long mStartId;
    private ViewPager mPager;
    private MyPagerAdapter mPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_detail);

        getLoaderManager().initLoader(0, null, this);

        if (savedInstanceState == null &&
                getIntent() != null && getIntent().getData() != null)
            mStartId = ItemsContract.Items.getItemId(getIntent().getData());

        setupPager();
    }

    /**
     * Initialize viewpager that provides different books
     */
    private void setupPager() {

        // viewpager element
        mPager = (ViewPager) findViewById(R.id.pager);
        // create adapter
        mPagerAdapter = new MyPagerAdapter(getFragmentManager());
        mPager.setAdapter(mPagerAdapter);

        // draw borders around pages
        mPager.setPageMargin((int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics()));
        mPager.setPageMarginDrawable(new ColorDrawable(0x22000000));

        // bind handler when viewpager page changes
        mPager.addOnPageChangeListener(pagerChange());

        // override the transition animation
        mPager.setPageTransformer(true, new CustomPageTransformer());
    }

    /**
     * Handler for when viewpager page changes
     */
    private ViewPager.OnPageChangeListener pagerChange() {
        return new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                if (mCursor != null) mCursor.moveToPosition(position);
                updateFab(mCursor.getString(ArticleLoader.Query.TITLE));
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        };
    }

    /**
     * Do. When switching between tabs, FABs disappear and then reappear.
     * ref: https://material.io/design/components/buttons-floating-action-button.html#behavior
     */
    private void updateFab(final String shareText) {
        FloatingActionButton mFab = (FloatingActionButton) findViewById(R.id.share_fab);
        mFab.setOnClickListener(fabClickAction(shareText));
        AlphaAnimation animation1 = new AlphaAnimation(0, 1);
        animation1.setDuration(400);
        animation1.setStartOffset(300);
        animation1.setFillAfter(true);
        mFab.startAnimation(animation1);
    }

    /**
     * Handle FAB button click
     */
    private View.OnClickListener fabClickAction(final String shareText) {
        final Activity activity = this;
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(Intent.createChooser(
                        ShareCompat.IntentBuilder.from(activity)
                                .setType("text/plain")
                                .setText(shareText)
                                .getIntent(), getString(R.string.action_share)));
            }
        };
    }

    /**
     * Change the default page transition of the viewpager
     * This transition keeps the background and toolbar static while other
     * content transitions normally.
     */
    public class CustomPageTransformer implements ViewPager.PageTransformer {
        @Override
        public void transformPage(View page, float position) {
            if (position >= -1 && position <= 1) {
                float static_position = -position * page.getWidth();
                page.findViewById(R.id.photo).setTranslationX(static_position);
                page.findViewById(R.id.toolbar).setTranslationX(static_position);
            } else {
                page.setAlpha(1);
            }
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mCursor = cursor;
        mPagerAdapter.notifyDataSetChanged();

        // Select the start ID
        if (mStartId > 0) {
            mCursor.moveToFirst();
            // TODO: optimize
            while (!mCursor.isAfterLast()) {
                if (mCursor.getLong(ArticleLoader.Query._ID) == mStartId) {
                    final int position = mCursor.getPosition();
                    mPager.setCurrentItem(position, false);
                    updateFab(mCursor.getString(ArticleLoader.Query.TITLE));
                    break;
                }
                mCursor.moveToNext();
            }
            mStartId = 0;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        mPagerAdapter.notifyDataSetChanged();
    }

    private class MyPagerAdapter extends FragmentStatePagerAdapter {
        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            mCursor.moveToPosition(position);
            return ArticleDetailFragment.newInstance(mCursor.getLong(ArticleLoader.Query._ID));
        }

        @Override
        public int getCount() {
            return (mCursor != null) ? mCursor.getCount() : 0;
        }
    }
}