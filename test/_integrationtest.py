#!/usr/bin/env python

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

