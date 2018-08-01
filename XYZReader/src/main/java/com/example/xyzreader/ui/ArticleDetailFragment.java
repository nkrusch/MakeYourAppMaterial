package com.example.xyzreader.ui;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    public static final String ARG_ITEM_ID = "item_id";
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    private SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy");
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2, 1, 1);

    private String fullBodyText;
    private Cursor mCursor;
    private View mRootView;
    private long mItemId;

    public ArticleDetailFragment() {
    }

    private void setText() {
        ((TextView) mRootView.findViewById(R.id.article_body)).setText(fullBodyText);
    }

    public static ArticleDetailFragment newInstance(long itemId) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID))
            mItemId = getArguments().getLong(ARG_ITEM_ID);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);
        mRootView.setAlpha(0);
        return mRootView;
    }

    private String parsePublishedDate(String date) {
        Date publishedDate;
        try {
            publishedDate= dateFormat.parse(date);
        } catch (ParseException ex) {
            publishedDate= new Date();
        }
        return publishedDate.before(START_OF_EPOCH.getTime()) ?
                outputFormat.format(publishedDate) :
                DateUtils.getRelativeTimeSpanString(
                        publishedDate.getTime(),
                        System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_ALL).toString();
    }

    private void setupViews() {
        if (mRootView == null || mCursor == null) return;

        String title = mCursor.getString(ArticleLoader.Query.TITLE);
        String author = mCursor.getString(ArticleLoader.Query.AUTHOR);
        fullBodyText = mCursor.getString(ArticleLoader.Query.BODY);
        String photoUrl = mCursor.getString(ArticleLoader.Query.PHOTO_URL);
        String date = parsePublishedDate(mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE));
        loadImage(photoUrl);

        Toolbar tb = (Toolbar) mRootView.findViewById(R.id.toolbar);
        tb.setNavigationIcon(R.drawable.ic_arrow_back);
        tb.setNavigationOnClickListener(upAction());

        TextView tv_articleTitle = (TextView) mRootView.findViewById(R.id.article_title);
        if (tv_articleTitle != null) tv_articleTitle.setText(title);
        else tb.setTitle(title);

        SpannableString tmp = new SpannableString(author + " – " + date);
        tmp.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                0, author.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        TextView tv_articleByLine = (TextView) mRootView.findViewById(R.id.article_byline);
        if (tv_articleByLine != null)
            tv_articleByLine.setText(tmp);

        int len = getResources().getInteger(R.integer.detail_snippet_len);
        String bodyText = fullBodyText.substring(0, Math.min(fullBodyText.length(), len));
        ((TextView) mRootView.findViewById(R.id.article_body)).setText(bodyText);
        mRootView.setAlpha(1);
    }

    private void loadImage(String photoUrl) {
        ImageLoaderHelper.getInstance(getActivity()).getImageLoader().get(photoUrl,
                new ImageLoader.ImageListener() {
                    @Override
                    public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {
                        Bitmap bitmap = imageContainer.getBitmap();
                        if (bitmap != null)
                            ((ImageView) mRootView.findViewById(R.id.photo))
                                    .setImageBitmap(imageContainer.getBitmap());
                    }

                    @Override
                    public void onErrorResponse(VolleyError volleyError) {

                    }
                });
    }

    private View.OnClickListener upAction() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((AppCompatActivity) getActivity()).onSupportNavigateUp();
            }
        };
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (!isAdded()) {
            if (cursor != null) cursor.close();
            return;
        }
        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            mCursor.close();
            mCursor = null;
        } else setupViews();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
    }
}