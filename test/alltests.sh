## begin license ##
#
# "Meresco Uri Enumerate" contains an http server which maps uris to integer numbers
#
# Copyright (C) 2016 Seecr (Seek You Too B.V.) http://seecr.nl
#
# This file is part of "Meresco Uri Enumerate"
#
# "Meresco Uri Enumerate" is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
#
# "Meresco Uri Enumerate" is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with "Meresco Uri Enumerate"; if not, write to the Free Software
# Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
#
## end license ##

BUILDDIR=../build
test -d ${BUILDDIR} && rm -rf ${BUILDDIR}
mkdir ${BUILDDIR}


JAVA_VERSION=8
if [ -z "${JAVA_BIN}" ]; then
    JAVA_BIN=/usr/bin
fi
${JAVA_BIN}/java -version 2>&1 | grep "1.${JAVA_VERSION}" > /dev/null
if [ $? -ne 0 ]; then
    echo "Java version should be java ${JAVA_VERSION}; You could specify a different java with JAVA_BIN"
    exit 1
fi
if [ -z "${JAVAC_BIN}" ]; then
    JAVAC_BIN=/usr/lib/jvm/java-1.${JAVA_VERSION}.0-openjdk.x86_64/bin
    if [ -f /etc/debian_version ]; then
        JAVAC_BIN=/usr/lib/jvm/java-${JAVA_VERSION}-openjdk-amd64/bin
    fi
fi
${JAVAC_BIN}/javac -version 2>&1 | grep "1.${JAVA_VERSION}" > /dev/null
if [ $? -ne 0 ]; then
    echo "javac version should be java ${JAVA_VERSION}; You could specify a different javac with JAVAC_BIN"
    exit 1
fi


JUNIT=/usr/share/java/junit4.jar
if [ ! -f ${JUNIT} ]; then
    echo "JUnit is not installed. Please install the junit4 package."
    exit 1
fi

JARS=$(find ../jars -type f -name "*.jar")
LUCENEVERSION=7.3.0
LUCENE_JARS=$(find /usr/share/java -type f -name "lucene-*${LUCENEVERSION}.jar")

CP="$JUNIT:$(echo $JARS | tr ' ' ':'):$(echo $LUCENE_JARS | tr ' ' ':'):../build"

javaFiles=$(find ../src -name "*.java")
${JAVAC_BIN}/javac -d ${BUILDDIR} -cp $CP $javaFiles
if [ "$?" != "0" ]; then
    echo "Build failed"
    exit 1
fi

javaFiles=$(find org -name "*.java")
${JAVAC_BIN}/javac -d ${BUILDDIR} -cp $CP $javaFiles
if [ "$?" != "0" ]; then
    echo "Test Build failed"
    exit 1
fi

testClasses=$(find org -name "*Test.java" | sed 's,.java,,g' | tr '/' '.')
echo "Running $testClasses"
${JAVA_BIN}/java -Xmx1024m -classpath ".:$CP" org.junit.runner.JUnitCore $testClasses
