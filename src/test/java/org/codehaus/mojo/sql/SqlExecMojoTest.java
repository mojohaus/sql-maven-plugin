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

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;

/**
 * Unit test for simple SqlExecMojo.
 */
public class SqlExecMojoTest extends AbstractMojoTestCase {

    private Properties p;

    public void setUp() throws Exception {
        super.setUp();
        p = new Properties();
        p.load(getClass().getResourceAsStream("/test.properties"));
    }

    /**
     * No error when there is no input
     */
    public void testNoCommandMojo() throws MojoExecutionException {
        SqlExecMojo mojo = createMojo();
        mojo.execute();

        assertEquals(0, mojo.getSuccessfulStatements());
    }

    public void testCreateCommandMojo() throws MojoExecutionException {
        SqlExecMojo mojo = createMojo();
        String command = "create table CREATE_COMMAND ( PERSON_ID integer, FIRSTNAME varchar, LASTNAME varchar)";
        mojo.addText(command);
        mojo.execute();

        assertEquals(1, mojo.getSuccessfulStatements());
    }

    public void testDropCommandMojo() throws MojoExecutionException {
        SqlExecMojo mojo = createMojo();
        mojo.setSqlCommand("create table DROP_TEST (id integer)");
        mojo.execute();

        mojo.clear();
        mojo.addText("drop table DROP_TEST");
        mojo.execute();

        assertEquals(1, mojo.getSuccessfulStatements());
    }

    public void testFileSetMojo() throws MojoExecutionException {

        Fileset ds = new Fileset();
        ds.setBasedir("src/test");
        ds.setIncludes(new String[] {"**/create*.sql"});
        ds.scan();
        assert (ds.getIncludedFiles().length == 1);

        SqlExecMojo mojo = createMojo();
        mojo.setFileset(ds);

        mojo.execute();

        assertEquals(3, mojo.getSuccessfulStatements());
    }

    public void testFileArrayMojo() throws MojoExecutionException {
        File[] srcFiles = new File[2];
        srcFiles[0] = new File("src/test/data/array-create-test-tables.sql");
        srcFiles[1] = new File("src/test/data/array-drop-test-tables.sql");

        SqlExecMojo mojo = createMojo();
        mojo.setSrcFiles(srcFiles);
        mojo.execute();

        assertEquals(6, mojo.getSuccessfulStatements());
    }

    /**
     * Ensure srcFiles always execute first
     */
    public void testAllMojo() throws MojoExecutionException {
        SqlExecMojo mojo = createMojo();
        String command = "create table PERSON2 ( PERSON_ID integer, FIRSTNAME varchar, LASTNAME varchar)";
        mojo.addText(command);

        File[] srcFiles = new File[1];
        srcFiles[0] = new File("src/test/data/create-test-tables.sql");
        mojo.setSrcFiles(srcFiles);

        Fileset ds = new Fileset();
        ds.setBasedir("src/test");
        ds.setIncludes(new String[] {"**/drop*.sql"});
        ds.scan();
        mojo.setFileset(ds);
        mojo.execute();

        assertEquals(7, mojo.getSuccessfulStatements());
    }

    public void testOrderFile() throws MojoExecutionException {
        Fileset ds = new Fileset();
        ds.setBasedir("src/test");
        ds.setIncludes(new String[] {"**/order-drop*.sql", "**/order-create*.sql"});
        ds.scan();

        SqlExecMojo mojo = createMojo();
        mojo.setFileset(ds);
        mojo.setOrderFile(SqlExecMojo.FILE_SORTING_ASC);
        mojo.execute();

        assertEquals(6, mojo.getSuccessfulStatements());

        try {
            mojo.setOrderFile(SqlExecMojo.FILE_SORTING_DSC);
            mojo.execute();
            fail("Execution is not aborted on error.");
        } catch (MojoExecutionException e) {
        }
    }

    public void testOnErrorContinueMojo() throws MojoExecutionException {
        SqlExecMojo mojo = createMojo();
        String command = "create table BOGUS"; // bad syntax
        mojo.addText(command);
        mojo.setOnError("continue");
        mojo.execute();
        assertEquals(0, mojo.getSuccessfulStatements());
    }

    public void testOnErrorAbortMojo() throws MojoExecutionException {
        SqlExecMojo mojo = createMojo();
        String command = "create table BOGUS"; // bad syntax
        mojo.addText(command);

        try {
            mojo.execute();
            fail("Execution is not aborted on error.");

        } catch (MojoExecutionException e) {

        }

        assertEquals(0, mojo.getSuccessfulStatements());
    }

