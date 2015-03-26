package com.erakk.lnreader.model;

import java.util.ArrayList;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * Created by jeremy.masclaux on 25/03/2015.
 */
public class PageNovelContentModel extends NovelContentModel
{
    public static String REGEX = "<p\\b[^>]*>(.*?)</p>|<div\\s+class=\"thumb tright\"[^>]*>((?:(?:(?!<div[^>]*>|</div>).)+|<div[^>]*>[\\s\\S]*?</div>)*)</div>";

    //max character in one page
    public static int MAX_CHARACTER_PAGE = 1400;

    //max block in one page
    public static int MAX_BLOC_PAGE = 35;

    //current page ( we generate virtual page from the text of virtual page)
    private int currentPage = 0;


    private ArrayList<String> pages = new ArrayList<>();

    //same as base class + current page
    public String getPage()
    {
        return super.getPage() + " page " + currentPage;
    }

    public void setContent(String content)
    {
        //Check number of page
        Pattern p = Pattern.compile(REGEX, Pattern.DOTALL ); // get all para
        Matcher m = p.matcher(content);

        int tempPara = 0;
        String tempParaText = "";

        while(m.find())
        {
            if ( m.group().length() != 0 ) //res
            {
                tempPara++;

                if( m.group().startsWith("<div class=") == false)
                {
                    tempParaText += m.group();
                }
                else // image new page !
                {
                    pages.add(m.group());
                }
            }

            if( tempParaText.length() >= MAX_CHARACTER_PAGE || tempPara >= MAX_BLOC_PAGE )
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

        super.setContent( content );
    }

    public String getContent()
    {
        return super.getContent();
    }
}
