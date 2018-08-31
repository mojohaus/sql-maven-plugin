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

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.logging.Logger;

import junit.framework.TestCase;

public class SqlSplitterTest extends TestCase {

    public void testContainsSqlString() throws Exception {
        containsNot("");
        containsNot(" ");
        containsNot("  \t  ");

        contains(";", 1);
        contains("SELECT * from myTable;", 22);

        contains("SELECT * from myTable; -- with sl comment", 22);
        contains("SELECT * from myTable; /* with part comment */", 22);

        contains("SELECT * from myTable /* with part comment inside*/  ; ", 54);

        contains("SELECT * from myTable /* with ; semicolon*/  ; ", 46);

        contains("INSERT INTO testTable (thevalue) VALUES ('value  !'); -- comment at the end", 53);

        // a " inside a ' quoted text
        contains("INSERT INTO testTable (thevalue) VALUES ('value \"  !');", 55);

        // a ' inside a " quoted text
        contains("INSERT INTO testTable (thevalue) VALUES (\"value '  !\");", 55);

        contains("INSERT INTO testTable (thevalue) VALUES (\"value --  \");", 55);
        contains("INSERT INTO testTable (thevalue) VALUES ('value --  ');", 55);

        containsNot("SELECT * from myTable where value = ';' AND -- semicolon is quoted!");

        contains("INSERT INTO testTable (thevalue) VALUES (' text '' other ');", 60);

        //
        contains("INSERT INTO testTable (thevalue) VALUES ('value  !'); -- comment with ' single quote", 53);
        contains("SELECT * from myTable /* comment with ' single quote */  ; ", 58);
    }

    /**
     * This unit test is meant for checking the performance with a profiler
     */
    public void testSplitterPerformance() throws Exception {
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            contains(
                    "INSERT INTO testTable (thevalue1, thevalue2, anothervalue3, justmore, andevenmore) "
                            + "/*it might also contain lots of other really loooooong and useless comments...*/ "
                            + "/*it might also contain lots of other really loooooong and useless comments...*/ "
                            + "/*it might also contain lots of other really loooooong and useless comments...*/ "
                            + "/*it might also contain lots of other really loooooong and useless comments...*/ "
                            + "/*it might also contain lots of other really loooooong and useless comments...*/ "
                            + "/*it might also contain lots of other really loooooong and useless comments...*/ "
                            + "/*it might also contain lots of other really loooooong and useless comments...*/ "
                            + "/*it might also contain lots of other really loooooong and useless comments...*/ "
                            + "/*it might also contain lots of other really loooooong and useless comments...*/ "
                            + "/*it might also contain lots of other really loooooong and useless comments...*/ "
                            + "/*it might also contain lots of other really loooooong and useless comments...*/ "
                            + "/*it might also contain lots of other really loooooong and useless comments...*/ "
                            + "/*it might also contain lots of other really loooooong and useless comments...*/ "
                            + "/*it might also contain lots of other really loooooong and useless comments...*/ "
                            + "/*it might also contain lots of other really loooooong and useless comments...*/ "
                            + "/*it might also contain lots of other really loooooong and useless comments...*/ "
                            + "/*it might also contain lots of other really loooooong and useless comments...*/ "
                            + "/*it might also contain lots of other really loooooong and useless comments...*/ "
                            + "/*it might also contain lots of other really loooooong and useless comments...*/ "
                            + "/*it might also contain lots of other really loooooong and useless comments...*/ "
                            + "/*it might also contain lots of other really loooooong and useless comments...*/ "
                            + "/*it might also contain lots of other really loooooong and useless comments...*/ "
                            + "/*it might also contain lots of other really loooooong and useless comments...*/ "
                            + "/*it might also contain lots of other really loooooong and useless comments...*/ "
                            + "/*it might also contain lots of other really loooooong and useless comments...*/ "
                            + "/*it might also contain lots of other really loooooong and useless comments...*/ "
                            + "/*it might also contain lots of other really loooooong and useless comments...*/ "
                            + "/*it might also contain lots of other really loooooong and useless comments...*/ "
                            + "/*it might also contain lots of other really loooooong and useless comments...*/ "
                            + "/*it might also contain lots of other really loooooong and useless comments...*/ "
                            + "/*it might also contain lots of other really loooooong and useless comments...*/ "
                            + "/*it might also contain lots of other really loooooong and useless comments...*/ "
                            + "/*it might also contain lots of other really loooooong and useless comments...*/ "
                            + "/*it might also contain lots of other really loooooong and useless comments...*/ "
                            + "/*it might also contain lots of other really loooooong and useless comments...*/ "
                            + "/*it might also contain lots of other really loooooong and useless comments...*/ "
                            + "/*it might also contain lots of other really loooooong and useless comments...*/ "
                            + "/*it might also contain lots of other really loooooong and useless comments...*/ "
                            + "/*it might also contain lots of other really loooooong and useless comments...*/ "
                            + "/*it might also contain lots of other really loooooong and useless comments...*/ "
                            + "/*it might also contain lots of other really loooooong and useless comments...*/ "
                            + "/*it might also contain lots of other really loooooong and useless comments...*/ "
                            + "/*it might also contain lots of other really loooooong and useless comments...*/ "
                            + "/*it might also contain lots of other really loooooong and useless comments...*/ "
                            + "/*it might also contain lots of other really loooooong and useless comments...*/ "
                            + "/*it might also contain lots of other really loooooong and useless comments...*/ "
                            + "VALUES ('value !', 'else', 'more', 'hopefullyfast');",
                    3861);
        }

