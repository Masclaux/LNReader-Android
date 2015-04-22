package com.erakk.lnreader.activity;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import android.util.Log;

import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.widget.Toast;

import com.erakk.lnreader.Constants;

import com.erakk.lnreader.LNReaderApplication;
import com.erakk.lnreader.R;
import com.erakk.lnreader.UIHelper;
import com.erakk.lnreader.callback.ICallbackEventData;
import com.erakk.lnreader.helper.DisplayNovelContentHtmlHelper;

import com.erakk.lnreader.helper.NonLeakingWebView;

import com.erakk.lnreader.helper.Util;
import com.erakk.lnreader.model.ImageModel;
import com.erakk.lnreader.model.NovelContentModel;
import com.erakk.lnreader.model.PageModel;
import com.erakk.lnreader.model.PageNovelContentModel;
import com.erakk.lnreader.parser.CommonParser;
import com.erakk.lnreader.task.AsyncTaskResult;
import com.erakk.lnreader.task.LoadImageTask;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.Calendar;

public class DisplayLightPageNovelContentActivity extends DisplayLightNovelContentActivity implements  View.OnTouchListener {
    private static final String TAG = DisplayLightPageNovelContentActivity.class.toString();

    private static final int MAX_CLICK_DURATION = 200;
    private static final float MIN_SWIPE_DISTANCE = 150;
    private static final float MAX_SWIPE_DURATION = 500;

    //Percentage of screen where tap left or right is active.
    protected static float TAP_ZONE_BOUND = 0.20f;

    //Vertical offset in pixel for the end of a page ( maxWith - PAGE_ENDING_OFFSET)
    protected static int PAGE_ENDING_OFFSET = 10;

    private PageNovelContentModel pageContent;

    private long startClickTime;

    private float startSwipeX = 0;

    private boolean requestNewChapter = false;

    private boolean requestPreviousPage = false;
    private boolean requestNexPage      = false;

