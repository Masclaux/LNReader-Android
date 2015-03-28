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
    public static String REGEX = "<p\\b[^>]*>(.*?)</p>|<div\\s+class=\"thumb tright\"[^>]*>((?:(?:(?!<div[^>]*>|</div>).)+|<div[^>]*>[\\s\\S]*?</div>)*)</div>"
            + "|<h2\\b[^>]*>(.*?)</h2>"+"|<h3\\b[^>]*>(.*?)</h3>";

    //max character in one page
    public static int MAX_CHARACTER_PAGE = 1400;

    //max block in one page
    public static int MAX_BLOC_PAGE = 35;

    //current page ( we generate virtual page from the text of virtual page)
    private int currentPage = 0;


    private ArrayList<String> pages = new ArrayList<>();

    //return the current number
    public int getCurrentPageNumber()
    {
        return currentPage + 1 ;
    }

    public void setContent(String content)
    {
        GenerateContent(content);

        super.setContent( content );
    }

    public String getContent()
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

    private void GenerateContent(String content)
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

                String res =  m.group();
                if( !res.startsWith("<div class="))
                {
                    tempParaText += res;
                }
                else // image new page !
                {
                    pages.add(res);//probably bad placement.
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