        long duration = System.currentTimeMillis() - startTime;
        Logger log = Logger.getLogger(SqlSplitterTest.class.getName());
        log.info("SqlPlitterTest performance took [ms]: " + duration);
    }

    public void testMsSQLStrings() throws Exception {
        String del = "GO";

        containsNot("SELECT COUNT(*) FROM Logs", del);
        containsNot("SELECT * FROM TPersons", del);
        contains("GO", del, 2);
    }

    // MSQL-48
    public void testSQLContainingRegExp() throws Exception {
        String sql = "EXECUTE IMMEDIATE 'PROCEDURE my_sproc(' ||\r\n"
                + "'    ...' ||\r\n" + "'    ...' ||\r\n"
                + "'  ...REGEXP_INSTR(v_foo, '''^[A-Za-z0-9]{2}[0-9]{3,4}$''') ...' ||\r\n"
                + "'...' ||\r\n"
                + "'EXCEPTION' ||\r\n" + "'WHEN OTHERS THEN' ||\r\n"
                + "'  DBMS_OUTPUT.put_line (''Error stack at top level:'');' ||\r\n"
                + "'  putline (DBMS_UTILITY.format_error_backtrace);' ||\r\n"
                + "'  bt.show_info (DBMS_UTILITY.format_error_backtrace);' ||\r\n"
                + "'END my_sproc;'";
        BufferedReader in = new BufferedReader(new StringReader(sql));

        // Only checking if this complex statement can be parsed
        String line;
        int lineNr = 0;
        for (; (line = in.readLine()) != null; lineNr++) {
            SqlSplitter.containsSqlEnd(line, ";", SqlSplitter.NO_END);
        }
        assertEquals("Not every line is parsed", 11, lineNr);
    }

    public void testOverflows() {
        assertEquals(
                SqlSplitter.OVERFLOW_SINGLE_QUOTE,
                SqlSplitter.containsSqlEnd("test 'with an open singlequote statics;", ";", SqlSplitter.NO_END));
        assertEquals(
                SqlSplitter.OVERFLOW_SINGLE_QUOTE,
                SqlSplitter.containsSqlEnd("test 'with an open singlequote statics;lalala", ";", SqlSplitter.NO_END));

        assertEquals(
                SqlSplitter.OVERFLOW_DOUBLE_QUOTE,
                SqlSplitter.containsSqlEnd("test \"with an open doublequote statics;", ";", SqlSplitter.NO_END));
        assertEquals(
                SqlSplitter.OVERFLOW_DOUBLE_QUOTE,
                SqlSplitter.containsSqlEnd("test \"with an open doublequote statics;lalala", ";", SqlSplitter.NO_END));

        assertEquals(
                39,
                SqlSplitter.containsSqlEnd(
                        "test \"with an open doublequote statics;", ";", SqlSplitter.OVERFLOW_DOUBLE_QUOTE));

        assertEquals(
                SqlSplitter.OVERFLOW_SINGLE_QUOTE,
                SqlSplitter.containsSqlEnd(
                        "test \"with an open doublequote statics;", ";", SqlSplitter.OVERFLOW_SINGLE_QUOTE));
        assertEquals(
                40,
                SqlSplitter.containsSqlEnd(
                        "test \"with an open doublequote statics';", ";", SqlSplitter.OVERFLOW_SINGLE_QUOTE));

        assertEquals(
                SqlSplitter.OVERFLOW_DOUBLE_QUOTE,
                SqlSplitter.containsSqlEnd(
                        "test 'with an open singlequote statics;", ";", SqlSplitter.OVERFLOW_DOUBLE_QUOTE));
        assertEquals(
                40,
                SqlSplitter.containsSqlEnd(
                        "test 'with an open singlequote statics\";", ";", SqlSplitter.OVERFLOW_DOUBLE_QUOTE));

        assertEquals(
                SqlSplitter.OVERFLOW_COMMENT, SqlSplitter.containsSqlEnd("test /* comment;", ";", SqlSplitter.NO_END));
        assertEquals(
                SqlSplitter.OVERFLOW_COMMENT,
                SqlSplitter.containsSqlEnd("comment; continued", ";", SqlSplitter.OVERFLOW_COMMENT));
        assertEquals(16, SqlSplitter.containsSqlEnd("test */ comment;", ";", SqlSplitter.OVERFLOW_COMMENT));

        // test value divided over 2 lines, second line hits a comment first
        assertEquals(
                SqlSplitter.OVERFLOW_SINGLE_QUOTE,
                SqlSplitter.containsSqlEnd("INSERT INTO topics VALUES( 'did you know: ", ";", SqlSplitter.NO_END));
        assertEquals(
                33,
                SqlSplitter.containsSqlEnd(
                        "javadoc always starts with /**');", ";", SqlSplitter.OVERFLOW_SINGLE_QUOTE));

        // bare minimums
        assertEquals(SqlSplitter.NO_END, SqlSplitter.containsSqlEnd("'", ";", SqlSplitter.OVERFLOW_SINGLE_QUOTE));
        assertEquals(SqlSplitter.NO_END, SqlSplitter.containsSqlEnd("\"", ";", SqlSplitter.OVERFLOW_DOUBLE_QUOTE));
        assertEquals(SqlSplitter.NO_END, SqlSplitter.containsSqlEnd("*/", ";", SqlSplitter.OVERFLOW_COMMENT));

        assertEquals(SqlSplitter.OVERFLOW_SINGLE_QUOTE, SqlSplitter.containsSqlEnd("'", ";", SqlSplitter.NO_END));
        assertEquals(SqlSplitter.OVERFLOW_DOUBLE_QUOTE, SqlSplitter.containsSqlEnd("\"", ";", SqlSplitter.NO_END));
        assertEquals(SqlSplitter.OVERFLOW_COMMENT, SqlSplitter.containsSqlEnd("/*", ";", SqlSplitter.NO_END));

        assertEquals(
                SqlSplitter.OVERFLOW_SINGLE_QUOTE,
                SqlSplitter.containsSqlEnd("", ";", SqlSplitter.OVERFLOW_SINGLE_QUOTE));
        assertEquals(
                SqlSplitter.OVERFLOW_SINGLE_QUOTE,
                SqlSplitter.containsSqlEnd("\"", ";", SqlSplitter.OVERFLOW_SINGLE_QUOTE));
        assertEquals(
                SqlSplitter.OVERFLOW_SINGLE_QUOTE,
                SqlSplitter.containsSqlEnd("/*", ";", SqlSplitter.OVERFLOW_SINGLE_QUOTE));
        assertEquals(
                SqlSplitter.OVERFLOW_SINGLE_QUOTE,
                SqlSplitter.containsSqlEnd("*/", ";", SqlSplitter.OVERFLOW_SINGLE_QUOTE));
        assertEquals(
                SqlSplitter.OVERFLOW_SINGLE_QUOTE,
                SqlSplitter.containsSqlEnd("''", ";", SqlSplitter.OVERFLOW_SINGLE_QUOTE));
        assertEquals(
                SqlSplitter.OVERFLOW_SINGLE_QUOTE,
                SqlSplitter.containsSqlEnd("\"\"", ";", SqlSplitter.OVERFLOW_SINGLE_QUOTE));

        assertEquals(
                SqlSplitter.OVERFLOW_DOUBLE_QUOTE,
                SqlSplitter.containsSqlEnd("", ";", SqlSplitter.OVERFLOW_DOUBLE_QUOTE));
        assertEquals(
                SqlSplitter.OVERFLOW_DOUBLE_QUOTE,
                SqlSplitter.containsSqlEnd("'", ";", SqlSplitter.OVERFLOW_DOUBLE_QUOTE));
        assertEquals(
                SqlSplitter.OVERFLOW_DOUBLE_QUOTE,
                SqlSplitter.containsSqlEnd("/*", ";", SqlSplitter.OVERFLOW_DOUBLE_QUOTE));
        assertEquals(
                SqlSplitter.OVERFLOW_DOUBLE_QUOTE,
                SqlSplitter.containsSqlEnd("*/", ";", SqlSplitter.OVERFLOW_DOUBLE_QUOTE));
        assertEquals(
                SqlSplitter.OVERFLOW_DOUBLE_QUOTE,
                SqlSplitter.containsSqlEnd("''", ";", SqlSplitter.OVERFLOW_DOUBLE_QUOTE));
        assertEquals(
                SqlSplitter.OVERFLOW_DOUBLE_QUOTE,
                SqlSplitter.containsSqlEnd("\"\"", ";", SqlSplitter.OVERFLOW_DOUBLE_QUOTE));

        assertEquals(SqlSplitter.OVERFLOW_COMMENT, SqlSplitter.containsSqlEnd("", ";", SqlSplitter.OVERFLOW_COMMENT));
        assertEquals(SqlSplitter.OVERFLOW_COMMENT, SqlSplitter.containsSqlEnd("'", ";", SqlSplitter.OVERFLOW_COMMENT));
        assertEquals(SqlSplitter.OVERFLOW_COMMENT, SqlSplitter.containsSqlEnd("\"", ";", SqlSplitter.OVERFLOW_COMMENT));
        assertEquals(SqlSplitter.OVERFLOW_COMMENT, SqlSplitter.containsSqlEnd("/*", ";", SqlSplitter.OVERFLOW_COMMENT));
        assertEquals(SqlSplitter.OVERFLOW_COMMENT, SqlSplitter.containsSqlEnd("''", ";", SqlSplitter.OVERFLOW_COMMENT));
        assertEquals(
                SqlSplitter.OVERFLOW_COMMENT, SqlSplitter.containsSqlEnd("\"\"", ";", SqlSplitter.OVERFLOW_COMMENT));

        // escaped escape character
        assertEquals(SqlSplitter.NO_END, SqlSplitter.containsSqlEnd("\\\\'", ";", SqlSplitter.OVERFLOW_SINGLE_QUOTE));
        assertEquals(
                SqlSplitter.OVERFLOW_SINGLE_QUOTE,
                SqlSplitter.containsSqlEnd("\\'", ";", SqlSplitter.OVERFLOW_SINGLE_QUOTE));
    }

    public void testAlphaDelimiter() throws Exception {
        assertEquals(2, SqlSplitter.containsSqlEnd("go", "go", SqlSplitter.NO_END));
        assertEquals(2, SqlSplitter.containsSqlEnd("Go", "Go", SqlSplitter.NO_END));
        assertEquals(5, SqlSplitter.containsSqlEnd("   GO", "GO", SqlSplitter.NO_END));
        assertEquals(2, SqlSplitter.containsSqlEnd("GO   ", "GO", SqlSplitter.NO_END));
    }

    /**
     * Test a problem with single quotes split over multiple lines
     *
     * @throws Exception
     */
    public void testSqlSingleQuotesInDifferentLines() throws Exception {
        // @formatter:off
        String sql = "BEGIN\n"
                + "requete='INSERT INTO rid_oid(rid,oids)\n"
                + "         VALUE('||quote_literal(rid)||',' \n"
                + "         ||quote_literal(Oid)||')';\n"
                + "EXECUTE requete;";
        // @formatter:on

        BufferedReader in = new BufferedReader(new StringReader(sql));

        // Only checking if this complex statement can be parsed
        String line;

        line = in.readLine();
        assertEquals(SqlSplitter.NO_END, SqlSplitter.containsSqlEnd(line, ";", SqlSplitter.NO_END));

        line = in.readLine();
        assertEquals(SqlSplitter.OVERFLOW_SINGLE_QUOTE, SqlSplitter.containsSqlEnd(line, ";", SqlSplitter.NO_END));

        line = in.readLine();
        assertEquals(SqlSplitter.NO_END, SqlSplitter.containsSqlEnd(line, ";", SqlSplitter.OVERFLOW_SINGLE_QUOTE));

        line = in.readLine();
        assertEquals(35, SqlSplitter.containsSqlEnd(line, ";", SqlSplitter.NO_END));

        line = in.readLine();
        assertEquals(16, SqlSplitter.containsSqlEnd(line, ";", SqlSplitter.NO_END));
    }

    /**
     * Test if separators inside multi-line comments get ignored. See MSQL-67
     *
     * @throws Exception
     */
    public void testSemicolonInComment() throws Exception {
        // @formatter:off
        String sql =
                "/* this is a commented-out statment:\n" + " SELECT * FROM TABLE;\n" + "and here the comment ends */ ";
        // @formatter:on

        BufferedReader in = new BufferedReader(new StringReader(sql));

        // Only checking if this complex statement can be parsed
        String line;

        line = in.readLine();
        assertEquals(SqlSplitter.OVERFLOW_COMMENT, SqlSplitter.containsSqlEnd(line, ";", SqlSplitter.NO_END));

        line = in.readLine();
        assertEquals(SqlSplitter.OVERFLOW_COMMENT, SqlSplitter.containsSqlEnd(line, ";", SqlSplitter.OVERFLOW_COMMENT));

        line = in.readLine();
        assertEquals(SqlSplitter.NO_END, SqlSplitter.containsSqlEnd(line, ";", SqlSplitter.OVERFLOW_COMMENT));
    }

    public void testSlashDelimiterWithComment() throws Exception {

        String sql = "begin\n/* this is a comment */\n SELECT * FROM TABLE;\nend;\n/\n";

        BufferedReader in = new BufferedReader(new StringReader(sql));

        // Only checking if this complex statement can be parsed
        String line;

        line = in.readLine();
        assertEquals(SqlSplitter.NO_END, SqlSplitter.containsSqlEnd(line, "/", SqlSplitter.NO_END));

        line = in.readLine();
        assertEquals(SqlSplitter.NO_END, SqlSplitter.containsSqlEnd(line, "/", SqlSplitter.NO_END));

        line = in.readLine();
        assertEquals(SqlSplitter.NO_END, SqlSplitter.containsSqlEnd(line, "/", SqlSplitter.NO_END));

        line = in.readLine();
        assertEquals(SqlSplitter.NO_END, SqlSplitter.containsSqlEnd(line, "/", SqlSplitter.NO_END));

        line = in.readLine();
        assertEquals(1, SqlSplitter.containsSqlEnd(line, "/", SqlSplitter.NO_END));
    }

    private void contains(String sql, int expectedIndex) throws Exception {
        contains(sql, ";", expectedIndex);
    }

    private void containsNot(String sql) throws Exception {
        containsNot(sql, ";");
    }

    private void contains(String sql, String delimiter, int expectedIndex) throws Exception {
        assertEquals(sql, expectedIndex, SqlSplitter.containsSqlEnd(sql, delimiter, SqlSplitter.NO_END));
    }

    private void containsNot(String sql, String delimiter) throws Exception {
        assertTrue(sql, SqlSplitter.containsSqlEnd(sql, delimiter, SqlSplitter.NO_END) == SqlSplitter.NO_END);
    }
}
