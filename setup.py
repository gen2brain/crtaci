#!/usr/bin/env python

import os
import sys
import shutil
import tarfile
import platform
import subprocess
from fnmatch import fnmatch
from os.path import join, dirname, realpath, basename
from distutils.core import setup, Command
from distutils.dep_util import newer
from distutils.command.build import build
from distutils.command.clean import clean
from distutils.dir_util import copy_tree

sys.path.insert(0, os.path.realpath(os.path.join("frontend", "python")))

from crtaci import APP_VERSION
BASE_DIR = dirname(realpath(__file__))


class build_qt(Command):
    user_options = []

    def initialize_options(self):
        pass

    def finalize_options(self):
        pass

    def compile_ui(self, ui_file):
        from PyQt4 import uic
        py_file = os.path.splitext(ui_file)[0] + "_ui.py"
        if not newer(ui_file, py_file):
            return
        fp = open(py_file, "w")
        uic.compileUi(ui_file, fp, pyqt3_wrapper=True, from_imports=True)
        fp.close()

    def compile_rc(self, qrc_file):
        import PyQt4
        py_file = os.path.splitext(qrc_file)[0] + "_rc.py"
        if not newer(qrc_file, py_file):
            return
        origpath = os.getenv("PATH")
        path = origpath.split(os.pathsep)
        path.append(dirname(PyQt4.__file__))
        os.putenv("PATH", os.pathsep.join(path))
        if subprocess.call(["pyrcc4", "-py3", qrc_file, "-o", py_file]) > 0:
            self.warn("Unable to compile resource file %s" % qrc_file)
            if not os.path.exists(py_file):
                sys.exit(1)
        os.putenv('PATH', origpath)

    def run(self):
        basepath = join(dirname(__file__), 'frontend', 'python', 'crtaci', 'ui')
        for dirpath, dirs, filenames in os.walk(basepath):
            for filename in filenames:
                if filename.endswith('.ui'):
                    self.compile_ui(join(dirpath, filename))
                elif filename.endswith('.qrc'):
                    self.compile_rc(join(dirpath, filename))


class build_exe(Command):
    user_options = []
    dist_dir = join(BASE_DIR, "dist", "windows")

    def initialize_options(self):
        pass

    def finalize_options(self):
        pass

    def copy_files(self):
        dest_path = join(self.dist_dir, "crtaci")
        for file_name in [
                "AUTHORS", "ChangeLog", "COPYING",
                "README.md", "mpv.exe", "mpv.com"]:
            shutil.copy(join(BASE_DIR, file_name), dest_path)
        for dir_name in [
            "backend", "fonts", "mpv"]:
            copy_tree(join(BASE_DIR, dir_name), join(dest_path, dir_name))

    def run_build_installer(self):
        iss_file = ""
        iss_in = join(self.dist_dir, "crtaci.iss.in")
        iss_out = join(self.dist_dir, "crtaci.iss")
        with open(iss_in, "r") as iss: data = iss.read()
        lines = data.split("\n")
        for line in lines:
            line = line.replace("{ICON}", realpath(join(self.dist_dir, "crtaci")))
            line = line.replace("{VERSION}", APP_VERSION)
            iss_file += line + "\n"
        with open(iss_out, "w") as iss: iss.write(iss_file)
        iscc = join(os.environ["ProgramFiles(x86)"], "Inno Setup 5", "ISCC.exe")
        subprocess.call([iscc, iss_out])

    def run(self):
        self.run_command("build_qt")
        set_rthook()
        run_build(self.dist_dir)
        self.copy_files()
        self.run_build_installer()


class build_dmg(Command):
    user_options = []
    dist_dir = join(BASE_DIR, "dist", "macosx")

    def initialize_options(self):
        pass

    def finalize_options(self):
        pass

    def set_plist(self):
        info_plist = join(self.dist_dir, "dmg", "Crtaci.app", "Contents", "Info.plist")
        shutil.copy(join(self.dist_dir, "crtaci.icns"),
                    join(self.dist_dir, "dmg", "Crtaci.app", "Contents", "Resources"))
        shutil.copy(join(self.dist_dir, "crtaci.sh"),
                    join(self.dist_dir, "dmg", "Crtaci.app", "Contents", "MacOS"))
        with open(info_plist, "r") as opts: data = opts.read()
        plist_file = ""
        lines = data.split("\n")
        for line in lines:
            if "0.0.0" in line:
                line = line.replace("0.0.0", "1.3")
            elif "icon-windowed.icns" in line:
                line = line.replace("icon-windowed.icns", "crtaci.icns")
            elif "MacOS/crtaci" in line:
                line = line.replace("MacOS/crtaci", "crtaci.sh")
            plist_file += line + "\n"
        with open(info_plist, "w") as opts: opts.write(plist_file)

    def copy_player(self):
        src_path = join(self.dist_dir, "mpv", "Contents")
        dest_path = join(self.dist_dir, "dmg", "Crtaci.app", "Contents")
        copy_tree(src_path, dest_path)

    def copy_files(self):
        dest_path = join(self.dist_dir, "dmg")
        if not os.path.exists(dest_path):
            os.mkdir(dest_path)
        shutil.move(join(self.dist_dir, "Crtaci.app"), dest_path)
        for file_name in ["AUTHORS", "ChangeLog", "COPYING", "README.md"]:
            shutil.copy(join(BASE_DIR, file_name), dest_path)
        backend_path = join(dest_path, "Crtaci.app", "Contents", "MacOS", "backend")
        if not os.path.exists(backend_path):
            os.mkdir(backend_path)
        shutil.copy(join(BASE_DIR, "backend", "crtaci-http"), backend_path)

    def run_build_dmg(self):
        src_path = join(self.dist_dir, "dmg")
        dst_path = join(self.dist_dir, "crtaci-%s.dmg" % APP_VERSION)
        subprocess.call(["hdiutil", "create", dst_path, "-srcfolder", src_path])

    def run(self):
        self.run_command("build_qt")
        set_rthook()
        run_build(self.dist_dir)
        self.copy_files()
        self.copy_player()
        self.set_plist()
        self.run_build_dmg()


