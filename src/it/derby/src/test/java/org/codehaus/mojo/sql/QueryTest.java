package org.codehaus.mojo.sql;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.ResultSet;
import java.sql.Statement;

import junit.framework.TestCase;

public class QueryTest
    extends TestCase
{

    public void testQuery()
        throws Exception
    {
        Class dc = Class.forName( "org.apache.derby.jdbc.EmbeddedDriver" );
        Driver   driverInstance = (Driver) dc.newInstance();

        Connection conn = driverInstance.connect( "jdbc:derby:target/testdb", null );

        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery( "select count(*) from derbyDB" );
        rs.next();
        assertEquals( 2, rs.getInt(1) );  
        
    }
}
