#-*- coding:utf-8 -*-

import sys
import urllib

import requests
from PyQt4.QtCore import QThread, pyqtSignal


class Client(QThread):

    finished = pyqtSignal()

    def __init__(self, server="127.0.0.1:7313", parent=None):
        QThread.__init__(self, parent)
        self.server = server
        self.mode = "search"
        self.results = []
        self.character = None

    def run(self):
        if self.mode == "search":
            self.results = self.get_cartoons()
        elif self.mode == "list":
            self.results = self.get_characters()
        self.finished.emit()

    def get_characters(self):
        try:
            response = requests.get("http://%s/list" % self.server)
            characters = response.json()
            return characters
        except Exception as err:
            sys.stderr.write("%s\n" % err)

    def get_cartoons(self):
        if self.character["AltName"]:
            char = self.character["AltName"]
        else:
            char = self.character["Name"]
        try:
            response = requests.get("http://%s/search/%s" % (
                self.server, urllib.quote(char)))
            cartoons = response.json()
            return cartoons
        except Exception as err:
            sys.stderr.write("%s\n" % err)
