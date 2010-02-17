package org.codehaus.mojo.sql;

import junit.framework.TestCase;

public class ProcedureCallHelperTest extends TestCase {
	private static String query1 = "EXECUTE PROCEDURE foo(1,'cats')";
	private static String query2 = "exec bar";
	private static String query3 = "EXECUTE\n PROCEDURE\n\n foobar\n  (1, 2, 'the cat''s'  )";
	private static String query4 = "SELECT 1 FROM tablename";
	private static String query5 = "call bar ('bob,frank')";
	private static String query6 = "Exec bar ('fred', nestedfunction('arg'))";
	
	public void testIsProcedureCall() {
		assertTrue(ProcedureCallHelper.isProcedureCall(query1));
		assertTrue(ProcedureCallHelper.isProcedureCall(query2));
		assertTrue(ProcedureCallHelper.isProcedureCall(query3));
		assertFalse(ProcedureCallHelper.isProcedureCall(query4));
		assertTrue(ProcedureCallHelper.isProcedureCall(query5));
		assertTrue(ProcedureCallHelper.isProcedureCall(query6));
	}
	
	public void testParseQuery1() {
		assertEquals("{call foo(1,'cats')}", ProcedureCallHelper.sqlProcedureCallToJDBCCallStatement(query1));
	}
	
	public void testParseQuery2() {
		assertEquals("{call bar()}", ProcedureCallHelper.sqlProcedureCallToJDBCCallStatement(query2));
	}
	
	public void testParseQuery3() {
		assertEquals("{call foobar(1, 2, 'the cat''s'  )}", ProcedureCallHelper.sqlProcedureCallToJDBCCallStatement(query3));
	}

	public void testParseQuery5() {
		assertEquals("{call bar('bob,frank')}", ProcedureCallHelper.sqlProcedureCallToJDBCCallStatement(query5));
	}
	
	public void testParseQuery6() {
		assertEquals("{call bar('fred', nestedfunction('arg'))}", ProcedureCallHelper.sqlProcedureCallToJDBCCallStatement(query6));
	}
}
