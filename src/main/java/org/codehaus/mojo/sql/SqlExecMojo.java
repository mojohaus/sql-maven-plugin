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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.apache.maven.shared.filtering.MavenFileFilterRequest;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.codehaus.plexus.interpolation.Interpolator;
import org.codehaus.plexus.interpolation.RegexBasedInterpolator;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcherException;

/**
 * Executes SQL against a database.
 * @goal execute
 */
public class SqlExecMojo
    extends AbstractMojo
{

    /**
     * Call {@link #setOnError(String)} with this value to abort SQL command execution
     * if an error is found.
     */
    public static final String ON_ERROR_ABORT = "abort";

    /**
     * Call {@link #setOnError(String)} with this value to continue SQL command execution until 
     * all commands have been attempted, then abort the build if an SQL error occurred in any 
     * of the commands.
     */
    public static final String ON_ERROR_ABORT_AFTER = "abortAfter";
    
    /**
     * Call {@link #setOnError(String)} with this value to continue SQL command execution
     * if an error is found.
     */
    public static final String ON_ERROR_CONTINUE = "continue";

    /**
     * Call {@link #setOrderFile(String)} with this value to sort in ascendant order the sql files.
     */
    public static final String FILE_SORTING_ASC = "ascending";

    /**
     * Call {@link #setOrderFile(String)} with this value to sort in descendant order the sql files.
     */
    public static final String FILE_SORTING_DSC = "descending";

    //////////////////////////// User Info ///////////////////////////////////

    /**
     * Database username.  If not given, it will be looked up through
     * <code>settings.xml</code>'s server with <code>${settingsKey}</code> as key.
     * @since 1.0
     * @parameter expression="${username}"
     */
    private String username;

    /**
     * Database password. If not given, it will be looked up through <code>settings.xml</code>'s
     * server with <code>${settingsKey}</code> as key.
     * @since 1.0
     * @parameter expression="${password}"
     */
    private String password;

    /**
      * Ignore the password and use anonymous access.
      * This may be useful for databases like MySQL which do not allow empty
      * password parameters in the connection initialization.
      * @since 1.4
      * @parameter default-value="false"
      */
    private boolean enableAnonymousPassword;
    
    /**
     * Additional key=value pairs separated by comma to be passed into JDBC driver.
     * @since 1.0
     * @parameter expression="${driverProperties}" default-value = ""
     */
    private String driverProperties;

    /**
     * @parameter expression="${settings}"
     * @required
     * @since 1.0
     * @readonly
     */
    private Settings settings;

    /**
     * Server's <code>id</code> in <code>settings.xml</code> to look up username and password.
     * Defaults to <code>${url}</code> if not given.
     * @since 1.0
     * @parameter expression="${settingsKey}"
     */
    private String settingsKey;
    
    /**
     * MNG-4384
     * 
     * @since 1.5
     * @component role="hidden.org.sonatype.plexus.components.sec.dispatcher.SecDispatcher"
     * @required  
     */
    private SecDispatcher securityDispatcher;    

    /**
     * Skip execution when there is an error obtaining a connection.
     * This is a special case to support databases, such as embedded Derby,
     * that can shutdown the database via the URL (i.e. <code>shutdown=true</code>).
     * @since 1.1
     * @parameter expression="${skipOnConnectionError}" default-value="false"
     */
    private boolean skipOnConnectionError;

    /**
     * Setting this parameter to <code>true</code> will force
     * the execution of this mojo, even if it would get skipped usually.
     *  
     * @parameter expression="${forceOpenJpaExecution}"
     *            default-value=false
     * @required
     */
    private boolean forceMojoExecution; 

    /**
     * The Maven Project Object
     *
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;
    
    /**
     * @parameter default-value="${session}"
     * @required
     * @readonly
     */
    private MavenSession mavenSession;

    //////////////////////////////// Source info /////////////////////////////

    /**
     * SQL input commands separated by <code>${delimiter}</code>.
     * @since 1.0
     * @parameter expression="${sqlCommand}" default-value=""
     */
    private String sqlCommand = "";

    /**
     * List of files containing SQL statements to load.
     * 
     * @since 1.0
     * @parameter
     * @see #fileset
     */
    private File[] srcFiles;

    /**
     * File(s) containing SQL statements to load.
     * Only use a Fileset if you want to use ant-like filepatterns, otherwise use srcFiles.
     * The order is based on a matching occurrence while scanning the directory (not the order of includes!).
     * 
     * @since 1.0
     * @parameter
     * @see #srcFiles
     */
    private Fileset fileset;

    /**
     * When <code>true</code>, skip the execution.
     * @since 1.0
     * @parameter default-value="false"
     */
    private boolean skip;

    ////////////////////////////////// Database info /////////////////////////
    /**
     * Database URL.
     * @parameter expression="${url}"
     * @required
     * @since 1.0-beta-1
     */
    private String url;

    /**
     * Database driver classname.
     * @since 1.0
     * @parameter expression="${driver}"
     * @required
     */
    private String driver;

    ////////////////////////////// Operation Configuration ////////////////////
    /**
     * Set to <code>true</code> to execute none-transactional SQL.
     * @since 1.0
     * @parameter expression="${autocommit}" default-value="false"
     */
    private boolean autocommit;

    /**
     * Action to perform if an error is found.
     * Possible values are <code>abort</code> and <code>continue</code>.
     * @since 1.0
     * @parameter expression="${onError}" default-value="abort"
     */
    private String onError = ON_ERROR_ABORT;

    ////////////////////////////// Parser Configuration ////////////////////

    /**
     * Set the delimiter that separates SQL statements.
     *
     * @since 1.0
     * @parameter expression="${delimiter}" default-value=";"
     */
    private String delimiter = ";";

    /**
     * <p>The delimiter type takes two values - "normal" and "row". Normal
     * means that any occurrence of the delimiter terminate the SQL
     * command whereas with row, only a line containing just the
     * delimiter is recognized as the end of the command.</p>
     * <p>For example, set this to "go" and delimiterType to "row" for
     * Sybase ASE or MS SQL Server.</p>
     * @since 1.2
     * @parameter expression="${delimiterType}" default-value="normal"
     */
    private String delimiterType = DelimiterType.NORMAL;

    /**
     * Set the order in which the SQL files will be executed.
     * Possible values are <code>ascending</code> and <code>descending</code>.
     * Any other value means that no sorting will be performed.
     * Refers to {@link #fileset} and {@link #srcFiles}
     * 
     * @since 1.1
     * @parameter expression="${orderFile}"
     */
    private String orderFile = null;

    /**
     * When <code>true</code>, the whole SQL content in <code>sqlCommand</code>, <code>srcFiles</code> and
     * <code>fileset</code> are sent directly to JDBC in one SQL statement. This option
     * is for executing database stored procedures/functions.
     * @deprecated used <i>delimiterType<i> instead.
     * @since 1.1
     * @parameter expression="${enableBlockMode}"
     */

    private boolean enableBlockMode = false;

    /**
     * Keep the format of an SQL block.
     * @since 1.1
     * @parameter expression="${keepFormat}" default-value="false"
     */
    private boolean keepFormat = false;

    ///////////////////////////////////////////////////////////////////////////////////////
    /**
     * Print SQL results.
     * @parameter 
     * @since 1.3
     */
    private boolean printResultSet = false;

    /**
     * Print header columns.
     */
    private boolean showheaders = true;

    /**
     * Dump the SQL exection's output to a file.  Default is stdout.
     * @parameter 
     * @since 1.3
     */
    private File outputFile;


    /**
     * @parameter default-value=","
     * @since 1.4
     */
    private String outputDelimiter;
    
    
    /**
     * Encoding to use when reading SQL statements from a file.
     * @parameter expression="${encoding}" default-value= "${project.build.sourceEncoding}"
     * @since 1.1
     */
    private String encoding = "";

    /**
     * Append to an existing file or overwrite it?
     */
    private boolean append = false;

    /**
     * Argument to Statement.setEscapeProcessing
     * If you want the driver to use regular SQL syntax then set this to false.
     * 
     * @since 1.4
     * @parameter expression="${escapeProcessing}" default-value="true"
     */
    private boolean escapeProcessing = true;

    ////////////////////////////////// Internal properties//////////////////////

    /**
     * number of successful executed statements 
     */
    private int successfulStatements = 0;

    /**
     * number of total executed statements
     */
    private int totalStatements = 0;

    /**
     * Database connection
     */
    private Connection conn = null;

    /**
     * SQL statement
     */
    private Statement statement = null;
    
    /**
     * SQL transactions to perform
     */
    private Vector transactions = new Vector();

    /**
     * @component role="org.apache.maven.shared.filtering.MavenFileFilter"
     * @since 1.4
     */
    private MavenFileFilter fileFilter;

    /**
     * Set to true if you want to filter the srcFiles using system-, user- and project properties
     * 
     * @parameter
     * @since 1.4
     */
    private boolean enableFiltering;
    
    /**
     * Interpolator especially for braceless expressions  
     */
    private Interpolator interpolator = new RegexBasedInterpolator( "\\$([^\\s;)]+?)", "(?=[\\s;)])" );
    
    /**
     * Add a SQL transaction to execute
     * @return a new SqlExecMojo.Transaction
     */
    public Transaction createTransaction()
    {
        Transaction t = new Transaction();
        transactions.addElement( t );
        return t;
    }

    /**
     * Set an inline SQL command to execute.
     * NB: Properties are not expanded in this text.
     * 
     * @param sql the sql statement to add
     */
    public void addText( String sql )
    {
        this.sqlCommand += sql;
    }

    /**
     * Set the file encoding to use on the SQL files read in
     *
     * @param encoding the encoding to use on the files
     */
    public void setEncoding( String encoding )
    {
        this.encoding = encoding;
    }

    /**
     * Set the delimiter that separates SQL statements. Defaults to &quot;;&quot;;
     * 
     * @param delimiter the new delimiter
     */
    public void setDelimiter( String delimiter )
    {
        this.delimiter = delimiter;
    }

    /**
     * Set the delimiter type: "normal" or "row" (default "normal").
     * 
     * @param delimiterType the new delimiterType
     */
    public void setDelimiterType( String delimiterType )
    {
        this.delimiterType = delimiterType;
    }

    /**
     * Print result sets from the statements;
     * optional, default false
     * 
     * @param print <code>true</code> to print the resultset, otherwise <code>false</code>
     * @deprecated typo, use setPrintResultSet()
     */
    public void setPrintResutlSet( boolean print )
    {
        setPrintResultSet( print );
    }
    
    /**
     * Print result sets from the statements;
     * optional, default false
     * 
     * @param print <code>true</code> to print the resultset, otherwise <code>false</code>
     */
    public void setPrintResultSet( boolean print )
    {
        this.printResultSet = print;
    }

    /**
     * Print headers for result sets from the
     * statements; optional, default true.
     * 
     * @param showheaders <code>true</code> to show the headers, otherwise <code>false</code>
     */
    public void setShowheaders( boolean showheaders )
    {
        this.showheaders = showheaders;
    }

    /**
     * Set the output file;
     * 
     * @param output the output file
     */
    public void setOutputFile( File output )
    {
        this.outputFile = output;
    }

    /**
     * whether output should be appended to or overwrite
     * an existing file.  Defaults to false.
     * 
     * @param append <code>true</code> to append, otherwise <code>false</code> to overwrite 
     */
    public void setAppend( boolean append )
    {
        this.append = append;
    }

    /**
     * whether or not format should be preserved.
     * Defaults to false.
     *
     * @param keepformat The keepformat to set
     */
    public void setKeepFormat( boolean keepformat )
    {
        this.keepFormat = keepformat;
    }

    /**
     * Set escape processing for statements.
     * 
     * @param enable <code>true</code> to escape, otherwiser <code>false</code>
     */
    public void setEscapeProcessing( boolean enable )
    {
        escapeProcessing = enable;
    }

    /**
     * <p>Determine if the mojo execution should get skipped.</p>
     * This is the case if:
     * <ul>
     *   <li>{@link #skip} is <code>true</code></li>
     *   <li>if the mojo gets executed on a project with packaging type 'pom' and
     *       {@link #forceMojoExecution} is <code>false</code></li>
     * </ul>
     * 
     * @return <code>true</code> if the mojo execution should be skipped.
     */
    protected boolean skipMojo() 
    {
        if ( skip )
        {
            getLog().info( "Skip sql execution" );
            return true;
        }
        
        if ( !forceMojoExecution && project != null && "pom".equals( project.getPackaging() ) )
        {
            getLog().info( "Skipping sql execution for project with packaging type 'pom'" );
            return true;
        }
        
        return false;
    }
    

    /**
     * Load the sql file and then execute it
     * @throws MojoExecutionException
     */
    public void execute()
        throws MojoExecutionException
    {

        if ( skipMojo() )
        {
            return;
        }

        successfulStatements = 0;

        totalStatements = 0;

        loadUserInfoFromSettings();

        addCommandToTransactions();

        addFilesToTransactions();

        addFileSetToTransactions();

        sortTransactions();

        try
        {
            conn = getConnection();
        }
        catch ( SQLException e )
        {
            if ( !this.skipOnConnectionError )
            {
                throw new MojoExecutionException( e.getMessage(), e );
            }
            else
            {
                //error on get connection and user asked to skip the rest
                return;
            }
        }

        try
        {
            statement = conn.createStatement();
            statement.setEscapeProcessing( escapeProcessing );

            PrintStream out = System.out;
            try
            {
                if ( outputFile != null )
                {
                    getLog().debug( "Opening PrintStream to output file " + outputFile );
                    out = new PrintStream( new BufferedOutputStream( new FileOutputStream( outputFile.getAbsolutePath(),
                                                                                           append ) ) );
                }

                // Process all transactions
                for ( Enumeration e = transactions.elements(); e.hasMoreElements(); )
                {
                    Transaction t = (Transaction) e.nextElement();

                    t.runTransaction( out );

                    if ( !autocommit )
                    {
                        getLog().debug( "Committing transaction" );
                        conn.commit();
                    }
                }
            }
            finally
            {
                if ( out != null && out != System.out )
                {
                    out.close();
                }
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
        catch ( SQLException e )
        {
            if ( !autocommit && conn != null && ON_ERROR_ABORT.equalsIgnoreCase( getOnError() ) )
            {
                try
                {
                    conn.rollback();
                }
                catch ( SQLException ex )
                {
                    // ignore
                }
            }
            throw new MojoExecutionException( e.getMessage(), e );
        }
        finally
        {
            try
            {
                if ( statement != null )
                {
                    statement.close();
                }
                if ( conn != null )
                {
                    conn.close();
                }
            }
            catch ( SQLException ex )
            {
                // ignore
            }
        }

        getLog().info(
                       getSuccessfulStatements() + " of " + getTotalStatements()
                           + " SQL statements executed successfully" );
        
        if ( ON_ERROR_ABORT_AFTER.equalsIgnoreCase( getOnError() ) 
             && totalStatements != successfulStatements )
        {
            throw new MojoExecutionException( "Some SQL statements failed to execute" );
        }

    }

    /**
     * Add sql command to transactions list.
     *
     */
    private void addCommandToTransactions()
    {
        createTransaction().addText( sqlCommand.trim() );
    }

    /**
     * Add user sql fileset to transation list
     *
     */
    private void addFileSetToTransactions()
    {
        String[] includedFiles;
        if ( fileset != null )
        {
            fileset.scan();
            includedFiles = fileset.getIncludedFiles();
        }
        else
        {
            includedFiles = new String[0];
        }

        for ( int j = 0; j < includedFiles.length; j++ )
        {
            createTransaction().setSrc( new File( fileset.getBasedir(), includedFiles[j] ) );
        }
    }

    /**
     * Add user input of srcFiles to transaction list.
     * @throws MojoExecutionException
     */
    private void addFilesToTransactions()
        throws MojoExecutionException
    {
        File[] files = getSrcFiles();
        
        MavenFileFilterRequest request = new MavenFileFilterRequest();
        request.setEncoding( encoding );
        request.setMavenSession( mavenSession );
        request.setMavenProject( project );
        request.setFiltering( enableFiltering );
        for ( int i = 0; files != null && i < files.length; ++i )
        {
            if ( files[i] != null && !files[i].exists() )
            {
                throw new MojoExecutionException( files[i].getPath() + " not found." );
            }

            File sourceFile = files[i];
            String basename = FileUtils.basename( sourceFile.getName() );
            String extension = FileUtils.extension( sourceFile.getName() );
            File targetFile = FileUtils.createTempFile( basename, extension, null );
            if ( !getLog().isDebugEnabled() ) 
            {
                targetFile.deleteOnExit();
            }
            
            request.setFrom( sourceFile );
            request.setTo( targetFile );
            
            try
            {
                fileFilter.copyFile( request );
            }
            catch ( MavenFilteringException e )
            {
                throw new MojoExecutionException( e.getMessage() );
            }
            
            createTransaction().setSrc( targetFile );
        }
    }

    /**
     * Sort the transaction list.
     */
    private void sortTransactions()
    {
        if ( FILE_SORTING_ASC.equalsIgnoreCase( this.orderFile ) )
        {
            Collections.sort( transactions );
        }
        else if ( FILE_SORTING_DSC.equalsIgnoreCase( this.orderFile ) )
        {
            Collections.sort( transactions, Collections.reverseOrder() );
        }
    }

    /**
     * Load username password from settings if user has not set them in JVM properties
     * 
     * @throws MojoExecutionException
     */
    private void loadUserInfoFromSettings()
        throws MojoExecutionException
    {
        if ( this.settingsKey == null )
        {
            this.settingsKey = getUrl();
        }

        if ( ( getUsername() == null || getPassword() == null ) && ( settings != null ) )
        {
            Server server = this.settings.getServer( this.settingsKey );

            if ( server != null )
            {
                if ( getUsername() == null )
                {
                    setUsername( server.getUsername() );
                }

                if ( getPassword() == null )
                {
                    if ( server.getPassword() != null )
                    {
                      try
                      {
                        setPassword( securityDispatcher.decrypt( server.getPassword() ) );
                      }
                      catch ( SecDispatcherException e )
                      {
                        throw new MojoExecutionException( e.getMessage() );
                      }  
                    }
                }
            }
        }

        if ( getUsername() == null )
        {
            //allow emtpy username
            setUsername( "" );
        }

        if ( getPassword() == null )
        {
            //allow emtpy password
            setPassword( "" );
        }
    }

    /**
     * Creates a new Connection as using the driver, url, userid and password
     * specified.
     *
     * The calling method is responsible for closing the connection.
     *
     * @return Connection the newly created connection.
     * @throws MojoExecutionException if the UserId/Password/Url is not set or there
     *    is no suitable driver or the driver fails to load.
     * @throws SQLException if there is problem getting connection with valid url
     *
     */
    private Connection getConnection()
        throws MojoExecutionException, SQLException
    {
        getLog().debug( "connecting to " + getUrl() );
        Properties info = new Properties();
        info.put( "user", getUsername() );
        
        if ( ! enableAnonymousPassword )
        {
            info.put( "password", getPassword() );
        }

        info.putAll( this.getDriverProperties() );

        Driver driverInstance = null;

        try
        {
            Class dc = Class.forName( getDriver() );
            driverInstance = (Driver) dc.newInstance();
        }
        catch ( ClassNotFoundException e )
        {
            throw new MojoExecutionException( "Driver class not found: " + getDriver(), e );
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Failure loading driver: " + getDriver(), e );
        }

        Connection conn = driverInstance.connect( getUrl(), info );

        if ( conn == null )
        {
            // Driver doesn't understand the URL
            throw new SQLException( "No suitable Driver for " + getUrl() );
        }

        conn.setAutoCommit( autocommit );
        return conn;
    }

    /**
     * parse driverProperties into Properties set
     * @return the driver properties
     * @throws MojoExecutionException
     */
    protected Properties getDriverProperties()
        throws MojoExecutionException
    {
        Properties properties = new Properties();

        if ( !StringUtils.isEmpty( this.driverProperties ) )
        {
            String[] tokens = StringUtils.split( this.driverProperties, "," );
            for ( int i = 0; i < tokens.length; ++i )
            {
                String[] keyValueTokens = StringUtils.split( tokens[i].trim(), "=" );
                if ( keyValueTokens.length != 2 )
                {
                    throw new MojoExecutionException( "Invalid JDBC Driver properties: " + this.driverProperties );
                }

                properties.setProperty( keyValueTokens[0], keyValueTokens[1] );

            }
        }

        return properties;
    }

    /**
     * read in lines and execute them
     * 
     * @param reader the reader
     * @param out the outputstream
     * @throws SQLException
     * @throws IOException
     */
    private void runStatements( Reader reader, PrintStream out )
        throws SQLException, IOException
    {
        String line;

        if ( enableBlockMode )
        {
            //no need to parse the content, ship it directly to jdbc in one sql statement
            line = IOUtil.toString( reader );
            execSQL( line, out );
            return;
        }

        StringBuffer sql = new StringBuffer();

        BufferedReader in = new BufferedReader( reader );

        while ( ( line = in.readLine() ) != null )
        {
            if ( !keepFormat )
            {
                line = line.trim();
            }
            
            if ( !keepFormat )
            {
                if ( line.startsWith( "//" ) )
                {
                    continue;
                }
                if ( line.startsWith( "--" ) )
                {
                    continue;
                }
                StringTokenizer st = new StringTokenizer( line );
                if ( st.hasMoreTokens() )
                {
                    String token = st.nextToken();
                    if ( "REM".equalsIgnoreCase( token ) )
                    {
                        continue;
                    }
                }
            }

            if ( !keepFormat )
            {
                sql.append( " " ).append( line );
            }
            else
            {
                sql.append( "\n" ).append( line );
            }

            // SQL defines "--" as a comment to EOL
            // and in Oracle it may contain a hint
            // so we cannot just remove it, instead we must end it
            if ( !keepFormat )
            {
                if ( SqlSplitter.containsSqlEnd( line, delimiter ) == SqlSplitter.NO_END )
                {
                    sql.append( "\n" );
                }
            }
            
            if ( ( delimiterType.equals( DelimiterType.NORMAL ) && SqlSplitter.containsSqlEnd( line, delimiter ) > 0 )
                || ( delimiterType.equals( DelimiterType.ROW ) && line.trim().equals( delimiter ) ) )
            {
                execSQL( sql.substring( 0, sql.length() - delimiter.length() ), out );
                sql.setLength( 0 ); // clean buffer
            }
        }

        // Catch any statements not followed by ;
        if ( !sql.toString().equals( "" ) )
        {
            execSQL( sql.toString(), out );
        }
    }

    /**
     * Exec the sql statement.
     * 
     * @param sql query to execute 
     * @param out the outputstream
     */
    private void execSQL( String sql, PrintStream out )
        throws SQLException
    {
        // Check and ignore empty statements
        if ( "".equals( sql.trim() ) )
        {
            return;
        }

        ResultSet resultSet = null;
        try
        {
            totalStatements++;
            getLog().debug( "SQL: " + sql );

            boolean ret;
            int updateCountTotal = 0;

            
            ret = statement.execute( sql );
            do
            {
                if ( !ret )
                {
                    int updateCount = statement.getUpdateCount();
                    if ( updateCount != -1 )
                    {
                        updateCountTotal += updateCount;
                    }
                }
                else
                {
                    resultSet = statement.getResultSet();
                    if ( printResultSet )
                    {
                        printResultSet( resultSet, out );
                    }
                }
                ret = statement.getMoreResults();
            }
            while ( ret );

            getLog().debug( updateCountTotal + " rows affected" );

            if ( printResultSet )
            {
                StringBuffer line = new StringBuffer();
                line.append( updateCountTotal ).append( " rows affected" );
                out.println( line );
            }

            SQLWarning warning = conn.getWarnings();
            while ( warning != null )
            {
                getLog().debug( warning + " sql warning" );
                warning = warning.getNextWarning();
            }
            conn.clearWarnings();
            successfulStatements++;
        }
        catch ( SQLException e )
        {
            getLog().error( "Failed to execute: " + sql );
            if ( ON_ERROR_ABORT.equalsIgnoreCase( getOnError() ) )
            {
                throw e;
            }
            getLog().error( e.toString() );
        }
        finally
        {
            if ( resultSet != null )
            {
                resultSet.close();
            }
        }
    }

    /**
     * print any results in the result set.
     * @param rs the resultset to print information about
     * @param out the place to print results
     * @throws SQLException on SQL problems.
     */
    private void printResultSet( ResultSet rs, PrintStream out )
        throws SQLException
    {
        if ( rs != null )
        {
            getLog().debug( "Processing new result set." );
            ResultSetMetaData md = rs.getMetaData();
            int columnCount = md.getColumnCount();
            StringBuffer line = new StringBuffer();
            if ( showheaders )
            {
                boolean first = true;
                for ( int col = 1; col <= columnCount; col++ )
                {
                    String columnValue = md.getColumnName( col );
                    
                    if ( columnValue != null )
                    {
                        columnValue = columnValue.trim();
                        
                        if ( ",".equals( outputDelimiter ) ) 
                        {
                            columnValue = StringEscapeUtils.escapeCsv( columnValue );
                        }
                    }

                    if ( first )
                    {
                        first = false;
                    }
                    else
                    {
                        line.append( outputDelimiter );
                    }
                    line.append( columnValue );
                }
                out.println( line );
                line = new StringBuffer();
            }
            while ( rs.next() )
            {
                boolean first = true;
                for ( int col = 1; col <= columnCount; col++ )
                {
                    String columnValue = rs.getString( col );
                    if ( columnValue != null )
                    {
                        columnValue = columnValue.trim();
                        
                        if ( ",".equals( outputDelimiter ) ) 
                        {
                            columnValue = StringEscapeUtils.escapeCsv( columnValue );
                        }
                    }

                    if ( first )
                    {
                        first = false;
                    }
                    else
                    {
                        line.append( outputDelimiter );
                    }
                    line.append( columnValue );
                }
                out.println( line );
                line = new StringBuffer();
            }
        }
        out.println();
    }

    /**
     * Contains the definition of a new transaction element.
     * Transactions allow several files or blocks of statements
     * to be executed using the same JDBC connection and commit
     * operation in between.
     */
    private class Transaction
        implements Comparable
    {
        private File tSrcFile = null;

        private String tSqlCommand = "";

        /**
         *
         */
        public void setSrc( File src )
        {
            this.tSrcFile = src;
        }

        /**
         *
         */
        public void addText( String sql )
        {
            this.tSqlCommand += sql;
        }

        /**
         *
         */
        private void runTransaction( PrintStream out )
            throws IOException, SQLException
        {
            if ( tSqlCommand.length() != 0 )
            {
                getLog().info( "Executing commands" );

                runStatements( new StringReader( tSqlCommand ), out );
            }

            if ( tSrcFile != null )
            {
                getLog().info( "Executing file: " + tSrcFile.getAbsolutePath() );

                Reader reader = null;

                if ( StringUtils.isEmpty( encoding ) )
                {
                    reader = new FileReader( tSrcFile );
                }
                else
                {
                    reader = new InputStreamReader( new FileInputStream( tSrcFile ), encoding );
                }

                try
                {
                    runStatements( reader, out );
                }
                finally
                {
                    reader.close();
                }
            }
        }

        public int compareTo( Object object )
        {
            Transaction transaction = (Transaction) object;

            if ( transaction.tSrcFile == null )
            {
                if ( this.tSrcFile == null )
                {
                    return 0;
                }
                else
                {
                    return Integer.MAX_VALUE;
                }
            }
            else
            {
                if ( this.tSrcFile == null )
                {
                    return Integer.MIN_VALUE;
                }
                else
                {
                    return this.tSrcFile.compareTo( transaction.tSrcFile );
                }
            }
        }
    }

    //
    // helper accessors for unit test purposes
    //

    public String getUsername()
    {
        return this.username;
    }

    public void setUsername( String username )
    {
        this.username = username;
    }

    public String getPassword()
    {
        return this.password;
    }

    public void setPassword( String password )
    {
        this.password = password;
    }

    public String getUrl()
    {
        return this.url;
    }

    public void setUrl( String url )
    {
        this.url = url;
    }

    public String getDriver()
    {
        return this.driver;
    }

    public void setDriver( String driver )
    {
        this.driver = driver;
    }

    void setAutocommit( boolean autocommit )
    {
        this.autocommit = autocommit;
    }

    void setFileset( Fileset fileset )
    {
        this.fileset = fileset;
    }

    public File[] getSrcFiles()
    {
        return this.srcFiles;
    }

    public void setSrcFiles( File[] files )
    {
        this.srcFiles = files;
    }

    public String getOrderFile()
    {
        return this.orderFile;
    }

    public void setOrderFile( String orderFile )
    {
        if ( FILE_SORTING_ASC.equalsIgnoreCase( orderFile ) )
        {
            this.orderFile = FILE_SORTING_ASC;
        }
        else if ( FILE_SORTING_DSC.equalsIgnoreCase( orderFile ) )
        {
            this.orderFile = FILE_SORTING_DSC;
        }
        else
        {
            throw new IllegalArgumentException( orderFile + " is not a valid value for orderFile, only '"
                + FILE_SORTING_ASC + "' or '" + FILE_SORTING_DSC + "'." );
        }
    }

    /**
     * @deprecated use {@link #getSuccessfulStatements()}
     */
    int getGoodSqls()
    {
        return this.getSuccessfulStatements();
    }

    /**
     * Number of SQL statements executed so far that caused errors.
     *
     * @return the number
     */
    public int getSuccessfulStatements()
    {
        return successfulStatements;
    }

    /**
     * Number of SQL statements executed so far, including the ones that caused errors.
     *
     * @return the number
     */
    public int getTotalStatements()
    {
        return totalStatements;
    }

    public String getOnError()
    {
        return this.onError;
    }

    public void setOnError( String action )
    {
        if ( ON_ERROR_ABORT.equalsIgnoreCase( action ) )
        {
            this.onError = ON_ERROR_ABORT;
        }
        else if ( ON_ERROR_CONTINUE.equalsIgnoreCase( action ) )
        {
            this.onError = ON_ERROR_CONTINUE;
        }
        else if ( ON_ERROR_ABORT_AFTER.equalsIgnoreCase( action ) )
        {
            this.onError = ON_ERROR_ABORT_AFTER;
        }
        else
        {
            throw new IllegalArgumentException( action 
                + " is not a valid value for onError, only '" + ON_ERROR_ABORT
                + "', '" + ON_ERROR_ABORT_AFTER + "', or '" + ON_ERROR_CONTINUE + "'." );
        }
    }

    void setSettings( Settings settings )
    {
        this.settings = settings;
    }

    void setSettingsKey( String key )
    {
        this.settingsKey = key;
    }

    void setSkip( boolean skip )
    {
        this.skip = skip;
    }

    public void setDriverProperties( String driverProperties )
    {
        this.driverProperties = driverProperties;
    }

    public boolean isEnableBlockMode()
    {
        return enableBlockMode;
    }

    public void setEnableBlockMode( boolean enableBlockMode )
    {
        this.enableBlockMode = enableBlockMode;
    }

    public String getSqlCommand()
    {
        return sqlCommand;
    }

    public void setSqlCommand( String sqlCommand )
    {
        this.sqlCommand = sqlCommand;
    }
    
    public Vector getTransactions()
    {
        return transactions;
    }

    public void setTransactions( Vector transactions )
    {
        this.transactions = transactions;
    }

    public void setFileFilter( MavenFileFilter filter )
    {
        this.fileFilter = filter;
    }
    
    public void setSecurityDispatcher( SecDispatcher securityDispatcher )
    {
        this.securityDispatcher = securityDispatcher;
    }
}
