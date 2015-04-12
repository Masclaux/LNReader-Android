package com.erakk.lnreader.model;

import android.util.Log;


import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;

/**
 * Model for novel in PageMode
 * Created by jeremy.masclaux on 25/03/2015.
 */
public class PageNovelContentModel extends NovelContentModel
{
    private static final String TAG = PageNovelContentModel.class.toString();

    public static String REGEX ="h2,h3,p,.image";

    //max character in one page
    public static int MAX_CHARACTER_PAGE = 2500;

    //max block in one page
    public static int MAX_BLOC_PAGE = 35;

    private ArrayList<String> pages = new ArrayList<>();

    //return the current number
    public int getCurrentPageNumber()
    {
        return currentPage + 1 ;
    }

    public PageNovelContentModel( NovelContentModel model )
    {
      id        =  model.id;
      content   =  model.content;
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
      currentPage =  model.currentPage;
    }

    public void setContent(String content)
    {
        super.setContent(content);
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

    public void generateContent(Document doc)
    {
        int tempPara  = 0;
        int tempImage = 0;

        Elements res =  doc.select(REGEX);
        if( res.size() > 0 )
        {
            StringBuilder builder = new StringBuilder();

            Element e;
            for (int i = 0; i < res.size(); i++)
            {
                e = res.get(i);
                if( !e.tagName().equals("a"))
                {

                    switch ( e.tagName() )
                    {
                        case "h2" :
                        {
                            builder.append("<h2>");
                            builder.append(e.html());
                            builder.append("</h2>");
                            break;
                        }

                        case "h3" :
                        {
                            builder.append("<h3>");
                            builder.append(e.html());
                            builder.append("</h3>");
                            break;
                        }

                        case "p" :
                        {
                            builder.append("<p>");
                            builder.append(e.html());
                            builder.append("</p>");
                            tempPara++;
                            break;
                        }
                    }

                }
                else // image new page !
                {
                    pages.add( Integer.toString(tempImage) );//probably bad placement.
                    tempImage++;
                }

                if(builder.length() >= MAX_CHARACTER_PAGE || tempPara >= MAX_BLOC_PAGE )
                {
                    pages.add(builder.toString());//new page
                    tempPara     = 0;
                    builder.setLength(0);
                }
            }

            //Last Page
            if( builder.length() > 0 )
            {
                pages.add(builder.toString());
            }
        }
    }
}
