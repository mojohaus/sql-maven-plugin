package org.codehaus.mojo.sql;


public class ProcedureCallHelper {
    public static boolean isProcedureCall(String sql) {
        String startOfSql = sql.trim().substring(0, 4).toUpperCase();
        if (startOfSql.startsWith("CALL") || startOfSql.startsWith("EXEC")) {
            return true;
        } else {
            return false;
        }
    }

    public static String sqlProcedureCallToJDBCCallStatement(String sql) {
        sql = sql.trim();
        String parameters = "";
        int parametersStart = sql.indexOf('(');
        if (parametersStart != -1) 
        {
            int parametersEnd = sql.length() - 1;
            for (; parametersEnd > parametersStart && sql.charAt(parametersEnd) != ')'; parametersEnd--) 
            {
            }
            if(parametersEnd == parametersStart) {
                throw new RuntimeException("Could not parse procedure call parameters.");
            }
            parameters = sql.substring(parametersStart+1, parametersEnd);
        }
        else 
        {
            parametersStart = sql.length();
        }
        int procedureNameEnds = parametersStart - 1;
        for (; procedureNameEnds >= 0 && Character.isWhitespace(sql.charAt(procedureNameEnds)); procedureNameEnds--) 
        {
        }
        int procedureNameStarts = procedureNameEnds;
        for (; procedureNameStarts >= 0 && !Character.isWhitespace(sql.charAt(procedureNameStarts)); procedureNameStarts--) 
        {
        }
        if (procedureNameStarts == 0 || procedureNameEnds == 0 || procedureNameStarts == procedureNameEnds) 
        {
            throw new RuntimeException("Cound not parse procedure call, failed to find procedure name");
        }
        procedureNameStarts += 1;
        procedureNameEnds += 1;
        String procedureName = sql.substring(procedureNameStarts, procedureNameEnds);
        return "{call " + procedureName + "(" + parameters + ")}";
    }
}
