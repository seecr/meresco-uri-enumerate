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

