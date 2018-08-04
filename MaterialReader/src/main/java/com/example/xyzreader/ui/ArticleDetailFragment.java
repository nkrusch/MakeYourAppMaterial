package com.example.xyzreader.ui;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    public static final String EXPANDED_STATE = "text_state";
    public static final String ARG_ITEM_ID = "item_id";
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    private SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy");
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2, 1, 1);

    private String fullBodyText;
    private Cursor mCursor;
    private View mRootView;
    private long mItemId;
    private int bodyTextVisibleLength = 0;
    private int scrollY;

    public ArticleDetailFragment() {
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
        setUserVisibleHint(false);

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
        if (savedInstanceState != null && savedInstanceState.containsKey(EXPANDED_STATE))
            bodyTextVisibleLength = savedInstanceState.getInt(EXPANDED_STATE);
        if (savedInstanceState != null && savedInstanceState.containsKey(EXTRA_SCROLLPOS))
            scrollY = savedInstanceState.getInt(EXTRA_SCROLLPOS);
        return mRootView;
    }

    /**
     * Get formatted book publish date
     */
    private String parsePublishedDate(String date) {
        Date publishedDate;
        try {
            publishedDate = dateFormat.parse(date);
        } catch (ParseException ex) {
            publishedDate = new Date();
        }
        return publishedDate.before(START_OF_EPOCH.getTime()) ?
                outputFormat.format(publishedDate) :
                DateUtils.getRelativeTimeSpanString(
                        publishedDate.getTime(),
                        System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_ALL).toString();
    }

    /**
     * set view texts and start loading book image
     */
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
        setBodyText();
    }

    /**
     * load book image then reveal the layout once image load is done
     */
    private void loadImage(String photoUrl) {
        ImageLoaderHelper.getInstance(getActivity()).getImageLoader().get(photoUrl,
                new ImageLoader.ImageListener() {
                    @Override
                    public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {
                        Bitmap bitmap = imageContainer.getBitmap();
                        ImageView photo = (ImageView) mRootView.findViewById(R.id.photo);
                        if (bitmap != null)
                            photo.setImageBitmap(imageContainer.getBitmap());
                        else
                            photo.setBackgroundColor(getResources()
                                    .getColor(R.color.photo_placeholder));
                        mRootView.animate().setDuration(300).alpha(1);
                    }

                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        mRootView.setAlpha(1);
                    }
                });
    }

    /**
     * handle toolbar up action
     */
    private View.OnClickListener upAction() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getActivity().finish();
            }

        };
    }

    /**
     * update body text; this is a hacky way to page the content
     * because it takes forever to load the full text into textview
     */
    private void setBodyText() {
        if (mRootView == null) return;
        TextView tv_bodytext = (TextView) mRootView.findViewById(R.id.article_body);
        final Button expando_btn = (Button) mRootView.findViewById(R.id.expand_body);
        final int len = getResources().getInteger(R.integer.detail_snippet_len);
        if (bodyTextVisibleLength == 0) bodyTextVisibleLength = len;

        if (bodyTextVisibleLength >= fullBodyText.length()) {
            tv_bodytext.setText(fullBodyText);
            expando_btn.setOnClickListener(null);
            expando_btn.setVisibility(GONE);

        } else {
            String bodyText = fullBodyText.substring(0, bodyTextVisibleLength);
            int cut_at = bodyText.lastIndexOf(".") + 1;
            bodyText = bodyText.substring(0, cut_at).trim();
            bodyTextVisibleLength = cut_at;
            tv_bodytext.setText(bodyText);

            expando_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    bodyTextVisibleLength += len;
                    setBodyText();
                }
            });
            expando_btn.setVisibility(VISIBLE);
        }
        restoreScrollPosition();
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

    private final String EXTRA_SCROLLPOS = "scroll_offset";

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(EXPANDED_STATE, bodyTextVisibleLength);
        getScrollY();
        outState.putInt(EXTRA_SCROLLPOS, scrollY);
    }

    @Override
    public void onPause() {
        super.onPause();
        getScrollY();
    }

    private void getScrollY() {
        Object s = mRootView.findViewById(R.id.scrollview);
        if (s instanceof NestedScrollView) {
            scrollY = ((NestedScrollView) s).getScrollY();
        }
        if (s instanceof ObservableScrollView) {
            scrollY = ((ObservableScrollView) s).getScrollY();
        }
    }

    private void restoreScrollPosition() {
        if (scrollY < 1) return;
        Object s = mRootView.findViewById(R.id.scrollview);
        Runnable r = new Runnable() {
            public void run() {
                mRootView.findViewById(R.id.scrollview).scrollTo(0, scrollY);
            }
        };
        if (s instanceof NestedScrollView) {
            ((NestedScrollView) s).post(r);
        }
        if (s instanceof ObservableScrollView) {
            ((ObservableScrollView) s).post(r);
        }
    }
}