# MojoHaus SQL Maven Plugin

This is the [sql-maven-plugin](http://www.mojohaus.org/sql-maven-plugin/).
 
[![Build Status](https://travis-ci.org/mojohaus/sql-maven-plugin.svg?branch=master)](https://travis-ci.org/mojohaus/sql-maven-plugin)

## Releasing

* Make sure `gpg-agent` is running.
* Execute `mvn -B release:prepare release:perform`

For publishing the site do the following:

```
cd target/checkout
mvn verify site site:stage scm-publish:publish-scm
```
