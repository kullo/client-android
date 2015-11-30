#!/bin/bash

# Show IPs
ifconfig| fgrep "inet " | cut -d' ' -f 12-13

cd ./www
python -m SimpleHTTPServer 8000
