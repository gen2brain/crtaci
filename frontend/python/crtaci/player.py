#-*- coding:utf-8 -*-

import os
import sys
import subprocess

from PyQt4.QtCore import QThread, pyqtSignal
from youtube_dl.YoutubeDL import YoutubeDL
from youtube_dl.utils import DownloadError

from crtaci.utils import which

if sys.platform.startswith("linux"):
    MPV = which("mpv")
    if not MPV:
        sys.stderr.write("This application needs latest mpv installed.\n")
        sys.exit(1)
elif sys.platform == "darwin":
    MPV = os.path.join(os.getcwd(), "mpv")
elif sys.platform == "win32":
    MPV = os.path.join(os.getcwd(), "mpv.exe")


class Player(QThread):

    finished = pyqtSignal(int)

    def __init__(self, url=None, parent=None):
        QThread.__init__(self, parent)
        self.url = url
        self.rotate = None

    def run(self):
        self.proc_open()

    def get_cmd(self):
        cmd = [MPV, "--fs", "--really-quiet", "--autofit", "60%"]
        if self.rotate:
            cmd += ["--video-rotate", self.rotate]
        return cmd

    def get_info(self):
        ydl = YoutubeDL({"quiet": True})
        ydl.add_default_info_extractors()
        try:
            info = ydl.extract_info(self.url, download=False)
        except DownloadError:
            info = None
        return info

    def proc_open(self):
        info = self.get_info()
        sys.stderr.write("URL: %s\n" % self.url)
        if info is not None:
            cmd = self.get_cmd()
            cmd += ["--media-title", info["title"].encode("utf-8"), info["url"]]
            sys.stderr.write("Playing: %s\n\n" % info["url"])
            ret = subprocess.call(cmd)
            self.finished.emit(ret)
        else:
            self.finished.emit(1)

