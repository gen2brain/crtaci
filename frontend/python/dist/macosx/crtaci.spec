#-*- coding:utf-8 -*-

import os
from os.path import join

DIST_DIR = os.environ["DIST_DIR"]
BASE_DIR = os.environ["BASE_DIR"]

a = Analysis([join(BASE_DIR, 'crtaci')], pathex=[join(BASE_DIR, 'frontend', 'python')],
    hiddenimports=[],
    hookspath=None,
    runtime_hooks=None)

a.binaries + [('libQtCLucene.4.dylib',
    '/usr/local/lib/libQtCLucene.4.dylib', 'BINARY')]

pyz = PYZ(a.pure)

exe = EXE(pyz,
    a.scripts,
    exclude_binaries=True,
    name='crtaci',
    debug=False,
    strip=None,
    upx=True,
    console=False)

coll = COLLECT(exe,
    a.binaries,
    a.zipfiles,
    a.datas,
    strip=None,
    upx=True,
    name=join('dist', 'macosx', 'crtaci'))

app = BUNDLE(coll,
    name=join('dist', 'macosx', 'Crtaci.app'))
