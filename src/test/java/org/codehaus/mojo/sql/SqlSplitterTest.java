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

import junit.framework.TestCase;

public class SqlSplitterTest extends TestCase 
{

    public void testContainsSqlString() throws Exception
    {
        containsNot( "" );
        containsNot( " " );
        containsNot( "  \t  " );

        contains( ";" );
        contains( "SELECT * from myTable;" );
        
        contains( "SELECT * from myTable; -- with sl comment" );
        contains( "SELECT * from myTable; /* with part comment */" );

        contains( "SELECT * from myTable /* with part comment inside*/  ; " );
        
        contains( "INSERT INTO testTable (thevalue) VALUES ('value  !'); -- comment at the end" );

        // a " inside a ' quoted text
        contains( "INSERT INTO testTable (thevalue) VALUES ('value \"  !');" );

        // a ' inside a " quoted text
        contains( "INSERT INTO testTable (thevalue) VALUES (\"value '  !\");" );

        containsNot( "SELECT * from myTable where value = ';' AND -- semicolon is quoted!" );

    }

    public void testMsSQLStrings() throws Exception
    {
        String del = "GO";
        
        containsNot( "SELECT COUNT(*) FROM Logs", del );
        containsNot( "SELECT * FROM TPersons", del );
        contains( "GO", del ); 
    }
    
    
    private void contains( String sql ) throws Exception
    {
        contains( sql, ";" );
    }

    private void containsNot( String sql ) throws Exception
    {
        containsNot( sql, ";" );
    }

    private void contains( String sql, String delimiter ) throws Exception
    {
        assertTrue( sql, SqlSplitter.containsSqlEnd( sql, delimiter ) > 0);
    }

    private void containsNot( String sql, String delimiter ) throws Exception
    {
        assertTrue( sql, SqlSplitter.containsSqlEnd( sql, delimiter ) == SqlSplitter.NO_END);
    }
}
