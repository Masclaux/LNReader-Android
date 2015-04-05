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

import com.erakk.lnreader.model.ImageModel;
import com.erakk.lnreader.model.NovelContentModel;
import com.erakk.lnreader.model.PageModel;
import com.erakk.lnreader.model.PageNovelContentModel;
import com.erakk.lnreader.parser.CommonParser;
import com.erakk.lnreader.task.LoadImageTask;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;


public class DisplayLightPageNovelContentActivity extends DisplayLightNovelContentActivity implements  View.OnTouchListener
{
    private static final String TAG = DisplayLightPageNovelContentActivity.class.toString();

    private PageNovelContentModel pageContent;

    //Load image TODO im sure there are an another method to get image properly
    private LoadImageTask task;


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        webView.setOnTouchListener(this);
    }


  //  public void setContent(PageNovelContentModel loadedContent){}


    @Override
    public void setContent(NovelContentModel loadedContent)
    {
        super.setContent(loadedContent);

        pageContent = new PageNovelContentModel();
        pageContent.setContent(content.getContent());

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

            StringBuilder html = new StringBuilder();
            html.append("<html><head>");
            html.append(DisplayNovelContentHtmlHelper.getCSSSheet());
            html.append(DisplayNovelContentHtmlHelper.getViewPortMeta());
            html.append(DisplayNovelContentHtmlHelper.prepareJavaScript(lastPos, content.getBookmarks(), getBookmarkPreferences()));
            html.append("</head><body onclick='toogleHighlight(this, event);' onload='setup();'>");
            html.append(pageContent.getContent());

            //Add to DisplayLightPageNovel.
            html.append( "<p align='right'>"+ pageContent.getCurrentPageNumber() +"</p>");


            html.append("</body></html>");

            wv.loadDataWithBaseURL(UIHelper.getBaseUrl(this), html.toString(), "text/html", "utf-8", NonLeakingWebView.PREFIX_PAGEMODEL + content.getPage());
            setChapterTitle(pageModel);
            Log.d(TAG, "Load Content: " + content.getLastXScroll() + " " + content.getLastYScroll() + " " + content.getLastZoom());

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

    private void PrepareHtml(String content)
    {
        final NonLeakingWebView wv = (NonLeakingWebView) findViewById(R.id.webViewContent);

        StringBuilder html = new StringBuilder();
        html.append("<html><head>");
        html.append(DisplayNovelContentHtmlHelper.getCSSSheet());
        html.append(DisplayNovelContentHtmlHelper.getViewPortMeta());
        html.append("</head><body onclick='toogleHighlight(this, event);' onload='setup();'>");
        html.append(content);

        //Add to DisplayLightPageNovel.
        html.append( "<p align='right'>"+ pageContent.getCurrentPageNumber() +"</p>");
        html.append("</body></html>");

        wv.loadDataWithBaseURL(UIHelper.getBaseUrl(this), html.toString(), "text/html", "utf-8", NonLeakingWebView.PREFIX_PAGEMODEL + pageContent.getPage());
    }


    private void PrepareImage(String content)
    {
       final NonLeakingWebView wv = (NonLeakingWebView) findViewById(R.id.webViewContent);

       ImageModel image =  pageContent.getCurrentImage();
       String imageUrl = image.getUrl().toString();//"file:///" + Util.sanitizeFilename(image.getUrl().toString());
        imageUrl = imageUrl.replace("file:////", "file:///");

        StringBuilder html = new StringBuilder();
        html.append("<html><head>");
        html.append(DisplayNovelContentHtmlHelper.getViewPortMeta());
        html.append("</head><body>");

        html.append(content.toString());
        //html.append("<img src=\"" + imageUrl+ "\" width=\"100%\" height=\"100%\" >");
        html.append("</body></html>");

        wv.loadDataWithBaseURL(UIHelper.getBaseUrl(this), html.toString(), "text/html", "utf-8", NonLeakingWebView.PREFIX_PAGEMODEL + pageContent.getPage());
    }

    public void PreviousPage()
    {
       String content =  pageContent.previousPage();
        if(!pageContent.isImage())
        {
            PrepareHtml(content);
        }
        else
        {
            PrepareImage(content);
        }
    }

    public void NextPage()
    {
        String content = pageContent.nextPage();
        if(!pageContent.isImage())
        {
            PrepareHtml(content);
        }
        else
        {
            PrepareImage(content);
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {

        long touchInitialTime = motionEvent.getDownTime();
        long touchCurrentTime = motionEvent.getEventTime();

        int eventType = motionEvent.getAction();
        if(eventType == MotionEvent.ACTION_UP && touchCurrentTime - touchInitialTime < 300) //TODO get tap delay
        {
            double middle  = webView.getWidth() * 0.5;
            float currentPosition = motionEvent.getX();

            boolean topDirection = currentPosition < middle;
            int yPos = webView.getScrollY();

            float density =  webView.getResources().getDisplayMetrics().density;
            int maxY = (int) ((webView.getContentHeight() * density) - webView.getHeight());
            maxY -= 10; // TODO write a useful comment

            if( yPos >= maxY && !topDirection) //end of page
            {
                NextPage();
                goTop(webView); //here go to new page.
            }
            else if( yPos == 0 && topDirection )//start of page
            {
                PreviousPage();
                goBottom(webView);
            }
            else //scroll
            {
                int scrollSize = UIHelper.getIntFromPreferences(Constants.PREF_SCROLL_SIZE, 5) * 300;
                if (topDirection) //left
                {
                    webView.flingScroll(0, -scrollSize);
                }
                else //right
                {
                    webView.flingScroll(0, +scrollSize);
                }
            }

            return true; //Handle single tap
        }

        return false; //no handle
    }
}