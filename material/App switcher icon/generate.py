#!/usr/bin/env python3

import json
import os
import subprocess

source_file = "kullo_app_switcher.svg"
out_filename = "kullo_app_switcher.png"
export_dir = "export"

# http://iconhandbook.co.uk/reference/chart/android/
sizes = {
    'mdpi': 32,
    'hdpi': 48,
    'xhdpi': 64,
    'xxhdpi': 96,
    'xxxhdpi': 128
}

try:
    os.makedirs(export_dir)
except OSError:
    print("Directory '%s' already exists" % export_dir)

for usage in sorted(sizes):
    size = sizes[usage]
    outdir = "%s/drawable-%s" % (export_dir, usage)
    outfile = os.path.join(outdir, out_filename)
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

