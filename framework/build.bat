rem call setenv.cmd

set ANT_HOME=C:\java\apache-ant-1.9.4
rem set JAVA_HOME=F:\java\jdk6-45-b64
set JAVA_HOME=C:\java\jdk8-25


%ANT_HOME%/bin/ant -l build.log -v -f build.xml package