package org.codehaus.mojo.sql;

/*
 * Copyright 2006 The Codehaus
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;

import java.io.File;
import java.util.Properties;

/**
 * Unit test for simple SqlExecMojo.<br/>
 * ATTENTION: These are no unit tests, cause the are order dependent.<br/>
 * This must be fixed. At the moment it is handled having appropriate method names.
 */
@FixMethodOrder( MethodSorters.NAME_ASCENDING )
public class SqlExecMojoTest
    extends AbstractMojoTestCase
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
        mojo.setDriverProperties( p.getProperty( "driverProperties" ) );
        mojo.setSqlCommand( null );
        mojo.setDelimiter( SqlExecMojo.DEFAULT_DELIMITER );// This will simulate the defaultValue of @Parameter (...)
        mojo.setOnError( SqlExecMojo.ON_ERROR_ABORT );
        mojo.setDelimiterType( DelimiterType.NORMAL );
        mojo.setEscapeProcessing( true );

        MavenFileFilter filter =
            (MavenFileFilter) lookup( "org.apache.maven.shared.filtering.MavenFileFilter", "default" );
        mojo.setFileFilter( filter );

        SecDispatcher securityDispatcher =
            (SecDispatcher) lookup( "org.sonatype.plexus.components.sec.dispatcher.SecDispatcher", "default" );
        mojo.setSecurityDispatcher( securityDispatcher );

        MavenProject project = new MavenProjectStub();
        setVariableValueToObject( mojo, "project", project );
    }

    /**
     * No error when there is no input
     */
    public void test018NoCommandMojo()
        throws MojoExecutionException
    {
        mojo.execute();

        assertEquals( 0, mojo.getSuccessfulStatements() );
    }

    public void test001CreateCommandMojo()
        throws MojoExecutionException
    {
        String command = "create table PERSON ( PERSON_ID integer, FIRSTNAME varchar, LASTNAME varchar)";
        mojo.addText( command );
        mojo.execute();

        assertEquals( 1, mojo.getSuccessfulStatements() );
    }

    public void test019DropCommandMojo()
        throws MojoExecutionException
    {
        String command = "drop table PERSON";
        mojo.addText( command );
        mojo.execute();
        assertEquals( 1, mojo.getSuccessfulStatements() );
    }

    public void test020FileSetMojo()
        throws MojoExecutionException
    {

        Fileset ds = new Fileset();
        ds.setBasedir( "src/test" );
        ds.setIncludes( new String[] { "**/create*.sql" } );
        ds.scan();
        assert( ds.getIncludedFiles().length == 1 );

        mojo.setFileset( ds );

        mojo.execute();

        assertEquals( 3, mojo.getSuccessfulStatements() );

    }

    public void test021FileArrayMojo()
        throws MojoExecutionException
    {
        File[] srcFiles = new File[1];
        srcFiles[0] = new File( "src/test/data/drop-test-tables.sql" );

        mojo.setSrcFiles( srcFiles );
        mojo.execute();

        assertEquals( 3, mojo.getSuccessfulStatements() );

    }

    /**
     * Ensure srcFiles always execute first
     */
    public void test022AllMojo()
        throws MojoExecutionException
    {

        String command = "create table PERSON2 ( PERSON_ID integer, FIRSTNAME varchar, LASTNAME varchar)";
        mojo.addText( command );

        File[] srcFiles = new File[1];
        srcFiles[0] = new File( "src/test/data/create-test-tables.sql" );
        mojo.setSrcFiles( srcFiles );

        Fileset ds = new Fileset();
        ds.setBasedir( "src/test" );
        ds.setIncludes( new String[] { "**/drop*.sql" } );
        ds.scan();
        mojo.setFileset( ds );
        mojo.execute();

        assertEquals( 7, mojo.getSuccessfulStatements() );
    }

    public void test023OrderFile()
        throws MojoExecutionException
    {
        Fileset ds = new Fileset();
        ds.setBasedir( "src/test" );
        ds.setIncludes( new String[] { "**/drop*.sql", "**/create*.sql" } );
        ds.scan();
        mojo.setFileset( ds );

        mojo.setOrderFile( SqlExecMojo.FILE_SORTING_ASC );
        mojo.execute();

        assertEquals( 6, mojo.getSuccessfulStatements() );

        try
        {
            mojo.setOrderFile( SqlExecMojo.FILE_SORTING_DSC );
            mojo.execute();
            fail( "Execution is not aborted on error." );
        }
        catch ( MojoExecutionException e )
        {
        }
    }

    public void test024OnErrorContinueMojo()
        throws MojoExecutionException
    {
        String command = "create table BOGUS"; // bad syntax
        mojo.addText( command );
        mojo.setOnError( "continue" );
        mojo.execute();
        assertEquals( 0, mojo.getSuccessfulStatements() );
    }

    public void test025OnErrorAbortMojo()
        throws MojoExecutionException
    {
        String command = "create table BOGUS"; // bad syntax
        mojo.addText( command );

        try
        {
            mojo.execute();
            fail( "Execution is not aborted on error." );

        }
        catch ( MojoExecutionException e )
        {

        }

        assertEquals( 0, mojo.getSuccessfulStatements() );
    }

    public void test026OnErrorAbortAfterMojo()
        throws MojoExecutionException
    {
        String commands = "create table BOGUS"; // bad syntax

        mojo.addText( commands );

        File[] srcFiles = new File[1];
        srcFiles[0] = new File( "src/test/data/invalid-syntax.sql" );

        assertTrue( srcFiles[0].exists() );

        mojo.setSrcFiles( srcFiles );
        mojo.setOnError( "abortAfter" );

        try
        {
            mojo.execute();
            fail( "Execution is not aborted on error." );

        }
        catch ( MojoExecutionException e )
        {
            // expected
        }

        assertEquals( 0, mojo.getSuccessfulStatements() );
        assertEquals( 2, mojo.getTotalStatements() );
    }

    public void test002DefaultUsernamePassword()
        throws MojoExecutionException
    {

        Settings settings = new Settings();
        Server server = new Server();
        settings.addServer( server );

        mojo.setSettings( settings );

        // force a lookup of username
        mojo.setUsername( null );
        mojo.setPassword( null );

        mojo.execute();

        assertEquals( "", mojo.getUsername() );
        assertEquals( "", mojo.getPassword() );

    }

    public void test003UsernamePasswordLookup()
        throws MojoExecutionException
    {

        Settings settings = new Settings();
        Server server = new Server();
        server.setId( "somekey" );
        server.setUsername( "username" );
        server.setPassword( "password" );
        settings.addServer( server );

        mojo.setSettings( settings );

        // force a lookup of username
        mojo.setSettingsKey( "somekey" );
        mojo.setUsername( null );
        mojo.setPassword( null );

        mojo.execute();

        assertEquals( "username", mojo.getUsername() );
        assertEquals( "password", mojo.getPassword() );

    }

    public void test004BadDriver()
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

    public void test005BadUrl()
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

    public void test006BadFile()
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

    public void test007OnError()
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
            // expected
        }
        try
        {
            mojo.setOnError( null );
            fail( IllegalArgumentException.class.getName() + " was not thrown." );
        }
        catch ( IllegalArgumentException e )
        {
            // expected
        }
    }

    public void test008Skip()
        throws MojoExecutionException
    {
        String command = "create table PERSON ( PERSON_ID integer, FIRSTNAME varchar, LASTNAME varchar)";
        mojo.addText( command );
        mojo.setSkip( true );
        mojo.execute();

        // no command was executed due to skip is on
        assertEquals( 0, mojo.getSuccessfulStatements() );
    }

    public void test009DriverProperties()
        throws MojoExecutionException
    {
        Properties driverProperties = this.mojo.getDriverProperties();
        assertEquals( 2, driverProperties.size() );
        assertEquals( "value1", driverProperties.get( "key1" ) );
        assertEquals( "value2", driverProperties.get( "key2" ) );

        mojo.setDriverProperties( "key1=value1,key2" );
        try
        {
            driverProperties = this.mojo.getDriverProperties();
        }
        catch ( MojoExecutionException e )
        {
        }

    }

    public void test010BlockMode()
        throws MojoExecutionException
    {
        String command = "create table BLOCKTABLE ( PERSON_ID integer, FIRSTNAME varchar, LASTNAME varchar)";
        mojo.addText( command );
        // TODO: Check if this is equal mojo.setEnableBlockMode( true );
        // to the following:
        mojo.setDelimiter( DelimiterType.ROW );
        mojo.execute();
        assertEquals( 1, mojo.getSuccessfulStatements() );

        mojo.setSqlCommand( "" );
        mojo.getTransactions().clear();
        command = "drop table BLOCKTABLE";
        mojo.addText( command );
        mojo.execute();
        assertEquals( 1, mojo.getSuccessfulStatements() );
    }

    public void test011KeepFormat()
        throws MojoExecutionException
    {
        // Normally a line starting in -- would be ignored, but with keepformat mode
        // on it will not.
        String command = "--create table PERSON ( PERSON_ID integer, FIRSTNAME varchar, LASTNAME varchar)";
        mojo.addText( command );
        mojo.setKeepFormat( true );

        try
        {
            mojo.execute();
            fail( "-- at the start of the SQL command is ignored." );
        }
        catch ( MojoExecutionException e )
        {
        }

        assertEquals( 0, mojo.getSuccessfulStatements() );

    }

    public void test012BadDelimiter()
        throws Exception
    {
        String command = "create table SEPARATOR ( PERSON_ID integer, FIRSTNAME varchar, LASTNAME varchar):"
            + "create table SEPARATOR2 ( PERSON_ID integer, FIRSTNAME varchar, LASTNAME varchar)";

        mojo.addText( command );
        mojo.setDelimiter( ":" );

        try
        {
            mojo.execute();
            fail( "Expected parser error." );
        }
        catch ( MojoExecutionException e )
        {
        }
    }

    public void test013GoodDelimiter()
        throws Exception
    {
        String command = "create table SEPARATOR ( PERSON_ID integer, FIRSTNAME varchar, LASTNAME varchar)\n:\n"
            + "create table SEPARATOR2 ( PERSON_ID integer, FIRSTNAME varchar, LASTNAME varchar)";

        mojo.addText( command );
        mojo.setDelimiter( ":" );

        mojo.execute();

        assertEquals( 2, mojo.getSuccessfulStatements() );
    }

    public void test014BadDelimiterType()
        throws Exception
    {
        String command = "create table BADDELIMTYPE ( PERSON_ID integer, FIRSTNAME varchar, LASTNAME varchar)" + "\n:"
            + "create table BADDELIMTYPE2 ( PERSON_ID integer, FIRSTNAME varchar, LASTNAME varchar)";

        mojo.addText( command );
        mojo.setDelimiter( ":" );
        mojo.setDelimiterType( DelimiterType.ROW );

        try
        {
            mojo.execute();
            fail( "Expected parser error." );
        }
        catch ( MojoExecutionException e )
        {
        }
    }

    public void test015GoodDelimiterType()
        throws Exception
    {
        String command = "create table GOODDELIMTYPE ( PERSON_ID integer, FIRSTNAME varchar, LASTNAME varchar)"
            + "\n:  \n" + "create table GOODDELIMTYPE2 ( PERSON_ID integer, FIRSTNAME varchar, LASTNAME varchar)";

        mojo.addText( command );
        mojo.setDelimiter( ":" );
        mojo.setDelimiterType( DelimiterType.NORMAL );

        mojo.execute();
        assertEquals( 2, mojo.getSuccessfulStatements() );
    }

    public void test016OutputFile()
        throws Exception
    {
        String command = "create table GOODDELIMTYPE3 ( PERSON_ID integer, FIRSTNAME varchar, LASTNAME varchar)"
            + "\n:  \n" + "create table GOODDELIMTYPE4 ( PERSON_ID integer, FIRSTNAME varchar, LASTNAME varchar)";

        mojo.addText( command );
        mojo.setDelimiter( ":" );
        mojo.setDelimiterType( DelimiterType.NORMAL );

        String basedir = System.getProperty( "basedir", "." );
        File outputFile = new File( basedir, "target/sql.out" );
        outputFile.delete();
        mojo.setOutputFile( outputFile );
        mojo.setPrintResultSet( true );

        mojo.execute();

        assertTrue( "Output file: " + outputFile + " not found.", outputFile.exists() );

        assertTrue( "Unexpected empty output file. ", outputFile.length() > 0 );

        // makesure we can remote the file, it is not locked
        // assertTrue( outputFile.delete() );

    }

    // MSQL-44
    public void test017AnonymousBlock()
        throws MojoExecutionException
    {
        String command = "--/ Anonymous SQL Block\n"
            + "create table ANONBLOCKTABLE ( PERSON_ID integer, FIRSTNAME varchar, LASTNAME varchar)\n" + "/\n"
            + "drop table ANONBLOCKTABLE";
        mojo.setDelimiter( "/" );
        mojo.addText( command );
        mojo.execute();
        assertEquals( 2, mojo.getSuccessfulStatements() );
    }

    public void test027SkipMissingFiles() throws MojoExecutionException {
        mojo.setSkipMissingFiles(true);
        mojo.setSrcFiles(new File[] {
                new File("src/test/data/create-test-tables.sql"),
                new File("non/existing/file/path.sql"),
                new File("src/test/data/drop-test-tables.sql" )});
        mojo.execute();
        assertEquals(6, mojo.getSuccessfulStatements());
    }
}
