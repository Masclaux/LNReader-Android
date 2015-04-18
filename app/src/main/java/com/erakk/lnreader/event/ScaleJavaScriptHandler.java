package com.erakk.lnreader.event;

import android.webkit.JavascriptInterface;

import com.erakk.lnreader.activity.DisplayLightPageNovelContentActivity;

//handle and set Scaling value from javascript
public class ScaleJavaScriptHandler
{
    DisplayLightPageNovelContentActivity parentActivity;

    public ScaleJavaScriptHandler(DisplayLightPageNovelContentActivity activity)
    {
        parentActivity = activity;
    }

    @JavascriptInterface
    public void setScale(float scaling)
    {
        parentActivity.currentScale = scaling;
    }
}
