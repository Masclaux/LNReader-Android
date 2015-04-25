package com.erakk.lnreader.model;

import android.util.Log;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Model for novel in PageMode
 * Created by jeremy.masclaux on 25/03/2015.
 */
public class PageNovelContentModel extends NovelContentModel
{
    private static final String TAG = PageNovelContentModel.class.toString();

    private static String P  = "<p\\b[^>]*>(.*?)</p>"  ;
    private static String H2 = "<h2\\b[^>]*><span class=\"mw-headline\"(.*?)</h2>";
    private static String H3 = "<h3\\b[^>]*><span class=\"mw-headline\"(.*?)</h3>";
    private static String IMAGE = "<a([^>]+)>(.+?)</a>";

    public static String REGEX = H2 + "|" + H3 + "|" + P + "|" + IMAGE;


    //max words in one page
    public static int MAX_WORDS = 350;

    //max block in one page
    public static int MAX_BLOC_PAGE = 17;

    private ArrayList<String> pages = new ArrayList<>();

    //return the current number
    public int getCurrentPageNumber()
    {
        return currentPage + 1 ;
    }

    public PageNovelContentModel( NovelContentModel model )
    {
        id        =  model.id;
        page      =  model.page;
        pageModel =  model.pageModel;
        lastXScroll =  model.lastXScroll;
        lastYScroll =  model.lastYScroll;
        lastZoom    =  model.lastZoom;
        lastUpdate  =  model.lastUpdate;
        lastCheck   =  model.lastCheck;
        isUpdatingFromInternet =  model.isUpdatingFromInternet;
        images    =  model.images;
        bookmarks =  model.bookmarks;
        currentPage = model.currentPage;

        setContent(model.content);
    }

    public void setContent(String content)
    {
        super.setContent(content);

        generateContent();
    }

    public String getPageContent()
    {
        if( pages.size() > currentPage )
        {
            return pages.get(currentPage);
        }
        else
        {
            return ""; // Error ?
        }
    }

    /**
     * The current page is an image ?
     * @return true if image
     */
    public boolean isImage()
    {
        try
        {
            int t = Integer.parseInt( pages.get(currentPage) );
            Log.i(TAG, "Request image " + t);

            return true;
        }
        catch (NumberFormatException e)
        {
            return false;
        }
    }

    /**
     * Check if we are at the beginning of the chapter
     * @return true if first page
     */
    public boolean isFirstPage()
    {
        return currentPage == 0;
    }

    /**
     * Check if we are at the end of the chapter
     * @return true if last page
     */
    public boolean isLastPage()
    {
        return pages.size() -1 == currentPage;
    }

    /**
     * Got to the previous page
     * @return the content of the new page
     */
    public String previousPage()
    {
        if( !isFirstPage()  )
        {
            currentPage--;
        }

        return getPageContent();
    }

    /**
     * Got to the page
     * @return the content of the new page
     */
    public String goToPage(int page)
    {
        if( currentPage <= pages.size() -1)
        {
            currentPage = page;
        }

        return getPageContent();
    }

    /**
     * Got to the next page
     * @return the content of the new page
     */
    public String nextPage()
    {
        if( !isLastPage() )
        {
            currentPage++;
        }

        return getPageContent();
    }

    /**
     * Number of pages
     * @return number of page
     */
    public int getPageNumber()
    {
        return pages.size();
    }


    private void generateContent()
    {
        //Check number of page
        Pattern p = Pattern.compile(REGEX, Pattern.DOTALL ); // get all para
        Matcher m = p.matcher(content);

        int tempPara = 0;
        String tempParaText = "";

        int tempImage = 0;

        int tempWords = 0;

        while(m.find())
        {
            if ( m.group().length() != 0 ) //res
            {
                String res =  m.group();
                if( !res.startsWith("<a") )
                {
                    //if H2 found and content already added new page
                    if( res.startsWith("<h2") && tempParaText.length() > 0 )
                    {
                        pages.add(tempParaText);//new page
                        tempPara     = 0;
                        tempWords    = 0;
                        tempParaText ="";
                    }

                    tempPara++;
                    tempParaText += res;
                    tempWords    += res.trim().split("\\s+").length;
                }
                else // image new page !
                {
                    if( res.contains( "class=\"image\"" ) )
                    {
                        pages.add( Integer.toString(tempImage) );//probably bad placement.
                        tempImage++;
                    }

                }
            }

            if(tempWords >= MAX_WORDS || tempPara >= MAX_BLOC_PAGE )
            {
                pages.add(tempParaText);//new page
                tempPara     = 0;
                tempWords    = 0;
                tempParaText ="";
            }
        }

        //Last Page
        if( tempParaText.length() > 0 )
        {
            pages.add(tempParaText);
        }
    }
}