class build_bin(Command):
    user_options = []
    dist_dir = join(BASE_DIR, "dist", "linux")

    def initialize_options(self):
        pass

    def finalize_options(self):
        pass

    def copy_files(self):
        dest_path = join(self.dist_dir, "crtaci")
        if not os.path.exists(dest_path):
            os.mkdir(dest_path)
        for file_name in [
                "AUTHORS", "ChangeLog", "COPYING", "README.md"]:
            shutil.copy(join(BASE_DIR, file_name), dest_path)
        shutil.move(join(self.dist_dir, "crtaci.bin"), join(dest_path, "crtaci"))

    def run_build_tarball(self):
        bin_dir = join(self.dist_dir, "crtaci")
        source_dir = "%s-%s" % (bin_dir, APP_VERSION)
        os.rename(bin_dir, source_dir)
        arch = platform.architecture()[0]
        output_file = join(self.dist_dir, "crtaci-%s-%s.tar.gz" % (APP_VERSION, arch))
        with tarfile.open(output_file, "w:gz") as tar:
            tar.add(source_dir, arcname=basename(source_dir))

    def run(self):
        self.run_command("build_qt")
        set_rthook()
        run_build(self.dist_dir)
        self.copy_files()
        self.run_build_tarball()


def run_build(dist_dir):
    import PyInstaller.build
    work_path = join(dist_dir, "build")
    spec_file = join(dist_dir, "crtaci.spec")
    os.environ["BASE_DIR"] = BASE_DIR
    os.environ["DIST_DIR"] = dist_dir
    opts = {"distpath": dist_dir, "workpath": work_path, "clean_build": True, "upx_dir": None}
    PyInstaller.build.main(None, spec_file, True, **opts)

def set_rthook():
    import PyInstaller
    hook_file = ""
    module_dir = dirname(PyInstaller.__file__)
    rthook = join(module_dir, "loader", "rthooks", "pyi_rth_qt4plugins.py")
    with open(rthook, "r") as hook: data = hook.read()
    if "sip.setapi" not in data:
        lines = data.split("\n")
        for line in lines:
            hook_file += line + "\n"
            if "MEIPASS" in line:
                hook_file += "\nimport sip\n"
                hook_file += "sip.setapi('QString', 2)\n"
                hook_file += "sip.setapi('QVariant', 2)\n"
        with open(rthook, "w") as hook: hook.write(hook_file)


class clean_local(Command):
    pats = ['*.py[co]', '*_ui.py', '*_rc.py']
    excludedirs = ['.git', 'build', 'dist']
    user_options = []

    def initialize_options(self):
        pass

    def finalize_options(self):
        pass

    def run(self):
        for e in self._walkpaths('.'):
            os.remove(e)

    def _walkpaths(self, path):
        for root, _dirs, files in os.walk(path):
            if any(root == join(path, e) or root.startswith(
                    join(path, e, '')) for e in self.excludedirs):
                continue
            for e in files:
                fpath = join(root, e)
                if any(fnmatch(fpath, p) for p in self.pats):
                    yield fpath


class mybuild(build):
    def run(self):
        self.run_command("build_qt")
        build.run(self)


class myclean(clean):
    def run(self):
        self.run_command("clean_local")
        clean.run(self)

cmdclass = {
    'build': mybuild,
    'build_qt': build_qt,
    'build_exe': build_exe,
    'build_dmg': build_dmg,
    'build_bin': build_bin,
    'clean': myclean,
    'clean_local': clean_local
}

setup(
    name = "crtaci",
    version = APP_VERSION,
    description = "Good old cartoons",
    author = "Milan Nikolic",
    author_email = "gen2brain@gmail.com",
    license = "GNU GPLv3",
    url = "http://crtaci.rs",
    packages = ["crtaci", "crtaci.ui"],
    package_dir = {"": join("frontend", "python")},
    scripts = ["crtaci", join("backend", "crtaci-http")],
    requires = ["PyQt4"],
    platforms = ["Linux", "Windows", "Darwin"],
    cmdclass = cmdclass,
    data_files = [
        ("share/pixmaps", ["xdg/crtaci.png"]),
        ("share/applications", ["xdg/crtaci.desktop"])
    ]
)
