#!/usr/bin/env python

# Author: Milan Nikolic <gen2brain@gmail.com>
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.


import os
import sys

import sip
sip.setapi("QString", 2)
sip.setapi("QVariant", 2)

from PyQt4.QtGui import QApplication

if os.path.isdir(os.path.join(".", "crtaci")) and os.path.isfile(
        os.path.join(".", "setup.py")):
    sys.path.insert(0, os.path.realpath("crtaci"))

from crtaci.main import MainWindow


def main():
    app = QApplication(sys.argv)
    app.setApplicationName("Crtaci")

    window = MainWindow()
    window.show()
    window.raise_()
    sys.exit(app.exec_())

if __name__ == "__main__":
    main()
