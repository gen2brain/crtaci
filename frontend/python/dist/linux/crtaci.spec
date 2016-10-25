#-*- coding:utf-8 -*-

import os
from os.path import join

DIST_DIR = os.environ["DIST_DIR"]
BASE_DIR = os.environ["BASE_DIR"]

a = Analysis([join(BASE_DIR, 'crtaci')], pathex=[join(BASE_DIR, 'frontend', 'python')])
a.datas += [((join('backend', 'crtaci-http'), join('backend', 'crtaci-http'), 'DATA'))]

pyz = PYZ(a.pure)

exe = EXE(pyz,
    a.scripts,
    a.binaries,
    a.zipfiles,
    a.datas,
    name=join(DIST_DIR, 'crtaci', 'crtaci.bin'),
    debug=False,
    strip=True,
    upx=True,
    console=True)
