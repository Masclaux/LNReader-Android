package com.erakk.lnreader.activity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.erakk.lnreader.Constants;
import com.erakk.lnreader.LNReaderApplication;
import com.erakk.lnreader.R;
import com.erakk.lnreader.UIHelper;
import com.erakk.lnreader.adapter.BookmarkModelAdapter;
import com.erakk.lnreader.adapter.PageModelAdapter;
import com.erakk.lnreader.callback.ICallbackEventData;
import com.erakk.lnreader.callback.IExtendedCallbackNotifier;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.helper.BakaTsukiWebChromeClient;
import com.erakk.lnreader.helper.BakaTsukiWebViewClient;
import com.erakk.lnreader.helper.DisplayNovelContentHtmlHelper;
import com.erakk.lnreader.helper.DisplayNovelContentTTSHelper;
import com.erakk.lnreader.helper.DisplayNovelContentUIHelper;
import com.erakk.lnreader.helper.NonLeakingWebView;
import com.erakk.lnreader.helper.OnCompleteListener;
import com.erakk.lnreader.helper.TtsHelper;
import com.erakk.lnreader.helper.Util;
import com.erakk.lnreader.model.BookModel;
import com.erakk.lnreader.model.BookmarkModel;
import com.erakk.lnreader.model.NovelCollectionModel;
import com.erakk.lnreader.model.NovelContentModel;
import com.erakk.lnreader.model.PageModel;
import com.erakk.lnreader.model.PageNovelContentModel;
import com.erakk.lnreader.parser.CommonParser;
import com.erakk.lnreader.task.AsyncTaskResult;
import com.erakk.lnreader.task.LoadNovelContentTask;
import com.erakk.lnreader.task.LoadWacTask;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.ArrayList;

public class DisplayLightPageNovelContentActivity extends DisplayLightNovelContentActivity
{
    private static final String TAG = DisplayLightPageNovelContentActivity.class.toString();

    /**
     * Setup the chapter from DB, Internal page only
     * Including JS for Bookmark highlighting, last read position, and CSS
     *
     * @param loadedContent
     */
    @SuppressLint("NewApi")
    public void setContent(PageNovelContentModel loadedContent)
    {

        this.content = loadedContent;

        try
        {
            PageModel pageModel = content.getPageModel();

            if (content.getLastUpdate().getTime() < pageModel.getLastUpdate().getTime())
                Toast.makeText(this, getResources().getString(R.string.content_may_updated, content.getLastUpdate().toString(), pageModel.getLastUpdate().toString()), Toast.LENGTH_LONG).show();

            // load the contents here
            final NonLeakingWebView wv = (NonLeakingWebView) findViewById(R.id.webViewContent);
            super.setWebViewSettings();

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
            html.append(content.getContent());
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

        }
        catch (Exception e)
        {
            Log.e(TAG, "Cannot load content.", e);

        }

    }
}