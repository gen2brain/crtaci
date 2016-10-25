#-*- coding:utf-8 -*-

import os
from os.path import join

DIST_DIR = os.environ["DIST_DIR"]
BASE_DIR = os.environ["BASE_DIR"]

a = Analysis([join(BASE_DIR, 'crtaci')], pathex=[join(BASE_DIR, 'frontend', 'python')])

pyz = PYZ(a.pure)

exe = EXE(pyz,
	a.scripts,
	exclude_binaries=1,
	name=join(DIST_DIR, 'build', 'pyi.win32', 'crtaci', 'crtaci.exe'),
	debug=False,
	strip=False,
	upx=True,
	console=False,
	icon=join(DIST_DIR, 'crtaci.ico'))

coll = COLLECT(exe,
	a.binaries,
	a.zipfiles,
	a.datas,
	strip=False,
	upx=True,
	name='crtaci')
