#!/usr/bin/env python
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

from os.path import abspath, dirname                              #DO_NOT_DISTRIBUTE
from os import system                                             #DO_NOT_DISTRIBUTE
from glob import glob                                             #DO_NOT_DISTRIBUTE
from sys import path as systemPath                                #DO_NOT_DISTRIBUTE
projectDir = dirname(dirname(abspath(__file__)))                  #DO_NOT_DISTRIBUTE
system('find %s -name "*.pyc" | xargs rm -f' % projectDir)        #DO_NOT_DISTRIBUTE
for path in glob(projectDir+'/deps.d/*'):                         #DO_NOT_DISTRIBUTE
    systemPath.insert(0, path)                                    #DO_NOT_DISTRIBUTE
systemPath.insert(0, projectDir)                                  #DO_NOT_DISTRIBUTE

from sys import argv

from seecr.test.testrunner import TestRunner
from _integration.integrationstate import IntegrationState

flags = ['--fast']

if __name__ == '__main__':
    fastMode = '--fast' in argv
    for flag in flags:
        if flag in argv:
            argv.remove(flag)

    runner = TestRunner()
    IntegrationState(
        'default',
        tests=[
            '_integration.numerateservertest.NumerateServerTest',
        ],
        fastMode=fastMode).addToTestRunner(runner)

    testnames = argv[1:]
    runner.run(testnames)

