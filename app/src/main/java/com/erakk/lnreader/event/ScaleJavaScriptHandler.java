package com.erakk.lnreader.event;

import android.webkit.JavascriptInterface;

import com.erakk.lnreader.activity.DisplayLightNovelContentActivity;


//handle and set Scaling value from javascript
public class ScaleJavaScriptHandler
{
    DisplayLightNovelContentActivity parentActivity;

    public ScaleJavaScriptHandler(DisplayLightNovelContentActivity activity)
    {
        parentActivity = activity;
    }

    @JavascriptInterface
    public void setScale(float scaling)
    {
        parentActivity.setScale( scaling );
    }
}
