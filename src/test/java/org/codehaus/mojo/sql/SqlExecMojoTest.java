package org.codehaus.mojo.sql;

/*
 * Copyright 2006 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

import junit.framework.TestCase;

import java.io.File;
import java.util.Properties;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;

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

    /**
     * No error when there is no input
     */
    public void testNoCommandMojo()
        throws MojoExecutionException
    {
        mojo.execute();

        assertEquals( 0, mojo.getGoodSqls() );
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

    public void testOnErrorContinueMojo()
        throws MojoExecutionException
    {
        String command = "create table BOGUS"; //bad syntax
        mojo.addText( command );
        mojo.setOnError( "continue" );
        mojo.execute();
        assertEquals( 0, mojo.getGoodSqls() );
    }

    public void testOnErrorAbortMojo()
        throws MojoExecutionException
    {
        String command = "create table BOGUS"; //bad syntax
        mojo.addText( command );

        try
        {
            mojo.execute();
            fail( "Execution is not aborted on error." );

        }
        catch ( MojoExecutionException e )
        {

        }

        assertEquals( 0, mojo.getGoodSqls() );
    }

    public void testNullSettings()
        throws MojoExecutionException
    {

        //force a lookup of username in settings which will fail wince
        //  settings is not set yet
        mojo.setUsername( null );

        try
        {
            mojo.execute();

            fail( "Failure is expected here since settings is null in unittest" );
        }
        catch ( NullPointerException e )
        {

        }
    }

    public void testDefaultUsernamePassword()
        throws MojoExecutionException
    {

        Settings settings = new Settings();
        Server server = new Server();
        settings.addServer( server );

        mojo.setSettings( settings );

        //force a lookup of username
        mojo.setUsername( null );
        mojo.setPassword( null );

        mojo.execute();

        assertEquals( "", mojo.getUsername() );
        assertEquals( "", mojo.getPassword() );

    }

    public void testUsernamePasswordLookup()
        throws MojoExecutionException
    {

        Settings settings = new Settings();
        Server server = new Server();
        server.setId( "somekey" );
        server.setUsername( "username" );
        server.setPassword( "password" );
        settings.addServer( server );

        mojo.setSettings( settings );

        //force a lookup of username
        mojo.setSettingsKey( "somekey" );
        mojo.setUsername( null );
        mojo.setPassword( null );

        mojo.execute();

        assertEquals( "username", mojo.getUsername() );
        assertEquals( "password", mojo.getPassword() );

    }

    public void testBadDriver()
    {
        mojo.setDriver( "bad-driver" );
        try
        {
            mojo.execute();

            fail( "Bad driver is not detected" );
        }
        catch ( MojoExecutionException e )
        {

        }
    }

    public void testBadUrl()
    {
        mojo.setUrl( "bad-url" );
        try
        {
            mojo.execute();

            fail( "Bad URL is not detected" );
        }
        catch ( MojoExecutionException e )
        {

        }
    }

    public void testBadFile()
    {
        File[] srcFiles = new File[1];
        srcFiles[0] = new File( "a-every-bogus-file-that-does-not-exist" );

        mojo.setSrcFiles( srcFiles );
        try
        {
            mojo.execute();

            fail( "Bad files is not detected" );
        }
        catch ( MojoExecutionException e )
        {

        }
    }

    public void testOnError()
    {
        mojo.setOnError( "AbOrT" );
        assertEquals( SqlExecMojo.ON_ERROR_ABORT, mojo.getOnError() );
        mojo.setOnError( "cOnTiNuE" );
        assertEquals( SqlExecMojo.ON_ERROR_CONTINUE, mojo.getOnError() );
        try
        {
            mojo.setOnError( "bad" );
            fail( IllegalArgumentException.class.getName() + " was not thrown." );
        }
        catch ( IllegalArgumentException e )
        {
            //expected
        }
        try
        {
            mojo.setOnError( null );
            fail( IllegalArgumentException.class.getName() + " was not thrown." );
        }
        catch ( IllegalArgumentException e )
        {
            //expected
        }
    }
}
