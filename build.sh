#!/bin/bash
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

source /usr/share/seecr-tools/functions.d/distro

VERSION=$1
LUCENEVERSION=6.1.0
JARS=$(find jars -type f -name "*.jar")
LUCENE_JARS=$(find /usr/share/java -type f -name "lucene-*${LUCENEVERSION}.jar")

BUILDDIR=./build
TARGET=meresco-uri-enumerate.jar
if [ "${VERSION}" != "" ]; then
    TARGET=meresco-uri-enumerate-${VERSION}.jar
fi

test -d $BUILDDIR && rm -r $BUILDDIR
mkdir $BUILDDIR

CP="$(echo $JARS | tr ' ' ':'):$(echo $LUCENE_JARS | tr ' ' ':')"

JAVA_VERSION=8
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

javaFiles=$(find src/org -name "*.java")
${JAVAC_BIN}/javac -d $BUILDDIR -cp $CP $javaFiles
if [ "$?" != "0" ]; then
    echo "Build failed"
    exit 1
fi

jar -cf $TARGET -C $BUILDDIR org

