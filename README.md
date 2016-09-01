# README #

* Hot deploy files from your workspace to jboss exploded deployed package
* Version 0.0.0.0.0.0.2

### How do I get set up? ###

* compile with maven : 
```
#!

mvn clean compile assembly:single
```

* fill in config.ini

```
#!

[config]
sourceFolders=/path/of/your/workspace
jbossHome=/path/of/jboss/standalone/
jbossDeployedPackagePrefix=prexif-of-package-1.0.0
jbossDeployedSubpath=
logLevel=debug
```

* launch (optionally overload ini config with command line options) with 
```
#!

java -jar packagedJar

usage: magicHotDeployer
 -d,--jbossHome <arg>                    jboss home (e.g.
                                         /...path.../standalone
 -l,--logLevel <arg>                     log level (one of trace, debug,
                                         info, warn, error, shut-up,
                                         every-single-shit
 -p,--jbossDeployedPackagePrefix <arg>   deploy package prefix (e.g.
                                         scadeweb-web-1.0.0.war)
 -s,--sourceFolders <arg>                source folders
 -z,--jbossDeployedSubpath <arg>         relative path inner to jboss
                                         deployed structure

e.g.
java -jar packagedJar -s "/another/project/folder" -z "/js" -l trace

```

### Who do I talk to? ###

* ref. dev@fabiofenoglio.it