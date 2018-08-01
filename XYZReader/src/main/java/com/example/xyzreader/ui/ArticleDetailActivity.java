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
import android.support.v7.widget.Toolbar;
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
    private FloatingActionButton mFab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_detail);
        getLoaderManager().initLoader(0, null, this);
        setupPager();
        if (savedInstanceState == null &&
                getIntent() != null &&
                getIntent().getData() != null) {
            mStartId = ItemsContract.Items.getItemId(getIntent().getData());
        }
        mFab = (FloatingActionButton) findViewById(R.id.share_fab);
        updateFab();
    }

    private void setupPager() {
        mPagerAdapter = new MyPagerAdapter(getFragmentManager());
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mPagerAdapter);
        mPager.setPageMargin((int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics()));
        mPager.setPageMarginDrawable(new ColorDrawable(0x22000000));
        mPager.addOnPageChangeListener(pagerChange());
        mPager.setPageTransformer(true, new CustomPageTransformer());
    }

    private ViewPager.OnPageChangeListener pagerChange() {
        return new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                if (mCursor != null) mCursor.moveToPosition(position);
                updateFab();
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
    private void updateFab() {
        setFabAction();
        AlphaAnimation animation1 = new AlphaAnimation(0, 1);
        animation1.setDuration(400);
        animation1.setStartOffset(300);
        animation1.setFillAfter(true);
        mFab.startAnimation(animation1);
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

    private void setFabAction() {
        final Activity activity = this;
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(Intent.createChooser(
                        ShareCompat.IntentBuilder.from(activity)
                                .setType("text/plain")
                                .setText("Some sample text")
                                .getIntent(), getString(R.string.action_share)));
            }
        });
    }

    public class CustomPageTransformer implements ViewPager.PageTransformer {
        @Override
        public void transformPage(View page, float position) {
            if (position >= -1 && position <= 1) {
                page.findViewById(R.id.photo).setTranslationX(-position * page.getWidth());
                Toolbar tb = (Toolbar) page.findViewById(R.id.toolbar);
                tb.setTranslationX(-position * page.getWidth());
            } else {
                page.setAlpha(1);
            }
        }
    }
}