    private int requestPosition = -1;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView.setOnTouchListener(this);
    }

    @Override
    public void setContent(NovelContentModel loadedContent) {
        pageContent = new PageNovelContentModel(loadedContent);
        Document doc = Jsoup.parse(loadedContent.getContent());

        this.images = CommonParser.parseImagesFromContentPage(doc);
        this.content = pageContent;

        pageContent.generateContent(doc);
        try {
            PageModel pageModel = content.getPageModel();

            if (content.getLastUpdate().getTime() < pageModel.getLastUpdate().getTime())
                Toast.makeText(this, getResources().getString(R.string.content_may_updated, content.getLastUpdate().toString(), pageModel.getLastUpdate().toString()), Toast.LENGTH_LONG).show();

            setWebViewSettings();

            int pIndex = getIntent().getIntExtra(Constants.EXTRA_P_INDEX, -1);
            requestPosition = pIndex > 0 ? pIndex : content.getLastYScroll();

            if (content.getLastZoom() > 0) {
                currentScale = (float) content.getLastZoom();
            }

            //previous chapter
            if (requestNewChapter) {
                requestNewChapter = false;
                goToPage(pageContent.getPageNumber() - 1);
            } else {
                goToPage(pageContent.getCurrentPage());
            }

            setChapterTitle(pageModel);

            buildTOCMenu(pageModel);
            buildBookmarkMenu();

            invalidateOptionsMenu();

            Log.d(TAG, "Loaded: " + content.getPage());

            Intent currIntent = this.getIntent();
            currIntent.putExtra(Constants.EXTRA_PAGE, content.getPage());
            currIntent.putExtra(Constants.EXTRA_PAGE_IS_EXTERNAL, false);
        } catch (Exception e) {
            Log.e(TAG, "Cannot load content.", e);
        }
    }

    /**
     * Prepare content for web view
     *
     * @param content  page content
     */
    private void prepareHtml( String content )
    {
        super.setWebViewSettings();

        webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        webView.setScrollbarFadingEnabled(false);
        webView.getSettings().setBuiltInZoomControls(false);

        final String baseUrl = UIHelper.getBaseUrl(this);
        final String failURL = NonLeakingWebView.PREFIX_PAGEMODEL + pageContent.getPage();
        final String html = "<html><head>" +
                DisplayNovelContentHtmlHelper.getCSSSheet() +
                DisplayNovelContentHtmlHelper.getViewPortMeta(false) +
                DisplayNovelContentHtmlHelper.prepareJavaScript(requestPosition, this.content.getBookmarks(), false )+//getBookmarkPreferences() ) +
                "</head><body onclick='toogleHighlight(this, event);' onload='setup(); initGesture(" + currentScale + ");'>"+
                content +
                "<p align='right'>" + pageContent.getCurrentPageNumber() + "</p>" +
                "</body></html>";

        webView.loadDataWithBaseURL(baseUrl, html, "text/html", "utf-8", failURL);

        requestPosition = 0;
    }

    /**
     * Prepare image content for web view
     */
    private void prepareImage() {
        int imageIndex = Integer.parseInt(pageContent.getPageContent());
        if (images.size() > imageIndex) {
            LoadImageTask imageTask = new LoadImageTask(images.get(imageIndex), false, this);
            String key = TAG + ":" + "";
            boolean isAdded = LNReaderApplication.getInstance().addTask(key, imageTask);
            if (isAdded) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                    imageTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                else
                    imageTask.execute();
            } else {
                LoadImageTask tempTask = (LoadImageTask) LNReaderApplication.getInstance().getTask(key);
                if (tempTask != null) {
                    imageTask = tempTask;
                    imageTask.callback = this;
                }
                toggleProgressBar(true);
            }
        }
    }

    @Override
    public void onCompleteCallback(ICallbackEventData message, AsyncTaskResult<?> result) {
        if (result.getResultType() == ImageModel.class)
        {
            webView.getSettings().setBuiltInZoomControls(true);

            ImageModel imageModel = (ImageModel) result.getResult();

            String imageUrl = "file:///" + Util.sanitizeFilename(imageModel.getPath());
            imageUrl = imageUrl.replace("file:////", "file:///");

            String html = "<html><head>" +
                    DisplayNovelContentHtmlHelper.getCSSSheet() +
                    DisplayNovelContentHtmlHelper.getViewPortMeta(true) +
                    "</head><body onload='setup();'>" +
                    "<center><img src=\"" + imageUrl + "\" style=\"max-width: 100%; width:auto; height: auto; display: inline\"></center>" +
                    "</body></html>";

            webView.loadDataWithBaseURL("file://", html, "text/html", "utf-8", null);
        }

        super.onCompleteCallback(message, result);
    }

    /**
     * Go to previous page or chapter
     */
    public void previousPage()
    {
        if (pageContent.isFirstPage())
        {
            requestNewChapter = true;
            previousChapter();
        }
        else
        {
            requestPreviousPage = true;

            String content = pageContent.previousPage();
            if (!pageContent.isImage())
            {
                saveCurrentScale();
                prepareHtml(content);

            }
            else
            {
                prepareImage();
            }
        }
    }

    /**
     * Go to next page or chapter
     */
    public void nextPage()
    {
        goTop(webView); //here go to new page.

        if (pageContent.isLastPage()) {
            nextChapter();
        } else
        {

            requestNexPage = true;

            String content = pageContent.nextPage();
            if (!pageContent.isImage())
            {
                saveCurrentScale();
                prepareHtml(content);

            } else {
                prepareImage();
            }
        }
    }

    /**
     * Got to the page
     */
    public void goToPage(int page)
    {
        goTop(webView); //here go to new page.

        pageContent.goToPage(page);

        String content = pageContent.getPageContent();
        if (!pageContent.isImage()) {
            prepareHtml(content);
        } else {
            prepareImage();
        }
    }

    /**
     * Simulate Click with touch event
     *
     * @param xPos x click pos
     */
    public void onContentClick(float xPos)
    {
        double width     = webView.getWidth();
        double leftArea  = width * TAP_ZONE_BOUND;
        double rightArea = width - leftArea;

        boolean isLeftClick = xPos < leftArea;
        boolean isRightClick = xPos > rightArea;

        int scrollSize = UIHelper.getIntFromPreferences(Constants.PREF_SCROLL_SIZE, 5) * 300;

        int height = (int) Math.floor(webView.getContentHeight() * webView.getScale() - PAGE_ENDING_OFFSET); // scale always at 1.0 except when an image is displayed
        int webViewHeight = webView.getMeasuredHeight();

        if (webView.getScrollY() + webViewHeight >= height && isRightClick) //end of page
        {
            nextPage();
        }
        else if (webView.getScrollY() == 0 && isLeftClick)//start of page
        {
            previousPage();
        }
        else //scroll
        {
            if (isLeftClick) //left
            {
                webView.flingScroll(0, -scrollSize);
            } else if (isRightClick)//right
            {
                webView.flingScroll(0, +scrollSize);
            }
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent)
    {
        switch (motionEvent.getAction())
        {
            case MotionEvent.ACTION_DOWN:
            {
                startClickTime = Calendar.getInstance().getTimeInMillis();
                startSwipeX    = motionEvent.getX();
                break;
            }
            case MotionEvent.ACTION_UP:
            {
                long clickDuration = Calendar.getInstance().getTimeInMillis() - startClickTime;
                float deltaX = motionEvent.getX() - startSwipeX;
                if (clickDuration < MAX_CLICK_DURATION)
                {
                    onContentClick(motionEvent.getX());
                }
                else if (Math.abs(deltaX) > MIN_SWIPE_DISTANCE && clickDuration < MAX_SWIPE_DURATION) //swipe
                {
                    //on swipe
                    if (motionEvent.getX() < startSwipeX)//right to left
                    {
                        nextPage();
                    }
                    else
                    {
                        previousPage();
                    }
                }//else probably long touch
            }
        }
        return false; // no handle
    }

    private void saveCurrentScale()
    {
        currentScale = this.client.getInternalScale();
    }


    public void notifyLoadComplete()
    {
       if( requestNexPage )
       {
           goTop(webView); //here go to top
       }
       else if( requestPreviousPage )
       {
           webView.postDelayed(new Runnable()
           {
               @Override
               public void run()
               {
                   try
                   {
                       Log.d(TAG, "go to bottom" );
                       webView.loadUrl("javascript:goToBottom()");
                   }
                   catch ( NullPointerException ex)
                   {
                       Log.i(TAG, "Failed to load the content");
                   }
               }
           }, UIHelper.getIntFromPreferences(Constants.PREF_KITKAT_WEBVIEW_FIX_DELAY, 500) + 100);
       }
       else
       {
           super.notifyLoadComplete();
       }

        requestNexPage      = false;
        requestPreviousPage = false;
    }
}