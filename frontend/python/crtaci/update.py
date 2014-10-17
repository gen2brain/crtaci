#-*- coding:utf-8 -*-

import requests
from PyQt4.QtCore import QThread, pyqtSignal

from crtaci import APP_VERSION


class Update(QThread):

    finished = pyqtSignal(bool)

    def __init__(self, parent=None):
        QThread.__init__(self, parent)
        self.result = False
        self.appurl = "https://github.com/gen2brain/crtaci/"
        self.appurl += "releases/download/%s/crtaci-%s.apk"

    def run(self):
        self.result = self.update_exists(APP_VERSION)
        self.finished.emit(self.result)

    def update_exists(self, curr):
        try:
            v = float(curr) + 0.1
            ver = "%.1f" % v
            response = requests.head(self.appurl % (ver, ver))
            if response.status_code == 302:
                return True
            else:
                return False
        except:
            return False
