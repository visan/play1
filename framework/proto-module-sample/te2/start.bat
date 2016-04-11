rem call setenv.cmd
set PLAY_HOME=C:\_Projects\play1\play1
set PATH=%PATH%;%PLAY_HOME%
rem
set JAVA_OPTS=-Duser.country=US -Duser.language=en-Dprecompiled=true
set JAVA_OPTS=%JAVA_OPTS% -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8001
set JAVA_OPTS=%JAVA_OPTS% -Xms48m -Xmx48m 

rem 
rem set MODULES=C:\_Projects\mt1;C:\_Projects\mt2
play run %JAVA_OPTS% 
