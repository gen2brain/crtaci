#-*- coding:utf-8 -*-

from PyQt4.QtGui import QMainWindow, QApplication, QListWidgetItem, QPixmap, QIcon, QDesktopServices, QMessageBox, QDialog, QMovie
from PyQt4.QtCore import Qt, QTimer, QSize, QUrl
from PyQt4.QtWebKit import QWebPage

from crtaci.http import Http
from crtaci.client import Client
from crtaci.player import Player
from crtaci.utils import titlecase, readrc
from crtaci.ui.mainwindow_ui import Ui_MainWindow
from crtaci.ui.about_ui import Ui_AboutDialog
from crtaci.ui import assets_rc
from crtaci.ui import icons_rc


class MainWindow(QMainWindow, Ui_MainWindow):

    def __init__(self):
        QMainWindow.__init__(self, parent=None)
        self.setupUi(self)
        self.center_window()

        self.http = Http(parent=self)
        self.http.start()

        self.client = Client()
        self.player = Player(parent=self)

        self.connect_signals()
        self.webView.page().setLinkDelegationPolicy(QWebPage.DelegateAllLinks)
        self.webView.setContextMenuPolicy(Qt.NoContextMenu)

        QTimer.singleShot(500, self.init)

    def init(self):
        self.set_loading()
        self.client.mode = "list"
        self.client.start()

    def connect_signals(self):
        self.client.finished.connect(self.on_client_finished)
        self.player.finished.connect(self.on_player_finished)
        self.webView.linkClicked.connect(self.on_link_clicked)
        self.listWidget.currentItemChanged.connect(self.on_item_changed)
        self.pushButton.clicked.connect(self.on_about_clicked)

    def center_window(self):
        size = self.size()
        desktop = QApplication.desktop()
        width, height = size.width(), size.height()
        dwidth, dheight = desktop.width(), desktop.height()
        cw, ch = (dwidth/2)-(width/2), (dheight/2)-(height/2)
        self.move(cw, ch)

    def add_items(self):
        characters = self.client.results
        for character in characters:
            item = QListWidgetItem(self.get_name(character["Name"]))
            item.setData(Qt.UserRole, character)
            item.setSizeHint(QSize(48, 48))
            icon = self.get_icon(character)
            if icon is not None:
                item.setIcon(icon)
            self.listWidget.addItem(item)
        self.listWidget.setCurrentRow(0)

    def on_item_changed(self, current, previous):
        self.labelLoading.setVisible(True)
        character = current.data(Qt.UserRole)
        self.client.mode = "search"
        self.client.character = character
        self.client.start()

    def on_client_finished(self):
        self.labelLoading.setVisible(False)
        if self.client.mode == "list":
            self.add_items()
        elif self.client.mode == "search":
            try:
                character =  self.get_name(self.client.results[0]["Character"])
                title = "%s / %s" % (u"Crtaći", self.get_name(self.client.results[0]["Character"]))
            except Exception:
                title = u"Crtaći"

            self.setWindowTitle(title)
            html = self.get_html(self.client.results)
            self.webView.setHtml(html)

    def on_player_finished(self, ret):
        if ret != 0:
            reply = QMessageBox.question(
                self, 'Player crashed!', 'Open URL in browser?',
                QMessageBox.Yes | QMessageBox.No, QMessageBox.Yes)
            if reply == QMessageBox.Yes:
                QDesktopServices.openUrl(QUrl(self.player.url))

    def on_link_clicked(self, url):
        self.player.url = url.toString()
        self.set_rotate()
        self.player.start()

    def on_about_clicked(self):
        AboutDialog(self)

    def set_loading(self):
        movie = QMovie(":/images/loading.gif")
        self.labelLoading.setMovie(movie)
        self.labelLoading.setVisible(False)
        movie.start()

    def set_rotate(self):
        if self.checkRotateRight.isChecked():
            self.player.rotate = "90"
        elif self.checkRotateLeft.isChecked():
            self.player.rotate = "270"
        else:
            self.player.rotate = None

    @staticmethod
    def get_name(name):
        name = name.replace(" - ", "").replace("-", "")
        return titlecase(name)

    @staticmethod
    def get_icon(character):
        if character["AltName"]:
            char = character["AltName"]
        else:
            char = character["Name"]
        char = char.replace(" - ", "")
        char = char.replace("-", "")
        char = char.replace(" ", "_")
        pixmap = QPixmap("://icons/%s.png" % char)
        if not pixmap.isNull():
            return QIcon(pixmap)
        else:
            return None

    @staticmethod
    def get_html(cartoons):
        html = ""
        template = readrc("://assets/view.html")
        for cartoon in cartoons:
            if cartoon["Service"] == "youtube":
                image = cartoon["Thumbnails"]["Large"]
            elif cartoon["Service"] == "dailymotion":
                image = cartoon["Thumbnails"]["Medium"]
            elif cartoon["Service"] == "vimeo":
                image = cartoon["Thumbnails"]["Medium"]
            se = ""
            if cartoon["Season"] != -1:
                se += "S%02d" % cartoon["Season"]
            if cartoon["Episode"] != -1:
                se += "E%02d" % cartoon["Episode"]
            if se:
                se = " - " + se
            html += '''
        <li>
            <div class="image"><a href="%s"><img data-original="%s" width="240" height="180"/><div class="play"></div></a></div>
            <div class="text">%s%s</div>
        </li>''' % (cartoon["Url"], image, cartoon["FormattedTitle"], se)
        return template.replace("{HTML}", html)


class AboutDialog(QDialog, Ui_AboutDialog):
    def __init__(self, parent):
        QDialog.__init__(self, parent)
        self.setupUi(self)
        self.setModal(True)
        self.show()

