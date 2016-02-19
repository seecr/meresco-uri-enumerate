
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
        self._startServer('numerate-server', self.binPath('start-numerate-server'), 'http://localhost:{}/info'.format(self.numerateServerPort), port=self.numerateServerPort, stateDir=join(self.integrationTempdir, 'numerate'))
