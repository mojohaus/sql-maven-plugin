package org.codehaus.mojo.sql;

import junit.framework.TestCase;

import java.util.Properties;


/**
 * Unit test for simple SqlExecMojo.
 */
public class SqlExecMojoTest extends TestCase {
    private Properties p;

    protected void setUp() throws Exception {
        super.setUp();
        p = new Properties();
        p.load(getClass().getResourceAsStream("/test.properties"));
    }

    public void testMojo() {
        SqlExecMojo mojo = new SqlExecMojo();

        // populate parameters
        mojo.setDriver(p.getProperty("driver"));
        mojo.setUsername(p.getProperty("user"));
        mojo.setPassword(p.getProperty("password"));
        mojo.setUrl(p.getProperty("url"));

        Fileset ds = new Fileset();
        ds.setBasedir("src/test");
        ds.setIncludes(new String[]{"**/*.sql"});
        ds.scan();
        assert(ds.getIncludedFiles().length == 1);

        mojo.setFileset(ds);

        mojo.execute();
    }
}
 