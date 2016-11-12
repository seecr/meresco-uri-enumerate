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

from os.path import join, abspath, dirname
from os import system
from traceback import print_exc
from time import time

from seecr.test.integrationtestcase import IntegrationState as SeecrIntegrationState
from seecr.test.portnumbergenerator import PortNumberGenerator
from seecr.test.utils import postRequest, sleepWheel
from meresco.components.json import JsonDict


mydir = dirname(abspath(__file__))
projectDir = dirname(dirname(mydir))

class IntegrationState(SeecrIntegrationState):
    def __init__(self, stateName, tests=None, fastMode=False):
        SeecrIntegrationState.__init__(self, "meresco-uri-enumerate-" + stateName, tests=tests, fastMode=fastMode)
        self.stateName = stateName
        if not fastMode:
            system('rm -rf ' + self.integrationTempdir)
            system('mkdir --parents '+ self.integrationTempdir)
        self.numerateServerPort = PortNumberGenerator.next()
        self.testdataDir = join(dirname(mydir), "data")

    def setUp(self):
        self.startNumerateServer()
        self._createDatabase()

    def binDir(self):
        return join(projectDir, 'bin')

    def _createDatabase(self):
        if self.fastMode:
            print "Reusing database in", self.integrationTempdir
            return
        start = time()
        print "Creating database in", self.integrationTempdir
        try:
            print "Finished creating database in %s seconds" % (time() - start)
        except Exception:
            print 'Error received while creating database for', self.stateName
            print_exc()
            exit(1)

    def startNumerateServer(self):
        self._startServer('numerate-server', self.binPath('start-uri-numerate-server'), 'http://localhost:{}/info'.format(self.numerateServerPort), port=self.numerateServerPort, stateDir=join(self.integrationTempdir, 'numerate'))
