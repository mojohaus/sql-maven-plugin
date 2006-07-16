package org.codehaus.mojo.sql;

/*
 * Copyright  2000-2006 The Apache Software Foundation
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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;

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
import java.util.Enumeration;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * Executes SQL against a database
 * @goal execute
 * @description A repackaging of ANT's SQLExec task.
 */
public class SqlExecMojo
    extends AbstractMojo
{

    //////////////////////////// User Info ///////////////////////////////////

    /**
     * Database username.  If not given, it will be looked up through 
     * settings.xml's server with ${settingsKey} as key
     * @parameter expression="${username}" 
     */
    private String username;

    /**
     * Database password. If not given, it will be looked up through settings.xml's 
     * server with ${settingsKey} as key
     * @parameter expression="${password}" 
     */
    private String password;

    /**
     * @parameter expression="${settings}"
     * @required
     * @readonly
     */
    private Settings settings;
    
    /**
     * Server's id in the settings.xml to be used to lookup username and password.
     * Default to ${url} if not given.
     * @parameter expression="${settingsKey}" 
     */
    private String settingsKey;    

    //////////////////////////////// Source info /////////////////////////////

    /**
     * SQL input commands separated by ${delimiter}
     * @parameter expression="${sqlCommand}" default-value=""
     */
    private String sqlCommand = "";

    /**
     * A single file containing SQL statements to load
     * @parameter expression="${srcFile}" 
     * @deprecated use srcFiles instead
     */
    private File srcFile;

    /**
     * List of files containing SQL statements to load
     * @parameter 
     */
    private File[] srcFiles;

    /**
     * File(s) containing SQL statements to load
     * @parameter
     */
    private Fileset fileset;

    ////////////////////////////////// Database info /////////////////////////
    /**
     * Database URL
     * @parameter expression="${url}" 
     * @required
     */
    private String url;

    /**
     * Database driver classname
     * @parameter expression="${driver}" 
     * @required
     */
    private String driver;

    ////////////////////////////// Operation Configuration ////////////////////
    /**
     * Set to true to execute none-transactional SQL
     * @parameter expression="${autocommit}" default-value="false"
     */
    private boolean autocommit;

    /**
     * Action to perform if an error is found
     * parameter expression="${onError}" default-value="abort"
     **/
    private String onError = "abort";

    /**
     * SQL Statement delimiter
     * @parameter expression="${delimiter}" default-value=";"
     */
    private String delimiter = ";";

    /**
     * The delimiter type indicating whether the delimiter will
     * only be recognized on a line by itself
     */
    private String delimiterType = DelimiterType.NORMAL;

    /**
     * Print SQL results.
     */
    private boolean print = false;

    /**
     * Print header columns.
     */
    private boolean showheaders = true;

    /**
     * Results Output file.
     */
    private File output = null;

    /**
     * Encoding to use when reading SQL statements from a file
     */
    private String encoding = null;

    /**
     * Append to an existing file or overwrite it?
     */
    private boolean append = false;

    /**
     * Keep the format of a sql block?
     */
    private boolean keepformat = false;

    /**
     * Argument to Statement.setEscapeProcessing
     */
    private boolean escapeProcessing = true;

    ////////////////////////////////// Internal properties//////////////////////

    private int goodSql = 0;

    private int totalSql = 0;

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
     * Add a SQL transaction to execute
     */
    public Transaction createTransaction()
    {
        Transaction t = new Transaction();
        transactions.addElement( t );
        return t;
    }

    /**
     * Set the name of the SQL file to be run.
     * Required unless statements are enclosed in the build file
     */
    public void setSrc( File srcFile )
    {
        this.srcFile = srcFile;
    }

    /**
     * Set an inline SQL command to execute.
     * NB: Properties are not expanded in this text.
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
     * optional
     *
     * <p>For example, set this to "go" and delimitertype to "ROW" for
     * Sybase ASE or MS SQL Server.</p>
     */
    public void setDelimiter( String delimiter )
    {
        this.delimiter = delimiter;
    }

    /**
     * Set the delimiter type: "normal" or "row" (default "normal").
     *
     * <p>The delimiter type takes two values - normal and row. Normal
     * means that any occurrence of the delimiter terminate the SQL
     * command whereas with row, only a line containing just the
     * delimiter is recognized as the end of the command.</p>
     */
    public void setDelimiterType( DelimiterType delimiterType )
    {
        this.delimiterType = delimiterType.getValue();
    }

    /**
     * Print result sets from the statements;
     * optional, default false
     */
    public void setPrint( boolean print )
    {
        this.print = print;
    }

    /**
     * Print headers for result sets from the
     * statements; optional, default true.
     */
    public void setShowheaders( boolean showheaders )
    {
        this.showheaders = showheaders;
    }

    /**
     * Set the output file;
     */
    public void setOutput( File output )
    {
        this.output = output;
    }

    /**
     * whether output should be appended to or overwrite
     * an existing file.  Defaults to false.
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
    public void setKeepformat( boolean keepformat )
    {
        this.keepformat = keepformat;
    }

    /**
     * Set escape processing for statements.
     */
    public void setEscapeProcessing( boolean enable )
    {
        escapeProcessing = enable;
    }

    /**
     * Load the sql file and then execute it
     */
    public void execute()
        throws MojoExecutionException
    {
        loadUserInfoFromSettings();

        addCommandToTransactions();

        addFilesToTransactions();

        addFileSetToTransactions();

        conn = getConnection();

        try
        {
            statement = conn.createStatement();
            statement.setEscapeProcessing( escapeProcessing );

            PrintStream out = System.out;
            try
            {
                if ( output != null )
                {
                    getLog().debug( "Opening PrintStream to output file " + output );
                    out = new PrintStream( new BufferedOutputStream( new FileOutputStream( output.getAbsolutePath(),
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
            if ( !autocommit && conn != null && onError.equals( "abort" ) )
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

        getLog().info( goodSql + " of " + totalSql + " SQL statements executed successfully" );

    }

    /**
     * Add sql command to transactions list
     *
     */
    private void addCommandToTransactions()
    {
        createTransaction().addText( sqlCommand.trim()  );
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
     * Add user input of srcFiles to transaction list
     * @throws MojoExecutionException
     */
    private void addFilesToTransactions()
        throws MojoExecutionException
    {
        for ( int i = 0; srcFiles != null && i < srcFiles.length; ++i )
        {
            if ( srcFiles[i] != null && !srcFiles[i].exists() )
            {
                throw new MojoExecutionException( srcFiles[i].getPath() + " not found." );
            }

            createTransaction().setSrc( srcFiles[i] );
        }

        if ( srcFile != null && !srcFile.exists() )
        {
            throw new MojoExecutionException( srcFile.getPath() + " not found." );
        }
        createTransaction().setSrc( srcFile );

    }

    /**
     * Load username password from settings if user has not set them in JVM properties
     */
    private void loadUserInfoFromSettings()
        throws MojoExecutionException
    {
        if ( this.settingsKey == null )
        {
            this.settingsKey = this.url;
        }
        
        if ( username == null || password == null )
        {
            Server server = this.settings.getServer( this.settingsKey );

            if ( server != null )
            {
                if ( username == null )
                {
                    username = server.getUsername();
                }

                if ( password == null )
                {
                    password = server.getPassword();
                }
            }
        }

        if ( username == null )
        {
            //allow emtpy username
            username= "";
        }

        if ( password == null )
        {
            //allow emtpy password
            password = "";
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
     * is no suitable driver or the driver fails to load.
     */
    private Connection getConnection()
        throws MojoExecutionException
    {
        try
        {

            getLog().debug( "connecting to " + url );
            Properties info = new Properties();
            info.put( "user", username );
            info.put( "password", password );
            Driver driverInstance = null;
            try
            {
                Class dc = Class.forName( driver );
                driverInstance = (Driver) dc.newInstance();
            }
            catch ( ClassNotFoundException e )
            {
                throw new MojoExecutionException( "Driver class not found: " + driver );
            }
            catch ( Exception e )
            {
                throw new MojoExecutionException( "Failure loading driver: " + driver );
            }

            Connection conn = driverInstance.connect( url, info );

            if ( conn == null )
            {
                // Driver doesn't understand the URL
                throw new SQLException( "No suitable Driver for " + url );
            }

            conn.setAutoCommit( autocommit );
            return conn;
        }
        catch ( SQLException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }

    }

    /**
     * read in lines and execute them
     */
    private void runStatements( Reader reader, PrintStream out )
        throws SQLException, IOException
    {
        StringBuffer sql = new StringBuffer();
        String line;

        BufferedReader in = new BufferedReader( reader );

        while ( ( line = in.readLine() ) != null )
        {
            if ( !keepformat )
            {
                line = line.trim();
            }
            //            line = getProject().replaceProperties(line);
            if ( !keepformat )
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

            if ( !keepformat )
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
            if ( !keepformat )
            {
                if ( line.indexOf( "--" ) >= 0 )
                {
                    sql.append( "\n" );
                }
            }
            if ( ( delimiterType.equals( DelimiterType.NORMAL ) && sql.toString().endsWith( delimiter ) )
                || ( delimiterType.equals( DelimiterType.ROW ) && line.equals( delimiter ) ) )
            {
                execSQL( sql.substring( 0, sql.length() - delimiter.length() ), out );
                sql.replace( 0, sql.length(), "" );
            }
        }
        // Catch any statements not followed by ;
        if ( !sql.equals( "" ) )
        {
            execSQL( sql.toString(), out );
        }
    }

    /**
     * Exec the sql statement.
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
            totalSql++;
            getLog().debug( "SQL: " + sql );

            boolean ret;
            int updateCount, updateCountTotal = 0;

            ret = statement.execute( sql );
            updateCount = statement.getUpdateCount();
            resultSet = statement.getResultSet();
            do
            {
                if ( !ret )
                {
                    if ( updateCount != -1 )
                    {
                        updateCountTotal += updateCount;
                    }
                }
                else
                {
                    if ( print )
                    {
                        printResults( resultSet, out );
                    }
                }
                ret = statement.getMoreResults();
                if ( ret )
                {
                    updateCount = statement.getUpdateCount();
                    resultSet = statement.getResultSet();
                }
            }
            while ( ret );

            getLog().debug( updateCountTotal + " rows affected" );

            if ( print )
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
            goodSql++;
        }
        catch ( SQLException e )
        {
            getLog().error( "Failed to execute: " + sql );
            if ( !onError.equals( "continue" ) )
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
    private void printResults( ResultSet rs, PrintStream out )
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
                for ( int col = 1; col < columnCount; col++ )
                {
                    line.append( md.getColumnName( col ) );
                    line.append( "," );
                }
                line.append( md.getColumnName( columnCount ) );
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
                    }

                    if ( first )
                    {
                        first = false;
                    }
                    else
                    {
                        line.append( "," );
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
                Reader reader = ( encoding == null ) ? new FileReader( tSrcFile )
                                                    : new InputStreamReader( new FileInputStream( tSrcFile ), encoding );
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
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername( String username )
    {
        this.username = username;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword( String password )
    {
        this.password = password;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl( String url )
    {
        this.url = url;
    }

    public String getDriver()
    {
        return driver;
    }

    public void setDriver( String driver )
    {
        this.driver = driver;
    }

    public boolean isAutocommit()
    {
        return autocommit;
    }

    public void setAutocommit( boolean autocommit )
    {
        this.autocommit = autocommit;
    }

    public Fileset getFileset()
    {
        return fileset;
    }

    public void setFileset( Fileset fileset )
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

    public int getGoodSqls()
    {
        return this.goodSql;
    }
    
    public void setOnError( String action )
    {
        this.onError = action;
    }
}
