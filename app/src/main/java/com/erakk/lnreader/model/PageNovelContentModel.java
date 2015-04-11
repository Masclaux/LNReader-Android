package com.erakk.lnreader.model;

import java.util.ArrayList;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Model for novel in PageMode
 * Created by jeremy.masclaux on 25/03/2015.
 */
public class PageNovelContentModel extends NovelContentModel
{
    public static String REGEX = "<p\\b[^>]*>(.*?)</p>|<div\\s+class=\"thumb tright\"[^>]*>((?:(?:(?!<div[^>]*>|</div>).)+|<div[^>]*>[\\s\\S]*?</div>)*)</div>"
            + "|<h2\\b[^>]*>(.*?)</h2>"+"|<h3\\b[^>]*>(.*?)</h3>";

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
        return !pages.get(currentPage).startsWith("<");
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

    public void generateContent()
    {
        //Check number of page
        Pattern p = Pattern.compile(REGEX, Pattern.DOTALL ); // get all para
        Matcher m = p.matcher(content);

        int tempPara  = 0;
        int tempImage = 0;
        String tempParaText = "";

        while(m.find())
        {
            if ( m.group().length() != 0 ) //res
            {
                tempPara++;

                String res =  m.group();
                if( !res.startsWith("<div class="))
                {
                    tempParaText += res;
                }
                else // image new page !
                {
                    pages.add( Integer.toString(tempImage) );//probably bad placement.
                    tempImage++;
                }
            }

            if(tempParaText.length() >= MAX_CHARACTER_PAGE || tempPara >= MAX_BLOC_PAGE )
            {
                pages.add(tempParaText);//new page
                tempPara     = 0;
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
