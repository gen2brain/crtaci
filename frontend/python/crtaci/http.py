#-*- coding:utf-8 -*-

import os
import sys
import subprocess

from PyQt4.QtCore import QThread

from crtaci.utils import which


class Http(QThread):

    def __init__(self, bind=":7313", parent=None):
        QThread.__init__(self, parent)
        self.proc = None
        self.bind = bind

    def __del__(self):
        if self.proc:
            self.stop()

    def run(self):
        self.proc_open()

    def get_cmd(self):
        if sys.platform.startswith("linux") or sys.platform == "darwin":
            if hasattr(sys, "_MEIPASS"):
                binary = os.path.join(sys._MEIPASS, "backend", "crtaci-http")
            else:
                binary = which("crtaci-http")
                if not binary:
                    binary = os.path.join(os.getcwd(), "backend", "crtaci-http")
        elif sys.platform == "win32":
            binary = os.path.join(os.getcwd(), "backend", "crtaci-http.exe")
        cmd = [binary, "-bind", self.bind]
        return cmd

    def proc_open(self):
        cmd = self.get_cmd()
        self.proc = subprocess.Popen(cmd)

    def stop(self):
        try:
            self.proc.terminate()
            self.proc.kill()
        except OSError:
            pass
