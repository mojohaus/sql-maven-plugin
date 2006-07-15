package org.codehaus.mojo.sql;

/*
 * The MIT License
 *
 * Copyright (c) 2004, The Codehaus
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import junit.framework.TestCase;

import java.io.File;
import java.util.Properties;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * Unit test for simple SqlExecMojo.
 */
public class SqlExecMojoTest
    extends TestCase
{
    private SqlExecMojo mojo;

    private Properties p;

    public void setUp()
        throws Exception
    {
        super.setUp();
        p = new Properties();
        p.load( getClass().getResourceAsStream( "/test.properties" ) );

        mojo = new SqlExecMojo();

        // populate parameters
        mojo.setDriver( p.getProperty( "driver" ) );
        mojo.setUsername( p.getProperty( "user" ) );
        mojo.setPassword( p.getProperty( "password" ) );
        mojo.setUrl( p.getProperty( "url" ) );

    }

 
    public void testCreateCommandMojo()
        throws MojoExecutionException
    {
        String command = "create table PERSON ( PERSON_ID integer, FIRSTNAME varchar, LASTNAME varchar)";
        mojo.addText( command );
        mojo.execute();
        
        assertEquals( 1, mojo.getGoodSqls() );
        
    }

    public void testDropCommandMojo()
        throws MojoExecutionException
    {
        String command = "drop table PERSON";
        mojo.addText( command );
        mojo.execute();
        assertEquals( 1, mojo.getGoodSqls() );
    }

    public void testFileSetMojo()
        throws MojoExecutionException
    {

        Fileset ds = new Fileset();
        ds.setBasedir( "src/test" );
        ds.setIncludes( new String[] { "**/create*.sql" } );
        ds.scan();
        assert ( ds.getIncludedFiles().length == 1 );

        mojo.setFileset( ds );

        mojo.execute();
        
        assertEquals( 3, mojo.getGoodSqls() );
        
    }

    public void testFileArrayMojo()
        throws MojoExecutionException
    {
        File[] srcFiles = new File[1];
        srcFiles[0] = new File( System.getProperty( "basedir" ) + "/src/test/data/drop-test-tables.sql" );

        mojo.setSrcFiles( srcFiles );
        mojo.execute();
        
        assertEquals( 3, mojo.getGoodSqls() );
        
    }

    /** 
     * Ensure srcFiles always execute first
     * 
     */
    public void testAllMojo()
        throws MojoExecutionException
    {
        
        String command = "create table PERSON2 ( PERSON_ID integer, FIRSTNAME varchar, LASTNAME varchar)";
        mojo.addText( command );        

        File[] srcFiles = new File[1];
        srcFiles[0] = new File( System.getProperty( "basedir" ) + "/src/test/data/create-test-tables.sql" );
        mojo.setSrcFiles( srcFiles );

        Fileset ds = new Fileset();
        ds.setBasedir( "src/test" );
        ds.setIncludes( new String[] { "**/drop*.sql" } );
        ds.scan();
        mojo.setFileset( ds );
        mojo.execute();

        assertEquals( 7, mojo.getGoodSqls() );
    }

}
