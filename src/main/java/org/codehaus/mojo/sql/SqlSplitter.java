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
public class SqlSplitter 
{
    /**
     * Value indicating the sql has no end-delimiter like i.e. semicolon
     */
    public static final  int NO_END = -1; 
    
    /**
     * Check if the given sql line contains an end of command ';'
     * Please note that we do <em>not</em> fully parse the SQL, 
     * so if we get a malformed statement, we cannot detect it.
     * 
     * @param line to parse
     * @param delimiter which should be used to split SQL commands
     * @return position after the end character if the given line contains the end of a SQL script, 
     *         {@value SqlSplitter#NO_END} } if it doesn't contain an end char.
     */
    public static int containsSqlEnd( String line, String delimiter )
    {
        // / * * / comments
        boolean isComment = false;
        
        boolean isAlphaDelimiter = StringUtils.isAlpha( delimiter );
        
        if ( line == null || line.length() == 0 )
        {
            return NO_END;
        }
        
        int pos = 0;
        int maxpos = line.length() - 1;

        char c1;
        char c2 = line.charAt( 0 );
        do
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
                // parse for a * / start of comment
                if ( c1 == '*' && c2 == '/' )
                {
                    isComment = false;
                }
                else
                {
                    pos++;
                    continue;
                }
            }

            // parse for a / * end of comment
            if ( c1 == '/' && c2 == '*' )
            {
                isComment = true;
                pos += 2;
                continue;
            }
            
            if ( c1 == '-' && c2 == '-' )
            {
                return NO_END;
            }
            
            if (  c1 == '\'' || c2 == '\"' )
            {
                String quoteChar = String.valueOf( c1 );
                String quoteEscape = "\\" + quoteChar;
                pos++;
                
                if ( line.length() <= pos )
                {
                    return NO_END;
                }
                
                do 
                {
                    if ( startsWith( line, quoteEscape, pos ) )
                    {
                        pos += 2;
                    }
                    if ( pos >= maxpos )
                    {
                        return maxpos + 1;
                    }
                } while ( !startsWith( line, quoteChar, pos++ ) );
                
                continue;
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
            
            pos++;
            
        } while ( line.length() >= pos );
            
        return NO_END;
    }

    /**
     * small performance optimized replacement for {@link String#startsWith(String, int)}
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
     * @param c the char to check
     * @return <code>true</code> if the given character is either a lower or an upperchase alphanumerical character
     */
    private static boolean isAlpha( char c )
    {
        return Character.isUpperCase( c ) || Character.isLowerCase( c );
    }
    
}
