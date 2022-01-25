from dln.settings import HAPD_PATH, IFILE_PATH, DNS_PATH
from os import path

T_BRIDGE = """
auto br-extern
iface br-extern inet static
  bridge_ports eth0 {iface}
  address 10.1.0.1
  netmask 255.255.0.0
"""

T_IF = """
auto {iface}
iface {iface} inet {mode}
  {body}
"""

T_HOSTAPD = """
interface={iface}
{br}
driver=nl80211
country_code=US

ssid={ssid}

logger_syslog=0
logger_syslog_level=4
logger_stdout=-1
logger_stdout_level=0

hw_mode=a
ieee80211n=1
require_ht=1
ieee80211ac=1
require_vht=1

vht_oper_chwidth=1
channel={ch}
vht_oper_cent_freq_seg0_idx={ch}
"""

T_WPA = """
\twpa-ssid WDLNMESH
\tpre-up wpa_supplicant -B -Dnl80211,wext -i {iface} -c /etc/wpa_supplicant/wpa_supplicant.conf -f /var/log/wpa_supplicant.log
\tpost-down wpa_cli -i {iface} terminate
"""

def _write_file(dryrun, path, text):
    if dryrun:
        with open(dryrun, 'a') as f:
            f.write(f"FILE - {path}")
            f.write(text)
            f.write("\n")
    else:
        try:
            with open(path, 'w') as f:
                f.write(text)
        except OSError as e:
            print(f"File error - {e}")

def _setup_extern(dryrun, iface):
    hapd_p = path.join(HAPD_PATH, f"{iface}.conf")
    d = {
        "br": "" if iface == "eth0" or iface.startswith("w") else iface,
        "m": "manual",
        "b": f"hostapd {hapd_p}" if iface.startswith("w") else ""
    }
    files = {
        path.join(IFILE_PATH, "br-extern"): T_BRIDGE.format(iface=d['br']),
        path.join(IFILE_PATH, iface): T_IF.format(iface=iface, mode=d['m'], body=d['b']),
        path.join(IFILE_PATH, "eth0"): T_IF.format(iface="eth0", mode=d['m'], body=""),
        hapd_p: T_HOSTAPD.format(iface=iface, br="bridge=br-extern", ssid="WDLN", ch=58)
    }
    for fpath, text in files.items():
        _write_file(dryrun, fpath, text)

def _setup_intern(dryrun, iface, mode, host):
    hapd_p = path.join(HAPD_PATH, f"{iface}.conf")
    d = {
        "if": iface,
        "m": "static" if mode == "base" else "dhcp",
    }
    d['b'] = ""
    if mode == "base":
        d['b'] = f"\taddress 10.0.0.1\n\tnetmask 255.255.0.0"
        if iface.startswith("w"): d['b'] += f"\n\thostapd {hapd_p}"
    else:
        d['b'] = f"\thostname {host}"
        if iface.startswith("w"):
            d['b'] += T_WPA.format(iface=iface)

    files = {
        path.join(IFILE_PATH, iface): T_IF.format(iface=iface, mode=d['m'], body=d['b']),
        hapd_p: T_HOSTAPD.format(iface=iface, br="", ssid="WDLNMESH", ch=122)
    }
    for fpath, text in files.items():
        _write_file(dryrun, fpath, text)

def write_config(dryrun, mode, client, mesh, host):
    _setup_extern(dryrun, client[0])
    _setup_intern(dryrun, mesh[0], mode, host)
    _write_file(dryrun, path.join(DNS_PATH, "ifaces.conf"), f"""
    interface={mesh[0]}
    """)
    _write_file(dryrun, "/etc/hostname", host)