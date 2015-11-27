
import os
import platform
import sys
import subprocess

def java(classpath, classname, args):
    cmd = []
    if platform.system().lower() == 'darwin':
        cmd = ['/usr/libexec/java_home', '-v', '1.8', '--exec']
    cmd += [ 'java', '-cp', ':'.join(classpath), classname ] + args
    return subprocess.call(cmd)

def run(classname):
    root = os.path.abspath(
        os.path.join(os.path.dirname(__file__), '..'))
    jars = ['dst/dep.jar', 'dst/app.jar']
    return java(
        [os.path.join(root, j) for j in jars],
        classname,
        sys.argv[1:])
