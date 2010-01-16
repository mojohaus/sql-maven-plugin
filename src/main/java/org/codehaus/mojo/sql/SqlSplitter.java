package org.codehaus.mojo.sql;

import org.codehaus.plexus.util.StringUtils;

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

/**
 * Utility class to split a long sql batch script into single SQL commands.
 */
public class SqlSplitter {
    
    public final static int NO_END = -1; 
    
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
    public static int containsSqlEnd(String line, String delimiter) {
        // / * * / comments
        boolean isComment = false;
        
        boolean isAlphaDelimiter = StringUtils.isAlpha( delimiter );
        
        if ( line == null || line.length() == 0 )
        {
            return NO_END;
        }
        
        int pos = 0;
        
        do 
        {
            if ( isComment )
            {
                if ( line.startsWith( "*/", pos ) )
                {
                    isComment = false;
                }
                else
                {
                    pos++;
                    continue;
                }
            }
            
            if ( line.startsWith( "/*", pos ) )
            {
                isComment = true;
                pos += 2;
                continue;
            }
            
            if ( line.startsWith( "--", pos ) )
            {
                return NO_END;
            }
            
            if (  line.startsWith( "'", pos ) || line.startsWith( "\"", pos ) )
            {
                String quoteChar = "" + line.charAt( pos );
                String quoteEscape = "\\" + quoteChar;
                pos++;
                
                if ( line.length() <= pos )
                {
                    return NO_END;
                }
                
                do 
                {
                    if ( line.startsWith( quoteEscape, pos ) )
                    {
                        pos += 2;
                    }
                } while ( !line.startsWith( quoteChar, pos++ ));
                
                continue;
            }

            if ( line.startsWith( delimiter, pos ) )
            {
                if ( isAlphaDelimiter )
                {
                    // check if delimiter is at start or end of line, surrounded
                    // by non-alpha character
                    if(  ( pos == 0 || !isAlpha( line.charAt( pos - 1 ) ) ) &&
                         ( line.length() == pos + delimiter.length() ||
                           !isAlpha( line.charAt( pos + delimiter.length() ) ) ) ) 
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
     * @return <code>true</code> if the given character is either a lower or an upperchase alphanumerical character
     */
    private static boolean isAlpha( char c )
    {
        return Character.isUpperCase( c ) || Character.isLowerCase( c );
    }
    
}
