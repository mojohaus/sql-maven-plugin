# MojoHaus SQL Maven Plugin

This is the [sql-maven-plugin](http://www.mojohaus.org/sql-maven-plugin/).
 
[![Apache License, Version 2.0, January 2004](https://img.shields.io/github/license/mojohaus/sql--maven-plugin.svg?label=License)](http://www.apache.org/licenses/)
[![Maven Central](https://img.shields.io/maven-central/v/org.codehaus.mojo/sql-maven-plugin.svg?label=Maven%20Central)](http://search.maven.org/#search%7Cga%7C1%7Csql-maven-plugin)
[![Build Status](https://travis-ci.org/mojohaus/sql-maven-plugin.svg?branch=master)](https://travis-ci.org/mojohaus/sql-maven-plugin)

## Releasing

* Make sure `gpg-agent` is running.
* Execute `mvn -B release:prepare release:perform`

For publishing the site do the following:

```
cd target/checkout
mvn verify site site:stage scm-publish:publish-scm
```
