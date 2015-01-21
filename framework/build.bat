rem call setenv.cmd
set PATH=%PATH%;C:\prog\Git\bin

set ANT_HOME=F:\java\apache-ant-1.9.4
rem set JAVA_HOME=F:\java\jdk6-45-b64
set JAVA_HOME=C:\java\jdk-9.25-b64


%ANT_HOME%/bin/ant -l build.log -v -f build.xml package