    public void testOnErrorAbortAfterMojo() throws MojoExecutionException {
        String commands = "create table BOGUS"; // bad syntax

        SqlExecMojo mojo = createMojo();
        mojo.addText(commands);

        File[] srcFiles = new File[1];
        srcFiles[0] = new File("src/test/data/invalid-syntax.sql");

        assertTrue(srcFiles[0].exists());

        mojo.setSrcFiles(srcFiles);
        mojo.setOnError("abortAfter");

        try {
            mojo.execute();
            fail("Execution is not aborted on error.");

        } catch (MojoExecutionException e) {
            // expected
        }

        assertEquals(0, mojo.getSuccessfulStatements());
        assertEquals(2, mojo.getTotalStatements());
    }

    public void testDefaultUsernamePassword() throws MojoExecutionException {

        Settings settings = new Settings();
        Server server = new Server();
        settings.addServer(server);

        SqlExecMojo mojo = createMojo();
        mojo.setSettings(settings);

        // force a lookup of username
        mojo.setUsername(null);
        mojo.setPassword(null);

        mojo.execute();

        assertEquals("", mojo.getUsername());
        assertEquals("", mojo.getPassword());
    }

    public void testUsernamePasswordLookup() throws MojoExecutionException {

        Settings settings = new Settings();
        Server server = new Server();
        server.setId("somekey");
        server.setUsername("username");
        server.setPassword("password");
        settings.addServer(server);

        SqlExecMojo mojo = createMojo();
        mojo.setSettings(settings);

        // force a lookup of username
        mojo.setSettingsKey("somekey");
        mojo.setUsername(null);
        mojo.setPassword(null);

        mojo.execute();

        assertEquals("username", mojo.getUsername());
        assertEquals("password", mojo.getPassword());
    }

    public void testBadDriver() {
        SqlExecMojo mojo = createMojo();
        mojo.setDriver("bad-driver");
        try {
            mojo.execute();

            fail("Bad driver is not detected");
        } catch (MojoExecutionException e) {

        }
    }

    public void testBadUrl() {
        SqlExecMojo mojo = createMojo();
        mojo.setUrl("bad-url");
        try {
            mojo.execute();

            fail("Bad URL is not detected");
        } catch (MojoExecutionException e) {

        }
    }

    public void testBadFile() {
        SqlExecMojo mojo = createMojo();
        File[] srcFiles = new File[1];
        srcFiles[0] = new File("a-every-bogus-file-that-does-not-exist");

        mojo.setSrcFiles(srcFiles);
        try {
            mojo.execute();

            fail("Bad files is not detected");
        } catch (MojoExecutionException e) {

        }
    }

