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
        cmd = [MPV, "--fs", "--really-quiet", "--autofit", "60%", "--cache", "1024", "--no-ytdl"]
        if self.rotate:
            cmd += ["--video-rotate", self.rotate]
        return cmd

    def get_info(self):
        ydl = YoutubeDL({"quiet": True, "prefer_insecure": True})
        ydl.add_default_info_extractors()
        try:
            info = ydl.extract_info(self.url, download=False)
        except DownloadError:
            info = None
        return info

    def get_url(self, info):
        try:
            return u"%s" % info["url"]
        except KeyError:
            for i in info["formats"]:
                if i["ext"] == "mp4" or i["ext"] == "flv":
                    return i["url"]

    def proc_open(self):
        info = self.get_info()
        sys.stderr.write("URL: %s\n" % self.url)
        if info is not None:
            cmd = self.get_cmd()
            title = u"%s" % info["title"]
            url = self.get_url(info)
            cmd += ["--media-title", title, url]
            sys.stderr.write("Playing: %s\n\n" % url)
            try:
                ret = subprocess.call(cmd)
            except TypeError:
                ret = 1
            self.finished.emit(ret)
        else:
            self.finished.emit(1)

