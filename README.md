# MojoHaus SQL Maven Plugin

This is the [sql-maven-plugin](http://www.mojohaus.org/sql-maven-plugin/).
 
[![Apache License, Version 2.0, January 2004](https://img.shields.io/github/license/mojohaus/sql-maven-plugin.svg?label=License)](http://www.apache.org/licenses/)
[![Maven Central](https://img.shields.io/maven-central/v/org.codehaus.mojo/sql-maven-plugin.svg?label=Maven%20Central)](http://search.maven.org/#search%7Cga%7C1%7Csql-maven-plugin)
[![Build Status](https://travis-ci.org/mojohaus/sql-maven-plugin.svg?branch=master)](https://travis-ci.org/mojohaus/sql-maven-plugin)

Use as following:


      <plugin>
        <groupId>net.rationalminds</groupId>
        <artifactId>sql-maven-plugin</artifactId>
        <version>1.0</version>
        <executions>
          <execution>
            <id>generate.messages</id>
            <phase>generate-resources</phase>
            <goals>
              <goal>execute</goal>
            </goals>
            <configuration>
              <autocommit>replace_me</autocommit>
              <printResultSet>true</printResultSet>
              <outputDelimiter>row</outputDelimiter>
              <srcFiles>
                <srcFile>replace_me</srcFile>
              </srcFiles>
              <outputFile>replace_me</outputFile>
              <driver>replace_me</driver>
              <url>replace_me</url>
              <username>replace_me</username>
              <password>replace_me</password> 
              <outputEncoding>UTF-8</outputEncoding>  <!-- Specify encoding out output file-->     
            </configuration>
          </execution>         
        </executions>
        <dependencies>
          <dependency>
            <groupId>replace_me</groupId>
            <artifactId>replace_me</artifactId>
            <version>replace_me</version>
            <scope>compile</scope>
          </dependency>
        </dependencies>
        <configuration>
          <driver>replace_me</driver>
          <url>replace_me</url>
          <username>replace_me</username>
          <password>replace_me</password>
        </configuration>
      </plugin>
