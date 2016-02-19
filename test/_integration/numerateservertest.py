
from seecr.test import IntegrationTestCase
from seecr.test.utils import postRequest, getRequest
from os import kill, remove
from os.path import isfile, join
from time import sleep


class NumerateServerTest(IntegrationTestCase):
    def testNumerate(self):
        header, body = postRequest(self.numerateServerPort, '/numerate', data='id0', parse=False)
        self.assertTrue("200 OK" in header.upper(), header)
        header, body2 = postRequest(self.numerateServerPort, '/numerate', data='id0', parse=False)
        self.assertTrue("200 OK" in header.upper(), header)
        self.assertEquals(body2, body)
        header, body3 = postRequest(self.numerateServerPort, '/numerate', data='id1', parse=False)
        self.assertNotEquals(body3, body)

    def testCommit(self):
        header, body = postRequest(self.numerateServerPort, '/commit', parse=False)
        self.assertTrue("200 OK" in header.upper(), header)

    def testInfo(self):
        header, body = postRequest(self.numerateServerPort, '/numerate', data='id0', parse=False)
        header, body = postRequest(self.numerateServerPort, '/numerate', data='id1', parse=False)
        header, body = getRequest(self.numerateServerPort, '/info', parse=False)
        self.assertTrue("200 OK" in header.upper(), header)
        self.assertEqual('{"total": 2}', body)

    def testDontStartAfterCrash(self):
        numerateStatePath = join(self.integrationTempdir, 'numerate')
        runningMarker = join(numerateStatePath, "running.marker")
        self.assertTrue(isfile(runningMarker))
        kill(self.pids["numerate-server"], 9)
        sleep(0.01)
        try:
            self.startNumerateServer()
            self.fail("Should not start with a running.marker")
        except SystemExit:
            pass
        remove(runningMarker)
        self.startNumerateServer()

