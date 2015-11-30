#!/usr/bin/env python3

import json
import os
import subprocess

source_file = "icon.svg"
export_dir = "export"

sizes = {
    'ldpi': 36,
    'mdpi': 48,
    'hdpi': 72,
    'xhdpi': 96,
    'xxhdpi': 144,
    'xxxhdpi': 192
}

try:
    os.makedirs(export_dir)
except OSError:
    print("Directory '%s' already exists" % export_dir)

for usage in sorted(sizes):
    size = sizes[usage]
    outdir = "%s/mipmap-%s" % (export_dir, usage)
    outfile = os.path.join(outdir, "ic_launcher.png")
    print("%s ..." % outfile)

    try:
        os.makedirs(outdir)
    except OSError:
        print("Directory '%s' already exists" % outdir)

    cmd = [
        '/usr/bin/inkscape',
        '--without-gui',
        "--file=" + source_file,
        "--export-png=" + outfile,
        "--export-width=" + str(size)
    ]
    subprocess.call(cmd)

