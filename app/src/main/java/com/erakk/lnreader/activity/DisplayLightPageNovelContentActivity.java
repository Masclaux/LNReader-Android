package com.erakk.lnreader.activity;

import android.content.Intent;
import android.os.Bundle;

import android.util.Log;

import android.view.MotionEvent;
import android.view.View;

import android.widget.Toast;
import com.erakk.lnreader.Constants;

import com.erakk.lnreader.LNReaderApplication;
import com.erakk.lnreader.R;
import com.erakk.lnreader.UIHelper;
import com.erakk.lnreader.helper.DisplayNovelContentHtmlHelper;

import com.erakk.lnreader.helper.NonLeakingWebView;

import com.erakk.lnreader.model.NovelContentModel;
import com.erakk.lnreader.model.PageModel;
import com.erakk.lnreader.model.PageNovelContentModel;
import com.erakk.lnreader.parser.CommonParser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.Calendar;


public class DisplayLightPageNovelContentActivity extends DisplayLightNovelContentActivity implements  View.OnTouchListener
{
    private static final String TAG = DisplayLightPageNovelContentActivity.class.toString();

    private PageNovelContentModel pageContent;

    private static final int MAX_CLICK_DURATION = 200;

    private static final float MIN_SWIPE_DISTANCE = 150;

    private long startClickTime;

    private float startSwipeX = 0;

