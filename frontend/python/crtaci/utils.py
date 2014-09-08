#-*- coding:utf-8 -*-

import os
import re

from PyQt4.QtCore import QFile


def which(prog):
    def is_exe(fpath):
        return os.path.exists(fpath) and os.access(fpath, os.X_OK)
    fpath, fname = os.path.split(prog)
    if fpath:
        if is_exe(prog):
            return prog
    else:
        for path in os.environ["PATH"].split(os.pathsep):
            filename = os.path.join(path, prog)
            if is_exe(filename):
                return filename
    return None


def titlecase(s):
    return re.sub(
        re.compile(r"[\w]+('[\w]+)?", flags=re.UNICODE),
        lambda mo: mo.group(0)[0].upper() + mo.group(0)[1:].lower(), s)


def readrc(rc):
    fd = QFile(rc)
    fd.open(QFile.ReadOnly)
    data = str(fd.readAll())
    fd.close()
    return data
