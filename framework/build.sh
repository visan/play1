#!/bin/bash

ANT_HOME=/java/ant-1.9.2
JAVA_HOME=/java/jdk-6-45-64b

export ANT_HOME JAVA_HOME

$ANT_HOME/bin/ant -l build.log -v -f build.xml package