    private boolean requestNewChapter = false;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        webView.setOnTouchListener(this);
    }

    @Override
    public void setContent(NovelContentModel loadedContent)
    {

        pageContent = new PageNovelContentModel(loadedContent);
        pageContent.generateContent();

        this.content = pageContent;


        Document imgDoc = Jsoup.parse(content.getContent());
        pageContent.setImages( CommonParser.getAllImagesFromContent(imgDoc, UIHelper.getBaseUrl(LNReaderApplication.getInstance().getApplicationContext())) );

        try
        {
            PageModel pageModel = content.getPageModel();

            if (content.getLastUpdate().getTime() < pageModel.getLastUpdate().getTime())
                Toast.makeText(this, getResources().getString(R.string.content_may_updated, content.getLastUpdate().toString(), pageModel.getLastUpdate().toString()), Toast.LENGTH_LONG).show();

            // load the contents here
            final NonLeakingWebView wv = (NonLeakingWebView) findViewById(R.id.webViewContent);
            setWebViewSettings();

            int lastPos = content.getLastYScroll();
            int pIndex = getIntent().getIntExtra(Constants.EXTRA_P_INDEX, -1);
            if (pIndex > 0)
                lastPos = pIndex;

            if (content.getLastZoom() > 0) {
                wv.setInitialScale((int) (content.getLastZoom() * 100));
            } else {
                wv.setInitialScale(100);
            }

            String html ="<html><head>"+
            DisplayNovelContentHtmlHelper.getCSSSheet()+
            DisplayNovelContentHtmlHelper.getViewPortMeta()+
            DisplayNovelContentHtmlHelper.prepareJavaScript(lastPos, content.getBookmarks(), false )+
            "</head><body onload='setup();'>"+
            pageContent.getPageContent();

            //Add to DisplayLightPageNovel.
            html+= "<p align='right'>"+ pageContent.getCurrentPageNumber() +"</p>";
            html+= "</body></html>";

            wv.loadDataWithBaseURL(UIHelper.getBaseUrl(this), html, "text/html", "utf-8", NonLeakingWebView.PREFIX_PAGEMODEL + content.getPage());
            setChapterTitle(pageModel);
            Log.d(TAG, "Load Content: " + content.getLastXScroll() + " " + content.getLastYScroll() + " " + content.getLastZoom());

            buildTOCMenu(pageModel);
            buildBookmarkMenu();

            invalidateOptionsMenu();

            Log.d(TAG, "Loaded: " + content.getPage());

            Intent currIntent = this.getIntent();
            currIntent.putExtra(Constants.EXTRA_PAGE, content.getPage());
            currIntent.putExtra(Constants.EXTRA_PAGE_IS_EXTERNAL, false);

            //previous chapter
            if( requestNewChapter == true )
            {
                requestNewChapter = false;
                goToPage( pageContent.getPageNumber() -1 );
            }
        }
        catch (Exception e)
        {
            Log.e(TAG, "Cannot load content.", e);
        }
    }

    /**
     * Prepare content for web view
     * @param content page content
     */
    private void prepareHtml(String content)
    {
        final NonLeakingWebView wv = (NonLeakingWebView) findViewById(R.id.webViewContent);

        String html = "<html><head>" +
                DisplayNovelContentHtmlHelper.getCSSSheet()+
                DisplayNovelContentHtmlHelper.getViewPortMeta()+
                "</head><body onload='setup();'>"+
                content+
                "<p align='right'>"+ pageContent.getCurrentPageNumber() +"</p>"+
                "</body></html>";

        wv.loadDataWithBaseURL(UIHelper.getBaseUrl(this), html, "text/html", "utf-8", NonLeakingWebView.PREFIX_PAGEMODEL + pageContent.getPage());
    }

    /**
     * Prepare image content for web view
     * @param content image content
     */
    private void prepareImage(String content)
    {
       final NonLeakingWebView wv = (NonLeakingWebView) findViewById(R.id.webViewContent);

        String html = "<html><head>" +
                DisplayNovelContentHtmlHelper.getViewPortMeta()+
                "</head><body>"+
                content+
                "</body></html>";

        wv.loadDataWithBaseURL(UIHelper.getBaseUrl(this), html, "text/html", "utf-8", NonLeakingWebView.PREFIX_PAGEMODEL + pageContent.getPage());
    }

    /**
     * Go to previous page or chapter
     */
    public void previousPage()
    {
       goBottom(webView); //here go to new page.

        if( pageContent.isFirstPage() )
        {
            requestNewChapter = true;
            previousChapter();
        }
        else
        {
            String content = pageContent.previousPage();
            if (!pageContent.isImage()) {
                prepareHtml(content);
            } else {
                prepareImage(content);
            }
        }
    }

    /**
     * Go to next page or chapter
     */
    public void nextPage()
    {
        goTop(webView); //here go to new page.

        if( pageContent.isLastPage() )
        {
            nextChapter();
        }
        else {
            String content = pageContent.nextPage();
            if (!pageContent.isImage()) {
                prepareHtml(content);
            } else {
                prepareImage(content);
            }
        }
    }

    /**
     * Got to the page
     * @return the content of the new page
     */
    public void goToPage(int page)
    {
        goTop(webView); //here go to new page.

        pageContent.goToPage(page);

        String content = pageContent.getPageContent();
        if (!pageContent.isImage())
        {
            prepareHtml(content);
        }
        else
        {
            prepareImage(content);
        }
    }

    /**
     * Simulate Click with touch event
     * @param xPos x click pos
     */
    public void onContentClick(float xPos)
    {
        double middle  = webView.getWidth() * 0.5;

        boolean isLeftClick = xPos < middle;

        int yContentPos = webView.getScrollY();

        float density =  webView.getResources().getDisplayMetrics().density;
        int maxY = (int) ((webView.getContentHeight() * density) - webView.getHeight());
        maxY -= 10; // TODO write a useful comment

        if( yContentPos >= maxY && !isLeftClick) //end of page
        {
            nextPage();
        }
        else if( yContentPos == 0 && isLeftClick )//start of page
        {
            previousPage();
        }
        else //scroll
        {
            int scrollSize = UIHelper.getIntFromPreferences(Constants.PREF_SCROLL_SIZE, 5) * 300;
            if (isLeftClick) //left
            {
                webView.flingScroll(0, -scrollSize);
            }
            else //right
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
                else if(Math.abs(deltaX) > MIN_SWIPE_DISTANCE) //swipe
                {
                  //on swipe
                   if( motionEvent.getX()  < startSwipeX )//right to left
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
}
