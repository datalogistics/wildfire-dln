import os

def fullpath(v):
    return os.path.abspath(os.path.expanduser(v))

HAPD_PATH = fullpath(os.environ.get('DLN_HAPDDIR') or "/etc/hostapd/")
CONF_PATH = fullpath(os.environ.get('DLN_CONFDIR') or "/etc/dlt/")
IFILE_PATH = fullpath(os.environ.get('DLN_IFILEPATH') or "/etc/network/interfaces.d/")
DNS_PATH = fullpath(os.environ.get('DLN_DNSPATH') or "/etc/dlt/dnsmasq.d/")
ENVFILE = fullpath(os.environ.get('DLN_ENVFILE') or os.path.join(CONF_PATH, "environment"))
