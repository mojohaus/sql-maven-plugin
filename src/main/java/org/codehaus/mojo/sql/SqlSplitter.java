package org.codehaus.mojo.sql;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */

import org.codehaus.plexus.util.StringUtils;

/**
 * Utility class to split a long sql batch script into single SQL commands.
 */
public final class SqlSplitter 
{
    private SqlSplitter()
    {
        // hide utility class constructor
    }
    
    /**
     * Value indicating the sql has no end-delimiter like i.e. the semicolon.
     */
    public static final int NO_END = -1;
    
    /**
     * parsed sql started a single quote static text which continues on the next line (did not end)
     */
    public static final int OVERFLOW_SINGLE_QUOTE = -2;

    /**
     * parsed sql started a double quote static text which continues on the next line (did not end)
     */
    public static final int OVERFLOW_DOUBLE_QUOTE = -4;

    /**
     * parsed sql started a comment with /_* which continues on the next line (did not end)
     */
    public static final int OVERFLOW_COMMENT = -8;

    /**
     * Check if the given sql line contains a delimiter representing the end of the command.
     * Please note that we do <em>not</em> fully parse the SQL, 
     * so if we get a malformed statement, we cannot detect it.
     * 
     * @param line to parse
     * @param delimiter which should be used to split SQL commands
     * @param overflowValue 0=none, {@link SqlSplitter#OVERFLOW_COMMENT},
     *        {@link SqlSplitter#OVERFLOW_SINGLE_QUOTE} or
     *        {@link SqlSplitter#OVERFLOW_DOUBLE_QUOTE}
     * @return position after the end character if the given line contains the end of a SQL script, 
     *         {@link SqlSplitter#NO_END} if it doesn't contain an end char. {@link SqlSplitter#OVERFLOW_SINGLE_QUOTE}
     *         will be returned if a single quote didn't get closed, {@link SqlSplitter#OVERFLOW_DOUBLE_QUOTE} likewise
     *         for not closed double quotes.
     */
    public static int containsSqlEnd( String line, String delimiter, final int overflowValue )
    {
        int ret = overflowValue >= 0 ? NO_END : overflowValue;

        // / * * / comments
        boolean isComment = ( overflowValue == OVERFLOW_COMMENT );

        String quoteChar = null;
        if ( overflowValue == OVERFLOW_SINGLE_QUOTE )
        {
            quoteChar = "'";
        }
        else if ( overflowValue == OVERFLOW_DOUBLE_QUOTE  )
        {
            quoteChar = "\"";
        }


        boolean isAlphaDelimiter = StringUtils.isAlpha( delimiter );
        
        if ( line == null || line.length() == 0 )
        {
            return ret;
        }
        
        int pos = 0;
        int maxpos = line.length() - 1;

        char c1;
        char c2 = line.charAt( 0 );
        statement: do
        {
            // use the nextchar from the previous iteration
            c1 = c2;

            if ( pos < maxpos )
            {
                // and set the following char
                c2 = line.charAt( pos + 1 );
            }
            else {
                // or reset to blank if the line has ended
                c2 = ' ';
            }

            if ( isComment )
            {
                do
                {
                    // keep c2 in line
                    if ( pos < maxpos )
                    {
                        c2 = line.charAt( pos + 1 );
                    }
                    
                    if ( startsWith( line, '*', pos ) && startsWith( line, '/', pos + 1 ) )
                    {
                        ret = NO_END;
                        isComment = false;
                        
                        continue statement;
                    }
                } 
                while ( pos++ < maxpos );
                
                //reached EOL
                break statement;
            }

            // if in quote-mode, search for end quote, respecting escaped characters
            if (  quoteChar != null )
            {
                String doubleQuote = quoteChar + quoteChar;
                do
                {
                    // keep c2 in line
                    if ( pos < maxpos )
                    {
                        c2 = line.charAt( pos + 1 );
                    }
                    
                    if ( startsWith( line, "\\", pos ) || startsWith( line, doubleQuote, pos ) )
                    {
                        //skip next character, but stay in quote-mode
                        pos++;
                    }
                    else if ( startsWith( line, quoteChar, pos ) )
                    {
                        ret = NO_END;
                        quoteChar = null;
                        
                        continue statement;
                    }
                }
                while ( pos++ < maxpos );

                // reach EOL
                break statement;
            }
            
            // verify if current char indicates start  of new quoted block 
            if ( c1 == '\'' || c1 == '"' )
            {
                quoteChar = String.valueOf( c1 );
                ret = quoteChar.equals( "'" ) ? OVERFLOW_SINGLE_QUOTE : OVERFLOW_DOUBLE_QUOTE;
                continue statement;
            }

            // parse for a / * start of comment
            if ( c1 == '/' && c2 == '*' )
            {
                isComment = true;
                pos++;
                ret = OVERFLOW_COMMENT;
                continue statement;
            }
            
            if ( c1 == '-' && c2 == '-' )
            {
                return ret;
            }

            if ( startsWith( line, delimiter, pos ) )
            {
                if ( isAlphaDelimiter )
                {
                    // check if delimiter is at start or end of line, surrounded
                    // by non-alpha character
                    if (  ( pos == 0 || !isAlpha( line.charAt( pos - 1 ) ) ) 
                                    && ( line.length() == pos + delimiter.length() 
                                    || !isAlpha( line.charAt( pos + delimiter.length() ) ) ) ) 
                    {
                        return pos + delimiter.length();
                    }
                }
                else
                {
                    return pos + delimiter.length();
                }
            }
            
        } while ( line.length() >= pos++ );
            
        return ret;
    }

    /**
     * Small performance optimized replacement for {@link String#startsWith(String, int)}.
     * 
     * @param toParse the String to parse
     * @param delimiter the delimiter to look for
     * @param position the initial position to start the scan with
     */
    private static boolean startsWith( String toParse, String delimiter, int position )
    {
        if ( delimiter.length() == 1 )
        {
            return toParse.length() > position && toParse.charAt( position ) == delimiter.charAt( 0 );
        }
        else
        {
            return toParse.startsWith( delimiter, position );
        }
    }
    
    /**
     * 
     * @param toParse the String to parse
     * @param delimiter the delimiter to look for
     * @param position the initial position to start the scan with
     * @return
     */
    private static boolean startsWith( String toParse, char delimiter, int position )
    {
        return toParse.length() > position && toParse.charAt( position ) == delimiter;
    }
    
    /**
     * @param c the char to check
     * @return <code>true</code> if the given character is either a lower or an upperchase alphanumerical character
     */
    private static boolean isAlpha( char c )
    {
        return Character.isUpperCase( c ) || Character.isLowerCase( c );
    }
    
}