    public void testOnError() {
        SqlExecMojo mojo = createMojo();
        mojo.setOnError("AbOrT");
        assertEquals(SqlExecMojo.ON_ERROR_ABORT, mojo.getOnError());
        mojo.setOnError("cOnTiNuE");
        assertEquals(SqlExecMojo.ON_ERROR_CONTINUE, mojo.getOnError());
        try {
            mojo.setOnError("bad");
            fail(IllegalArgumentException.class.getName() + " was not thrown.");
        } catch (IllegalArgumentException e) {
            // expected
        }
        try {
            mojo.setOnError(null);
            fail(IllegalArgumentException.class.getName() + " was not thrown.");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    public void testSkip() throws MojoExecutionException {
        String command = "create table PERSON ( PERSON_ID integer, FIRSTNAME varchar, LASTNAME varchar)";
        SqlExecMojo mojo = createMojo();
        mojo.addText(command);
        mojo.setSkip(true);
        mojo.execute();

        // no command was executed due to skip is on
        assertEquals(0, mojo.getSuccessfulStatements());
    }

    public void testDriverProperties() throws MojoExecutionException {
        SqlExecMojo mojo = createMojo();
        Properties driverProperties = mojo.getDriverProperties();
        assertEquals(2, driverProperties.size());
        assertEquals("value1", driverProperties.get("key1"));
        assertEquals("value2", driverProperties.get("key2"));

        mojo.setDriverProperties("key1=value1,key2");
        try {
            driverProperties = mojo.getDriverProperties();
        } catch (MojoExecutionException e) {
        }
    }

    public void testBlockMode() throws MojoExecutionException {
        String command = "create table BLOCKTABLE ( PERSON_ID integer, FIRSTNAME varchar, LASTNAME varchar)";

        SqlExecMojo mojo = createMojo();
        mojo.addText(command);
        // TODO: Check if this is equal mojo.setEnableBlockMode( true );
        // to the following:
        mojo.setDelimiter(DelimiterType.ROW);
        mojo.execute();
        assertEquals(1, mojo.getSuccessfulStatements());

        mojo.setSqlCommand("");
        mojo.getTransactions().clear();
        command = "drop table BLOCKTABLE";
        mojo.addText(command);
        mojo.execute();
        assertEquals(1, mojo.getSuccessfulStatements());
    }

    public void testKeepFormat() throws MojoExecutionException {
        // Normally a line starting in -- would be ignored, but with keepformat mode
        // on it will not.
        String command = "--create table PERSON ( PERSON_ID integer, FIRSTNAME varchar, LASTNAME varchar)";

        SqlExecMojo mojo = createMojo();
        mojo.addText(command);
        mojo.setKeepFormat(true);

        try {
            mojo.execute();
            fail("-- at the start of the SQL command is ignored.");
        } catch (MojoExecutionException e) {
        }

        assertEquals(0, mojo.getSuccessfulStatements());
    }

    public void testBadDelimiter() throws Exception {
        String command = "create table SEPARATOR ( PERSON_ID integer, FIRSTNAME varchar, LASTNAME varchar):"
                + "create table SEPARATOR2 ( PERSON_ID integer, FIRSTNAME varchar, LASTNAME varchar)";

        SqlExecMojo mojo = createMojo();
        mojo.addText(command);
        mojo.setDelimiter(":");

        try {
            mojo.execute();
            fail("Expected parser error.");
        } catch (MojoExecutionException e) {
        }
    }

    public void testGoodDelimiter() throws Exception {
        String command = "create table SEPARATOR ( PERSON_ID integer, FIRSTNAME varchar, LASTNAME varchar)\n:\n"
                + "create table SEPARATOR2 ( PERSON_ID integer, FIRSTNAME varchar, LASTNAME varchar)";

        SqlExecMojo mojo = createMojo();
        mojo.addText(command);
        mojo.setDelimiter(":");

        mojo.execute();

        assertEquals(2, mojo.getSuccessfulStatements());
    }

    public void testBadDelimiterType() throws Exception {
        String command = "create table BADDELIMTYPE ( PERSON_ID integer, FIRSTNAME varchar, LASTNAME varchar)" + "\n:"
                + "create table BADDELIMTYPE2 ( PERSON_ID integer, FIRSTNAME varchar, LASTNAME varchar)";

        SqlExecMojo mojo = createMojo();
        mojo.addText(command);
        mojo.setDelimiter(":");
        mojo.setDelimiterType(DelimiterType.ROW);

        try {
            mojo.execute();
            fail("Expected parser error.");
        } catch (MojoExecutionException e) {
        }
    }

    public void testGoodDelimiterType() throws Exception {
        String command = "create table GOODDELIMTYPE ( PERSON_ID integer, FIRSTNAME varchar, LASTNAME varchar)"
                + "\n:  \n" + "create table GOODDELIMTYPE2 ( PERSON_ID integer, FIRSTNAME varchar, LASTNAME varchar)";

        SqlExecMojo mojo = createMojo();
        mojo.addText(command);
        mojo.setDelimiter(":");
        mojo.setDelimiterType(DelimiterType.NORMAL);

        mojo.execute();
        assertEquals(2, mojo.getSuccessfulStatements());
    }

    public void testOutputFile() throws Exception {
        String basedir = System.getProperty("basedir", ".");
        File outputFile = new File(basedir, "target/sql.out");
        outputFile.delete();

        SqlExecMojo mojo = createMojo();
        mojo.setOutputFile(outputFile);
        mojo.setPrintResultSet(true);

        String command = "create table GOODDELIMTYPE3 ( PERSON_ID integer, FIRSTNAME varchar, LASTNAME varchar);";
        mojo.addText(command);
        mojo.execute();

        List<String> list = Files.readAllLines(outputFile.toPath(), StandardCharsets.UTF_8);
        assertEquals(1, list.size());
        assertEquals("0 rows affected", list.get(0));

        mojo.clear();
        mojo.setSqlCommand("insert into GOODDELIMTYPE3 (person_id) values (1)");
        mojo.execute();

        list = Files.readAllLines(outputFile.toPath(), StandardCharsets.UTF_8);
        assertEquals(1, list.size());
        assertEquals("1 rows affected", list.get(0));

        mojo.clear();
        mojo.setSqlCommand("select * from GOODDELIMTYPE3");
        mojo.execute();

        list = Files.readAllLines(outputFile.toPath(), StandardCharsets.UTF_8);
        assertEquals(4, list.size());
        assertEquals("PERSON_ID,FIRSTNAME,LASTNAME", list.get(0));
        assertEquals("1,null,null", list.get(1));
        assertEquals("", list.get(2));
        assertEquals("0 rows affected", list.get(3));
    }

    // MSQL-44
    public void testAnonymousBlock() throws MojoExecutionException {
        String command = "--/ Anonymous SQL Block\n"
                + "create table ANONBLOCKTABLE ( PERSON_ID integer, FIRSTNAME varchar, LASTNAME varchar)\n" + "/\n"
                + "drop table ANONBLOCKTABLE";

        SqlExecMojo mojo = createMojo();
        mojo.setDelimiter("/");
        mojo.addText(command);
        mojo.execute();
        assertEquals(2, mojo.getSuccessfulStatements());
    }

    public void testSkipMissingFiles() throws MojoExecutionException {
        SqlExecMojo mojo = createMojo();
        mojo.setSkipMissingFiles(true);
        mojo.setSrcFiles(new File[] {
            new File("src/test/data/x-create-test-tables.sql"),
            new File("non/existing/file/path.sql"),
            new File("src/test/data/x-drop-test-tables.sql")
        });
        mojo.execute();
        assertEquals(2, mojo.getSuccessfulStatements());
    }

    public void testValidationSql() {
        SqlExecMojo mojo = createMojo();
        mojo.setConnectionValidationSqls(Arrays.asList("select 1 from nowhere"));
        try {
            mojo.execute();

            fail("Invalid connection is not detected");
        } catch (MojoExecutionException e) {

        }

        assertTrue(mojo.isConnectionClosed());
    }

    public void testConnectionRetryCountOnValidationError() {
        SqlExecMojo mojo = createMojo();
        mojo.setConnectionValidationSqls(Arrays.asList("select 1 from nowhere"));
        mojo.setConnectionRetryCount(5);
        try {
            mojo.execute();

            fail("Invalid connection is not detected");
        } catch (MojoExecutionException e) {

        }

        assertTrue(mojo.getConnectionRetryAttempts() >= 5);
        assertTrue(mojo.isConnectionClosed());
    }

    public void testConnectionRetryCountOnConnectionError() {
        SqlExecMojo mojo = createMojo();
        mojo.setUrl("bad-url");
        mojo.setConnectionRetryCount(5);
        try {
            mojo.execute();

            fail("Invalid connection is not detected");
        } catch (MojoExecutionException e) {

        }

        assertTrue(mojo.getConnectionRetryAttempts() >= 5);
        assertTrue(mojo.isConnectionClosed());
    }

    public void testConnectionRetryIntervalC2I2() {
        SqlExecMojo mojo = createMojo();
        mojo.setUrl("no-db-here");
        mojo.setConnectionRetryCount(2);
        mojo.setConnectionRetryInterval(2);

        long start = System.currentTimeMillis();

        try {
            mojo.execute();
            fail("Invalid connection is not detected");
        } catch (MojoExecutionException e) {
            long end = System.currentTimeMillis();
            assertTrue((end - start) >= 4000);
        }
    }

    public void testConnectionRetryIntervalC1I3() {
        SqlExecMojo mojo = createMojo();
        mojo.setUrl("no-db-here");
        mojo.setConnectionRetryCount(1);
        mojo.setConnectionRetryInterval(3);

        long start = System.currentTimeMillis();

        try {
            mojo.execute();
            fail("Invalid connection is not detected");
        } catch (MojoExecutionException e) {
            long end = System.currentTimeMillis();
            assertTrue((end - start) >= 3000);
        }
    }

    public void testConnectionRetryOnConnectionError() {
        SqlExecMojo mojo = createMojo();
        final String url = mojo.getUrl();
        mojo.setUrl("bad-url");
        mojo.setConnectionRetryCount(5);
        mojo.setConnectionRetryInterval(1);

        new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // Wait for three connection attempts
                            Thread.sleep(3000);
                        } catch (InterruptedException ex) {
                            // ignore
                        }
                        mojo.setUrl(url); // Url fixed, next connection should be success
                    }
                })
                .start();

        try {
            mojo.execute();

        } catch (MojoExecutionException e) {
            fail("Connection was not restored");
        }

        assertTrue(mojo.getConnectionRetryAttempts() >= 3);
        assertTrue(mojo.isConnectionClosed());
    }

    public void testConnectionRetryOnValidationError() {
        SqlExecMojo mojo = createMojo();
        mojo.addText("select * from test32");
        mojo.setConnectionValidationSqls(Arrays.asList("select 1 from test32"));
        mojo.setConnectionRetryCount(5);
        mojo.setConnectionRetryInterval(1);

        runSqlAfter(mojo, 3, Arrays.asList("create table test32 ( ID integer)"));

        try {
            mojo.execute();

        } catch (MojoExecutionException e) {
            fail("Connection was not restored");
        }

        assertTrue(mojo.getConnectionRetryAttempts() >= 3);
        assertTrue(mojo.isConnectionClosed());
    }

    public void testConnectionRetryOnValidationErrorMultipleValidationSql() {
        SqlExecMojo mojo = createMojo();
        mojo.addText("select * from con_retry");
        mojo.setConnectionValidationSqls(Arrays.asList("select 1 from con_retry", "select 1 from con_retry2"));
        mojo.setConnectionRetryCount(5);
        mojo.setConnectionRetryInterval(1);

        runSqlAfter(mojo, 1, Arrays.asList("create table con_retry ( ID integer)"));
        runSqlAfter(mojo, 2, Arrays.asList("create table con_retry2 ( ID integer)"));

        try {
            mojo.execute();

        } catch (MojoExecutionException e) {
            fail("Connection not was restored");
        }

        assertTrue(mojo.getConnectionRetryAttempts() >= 2);
        assertTrue(mojo.isConnectionClosed());
    }

    public void testValidationErrorMultipleValidationSql() {
        SqlExecMojo mojo = createMojo();
        mojo.addText("select * from valid_err");
        mojo.setConnectionValidationSqls(Arrays.asList("select 1 from valid_err", "select 1 from not"));
        mojo.setConnectionRetryCount(3);
        mojo.setConnectionRetryInterval(1);

        runSqlAfter(mojo, 1, Arrays.asList("create table valid_err ( ID integer)"));

        try {
            mojo.execute();

            fail("Connection was restored");
        } catch (MojoExecutionException e) {
        }

        assertTrue(mojo.getConnectionRetryAttempts() >= 3);
        assertTrue(mojo.isConnectionClosed());
    }

    public void testCustomPrintResultSet() throws Exception {
        CustomSqlExecMojo customMojo = new CustomSqlExecMojo();
        setUp(customMojo);

        String basedir = System.getProperty("basedir", ".");
        File outputFile = new File(basedir, "target/custom-print-resultset.out");
        outputFile.delete();
        customMojo.setOutputFile(outputFile);
        customMojo.setPrintResultSet(true);

        String command = "create table CUSTOM_PRINT ( PERSON_ID integer, FIRSTNAME varchar, LASTNAME varchar)";
        customMojo.addText(command);
        customMojo.execute();

        List<String> list = Files.readAllLines(outputFile.toPath(), StandardCharsets.UTF_8);
        assertEquals(1, list.size());
        assertEquals("0 cows affected", list.get(0));

        customMojo.clear();
        customMojo.setSqlCommand("insert into CUSTOM_PRINT (person_id) values (1)");
        customMojo.execute();

        list = Files.readAllLines(outputFile.toPath(), StandardCharsets.UTF_8);
        assertEquals(1, list.size());
        assertEquals("1 cows affected", list.get(0));

        customMojo.clear();
        customMojo.setSqlCommand("select * from CUSTOM_PRINT");
        customMojo.execute();

        list = Files.readAllLines(outputFile.toPath(), StandardCharsets.UTF_8);
        assertEquals(2, list.size());
        assertEquals("This is the way", list.get(0));
        assertEquals("0 cows affected", list.get(1));
    }

    private SqlExecMojo createMojo() {
        SqlExecMojo mojo = new SqlExecMojo();
        setUp(mojo);
        return mojo;
    }

    private void setUp(SqlExecMojo mojoToSetup) {

        // populate parameters
        mojoToSetup.setDriver(p.getProperty("driver"));
        mojoToSetup.setUsername(p.getProperty("user"));
        mojoToSetup.setPassword(p.getProperty("password"));
        mojoToSetup.setUrl(p.getProperty("url"));
        mojoToSetup.setDriverProperties(p.getProperty("driverProperties"));
        mojoToSetup.setSqlCommand(null);
        mojoToSetup.setDelimiter(
                SqlExecMojo.DEFAULT_DELIMITER); // This will simulate the defaultValue of @Parameter (...)
        mojoToSetup.setOnError(SqlExecMojo.ON_ERROR_ABORT);
        mojoToSetup.setDelimiterType(DelimiterType.NORMAL);
        mojoToSetup.setEscapeProcessing(true);
        mojoToSetup.setOutputDelimiter(",");

        try {
            MavenFileFilter filter =
                    (MavenFileFilter) lookup("org.apache.maven.shared.filtering.MavenFileFilter", "default");
            mojoToSetup.setFileFilter(filter);

            SecDispatcher securityDispatcher =
                    (SecDispatcher) lookup("org.sonatype.plexus.components.sec.dispatcher.SecDispatcher", "default");
            mojoToSetup.setSecurityDispatcher(securityDispatcher);

            MavenProject project = new MavenProjectStub();
            setVariableValueToObject(mojoToSetup, "project", project);
        } catch (ComponentLookupException | IllegalAccessException e) {
            throw new RuntimeException("Failed to setup mojo: " + e.getMessage(), e);
        }
    }

    public void testOutputFileEncoding() throws Exception {
        String command = "create table ENCODING ( PERSON_ID integer, FIRSTNAME varchar, LASTNAME varchar);\n"
                + "insert into ENCODING (PERSON_ID, FIRSTNAME, LASTNAME) values (1, 'A', 'B');";

        SqlExecMojo mojo = createMojo();
        mojo.addText(command);

        String basedir = System.getProperty("basedir", ".");
        File outputFile = new File(basedir, "target/sql-encoding.out");
        outputFile.delete();
        mojo.setOutputFile(outputFile);
        mojo.setPrintResultSet(true);

        mojo.execute();

        assertTrue("Output file: " + outputFile + " not found.", outputFile.exists());
        assertTrue("Unexpected empty output file. ", outputFile.length() > 0);

        List<String> list = Files.readAllLines(outputFile.toPath(), StandardCharsets.UTF_8);
        assertTrue(list.size() == 2);
        assertEquals("0 rows affected", list.get(0));
        assertEquals("1 rows affected", list.get(1));
    }

    public void testOutputFileEncodingUtf16() throws Exception {
        String command = "create table ENCODING_UTF_16 ( PERSON_ID integer, FIRSTNAME varchar, LASTNAME varchar);\n"
                + "insert into ENCODING_UTF_16 (PERSON_ID, FIRSTNAME, LASTNAME) values (1, 'A', 'B');";

        SqlExecMojo mojo = createMojo();
        mojo.addText(command);

        String basedir = System.getProperty("basedir", ".");
        File outputFile = new File(basedir, "target/sql-encoding-utf-16.out");
        outputFile.delete();
        mojo.setOutputFile(outputFile);
        mojo.setOutputEncoding("UTF-16");
        mojo.setPrintResultSet(true);

        mojo.execute();

        assertTrue("Output file: " + outputFile + " not found.", outputFile.exists());
        assertTrue("Unexpected empty output file. ", outputFile.length() > 0);

        List<String> list = Files.readAllLines(outputFile.toPath(), StandardCharsets.UTF_16);
        assertTrue(list.size() == 2);
        assertEquals("0 rows affected", list.get(0));
        assertEquals("1 rows affected", list.get(1));
    }

    private void runSqlAfter(SqlExecMojo mojo, int secs, final List<String> sqls) {
        new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // Wait for three connection attempts
                            Thread.sleep(secs * 1000);
                        } catch (InterruptedException ex) {
                        }

                        Connection conn = null;

                        try {
                            conn = mojo.getConnection();

                            try (Statement stmt = conn.createStatement()) {
                                for (String sql : sqls) {
                                    stmt.execute(sql);
                                }
                            }
                        } catch (Exception e) {
                            fail(e.getMessage());
                        } finally {
                            mojo.closeConnection();
                        }
                    }
                })
                .start();
    }
}
