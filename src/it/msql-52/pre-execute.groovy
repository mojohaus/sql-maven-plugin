File outputFile = new File( basedir, 'target/oracle-sqlplus.sql')
outputFile.getParentFile().mkdirs();
new FileReader( 'src/main/sql/oracle-sqlplus.sql' ).transformLine( outputFile.newWriter() ) { line->
  line.replaceAll( '&1', 'customer' ).replaceAll( '&&schema..', 'SOME_SCHEMA.' ) 
}
