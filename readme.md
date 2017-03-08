
# Introduction
Apache maven plugin to enforce that all keys are replaced when using maven resources filtering `<filters>`.

## Scenario usages
* For **failing fast** at build time when some keys are not replaced,
* To ensure that no password are in clear text,
* To enforce anything that can be checked with regexp.

This maven plugin use regular expression to locate matches and make the build fail with a nice readable message. It traverse recursively all folders and support multiple grep.

**In Short it enforce some criteria using grep.**

# Quick Usage
```<build>
<plugins>
    <plugin>
        <groupId>com.cedric.walter.maven.grep</groupId>
        <artifactId>ressources-grep-maven-plugin</artifactId>
        <version>1.0.0</version>
        <executions>
            <execution>
                <goals>
                    <goal>grep</goal>
                </goals>
                <phase>process-resources</phase>
                <configuration>
                    <folder>${project.build.directory}</folder> <!-- optional, will search recursively in all folder below this folder -->
                    <greps>
                        <grep>
                            <filePattern>([^\s]+(\.(?i)(txt|xml|xsl|properties|csv|conf|properties|jsp|css|scss|svg|js|html|episode|xsd|dtd))$)</filePattern> <!-- optional, default are all text format -->
                            <outputPattern>found in file ${fileName} at line ${lineNumber} : ${line}</outputPattern> <!-- optional, but can be changed -->
                            <failIfFound>true</failIfFound>
                            <grepPattern>@.*@</grepPattern> <!-- what to search for, use java match-->
                        </grep>
                    </greps>
                </configuration>
            </execution>
        </executions>
    </plugin>
</plugins>
```
   