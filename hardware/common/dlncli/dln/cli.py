import warnings
warnings.filterwarnings("ignore")
import npyscreen as nps
import netifaces, os, socket, subprocess, glob
import argparse, datetime

from dln import manage, settings

class NameForm(nps.Form):
    def create(self):
        date = datetime.datetime.utcnow()
        tm = date.time()
        self.add(nps.TitleFixedText, name="Name:", editable=False)
        self.f_host = self.add(nps.Textfield, value=socket.gethostname())
        self.tester = self.add(nps.FixedText, editable=False)
        self.f_date = self.add(nps.TitleDateCombo, name="Current Time")
        self.f_hour = self.add(nps.TitleText, name="Hour:", value=str(tm.hour))
        self.f_minute = self.add(nps.TitleText, name="Minute:", value=str(tm.minute))
        self.f_second = self.add(nps.TitleText, name="Second:", value=str(tm.second))
        self.f_mode = self.add(nps.TitleSelectOne, name="Operation Mode",
                               values=["Base", "Ferry"], scroll_exit=True, max_height=5)
        self.f_measure = self.add(nps.TitleSelectOne, name="Measurements",
                                  values=["Enable"], scroll_exit=True, max_height=2)

    def beforeEditing(self):
        self.f_mode.value = self.parentApp.mode

    def afterEditing(self):
        self.parentApp.env["DLNNAME"] = self.f_host.value
        try:
            self.parentApp.env["DLNMODE"] = ["base", "ferry"][self.f_mode.value[0]]
        except IndexError:
            self.parentApp.env["DLNMODE"] = "base"
        self.parentApp.env["CURTIME"] = str(self.f_date.value)
        self.parentApp.env["MEASURE"] = bool(self.f_measure.value)
        self.parentApp.setNextForm("EXTERN")

class ExternalForm(nps.ActionForm):
    def create(self):
        self.f_extern = self.add(nps.TitleSelectOne, value=[0,],
                                 name="Select external facing interfaces", scroll_exit=True)

    def beforeEditing(self):
        self.f_extern.values = self.parentApp.ifaces

    def on_ok(self):
        app = self.parentApp
        app.env["DLN_CLIENTIF"] = [app.ifaces[x] for x in self.f_extern.value]
        for iface in app.env["DLN_CLIENTIF"] + ["eth0"]:
            try: app.ifaces.remove(iface)
            except ValueError: pass
        app.setNextForm("INTERN")

    def on_cancel(self):
        self.parentApp.setNextForm("MAIN")

class InternalForm(nps.ActionForm):
    def create(self):
        self.f_intern = self.add(nps.TitleSelectOne, value=[0,],
                                 name="Select internal facing interfaces", scroll_exit=True)

    def beforeEditing(self):
        self.f_intern.values = self.parentApp.ifaces

    def on_ok(self):
        app = self.parentApp
        app.env["DLN_MESHIF"] = [app.ifaces[x] for x in self.f_intern.value]
        app.setNextForm(None)

    def on_cancel(self):
        self.parentApp.setNextForm("EXTERN")

class DLNApp(nps.NPSAppManaged):
    @classmethod
    def read_env(cls):
        try:
            with open(settings.ENVFILE) as f:
                split = lambda x: (x[0], x[1])
                return dict([v.split("=") for v in f.readlines() if "=" in v])
        except OSError:
            return {}

    def __init__(self, dryrun):
        self.dryrun = dryrun
        self.ifaces = netifaces.interfaces()
        r_if = [v for v in self.ifaces]
        try: self.ifaces.remove('lo')
        except: pass
        for f in r_if:
            if f.startswith('br'):
                try: self.ifaces.remove(f)
                except: pass
        self.env = DLNApp.read_env()
        super().__init__()

    def onStart(self):
        self.mode = 0 if self.env.get("DLNMODE", "base") == "base" else 1
        self.registerForm("MAIN", NameForm())
        self.registerForm("EXTERN", ExternalForm())
        self.registerForm("INTERN", InternalForm())

    def onCleanExit(self):
        finalize_settings(self.dryrun, self.env)

def finalize_settings(dryrun, env):
    if not dryrun:
        os.makedirs(os.path.dirname(settings.ENVFILE), exist_ok=True)
    environment = "\n".join([f"{k}={v}" for k,v in env.items()])
    if dryrun:
        with open(dryrun, 'a') as f:
            f.write("FILE - envfile\n")
            f.write(environment)
            f.write("\n")
    else:
        with open(settings.ENVFILE, 'w') as f:
            f.write(environment)

    manage.write_config(dryrun, env['DLNMODE'],
                        env['DLN_CLIENTIF'],
                        env['DLN_MESHIF'],
                        env['DLNNAME'])

def start_config(dryrun): 
    if dryrun:
        with open(dryrun, 'a') as f:
            f.write(f"RUN - /opt/dlt/bin/dlnconfig stop")
    else:
        with subprocess.Popen(['/opt/dlt/bin/dlnconfig', 'stop']) as proc:
            if proc.stdout: print(proc.stdout.read())
        files = glob.glob('/etc/network/interfaces.d/*')
        for f in files:
            try: os.remove(f)
            except: pass

def end_config(dryrun, mode, host, meshif, clientif):
    clientif = clientif or ''
    if dryrun:
        with open(dryrun, 'a') as f:
            f.write(f"RUN - /opt/dlt/bin/dlnconfig start {mode} {host} {meshif} {clientif}")
    else:
        with subprocess.Popen(['/opt/dlt/bin/dlnconfig', 'start', mode, host, meshif, clientif]) as proc:
            if proc.stdout: print(proc.stdout.read())

def main():
    parser = argparse.ArgumentParser(description="CLI for the Wildfire Data Logistics Network")
    parser.add_argument('--dryrun', type=str, help="Print generated configuration only")
    parser.add_argument('operation', type=str, choices=["init", "reset", "hardreset", "service"], help="Operation to perform on ferry [init, reset, hardreset]")
    args = parser.parse_args()

    if args.dryrun:
        with open(args.dryrun, 'w') as f: pass

    if args.operation == 'init':
        start_config(args.dryrun)
        app = DLNApp(args.dryrun)
        app.run()
        clientif = app.env['DLN_CLIENTIF'][0] if app.env['MEASURE'] else False
        end_config(args.dryrun, app.env['DLNMODE'], app.env['DLNNAME'], app.env['DLN_MESHIF'][0], clientif)
    elif args.operation == 'reset':
        start_config(args.dryrun)
        env = DLNApp.read_env()
        clientif = env.get('DLN_CLIENTIF', 'eth0') if env.get('MEASURE', None) else False
        end_config(args.dryrun, env.get('DLNMODE', 'base'), env.get('DLNNAME', 'base00'), env.get('DLN_MESHIF', 'wlan0'), env.get('MEASURE', False), clientif)
    elif args.operation == 'hardreset':
        with open(settings.ENVFILE, 'w') as f: pass
        start_config(args.dryrun)
        finalize_settings(args.dryrun, {"DLNMODE": "base", "DLN_CLIENTIF": ["eth0"], "DLN_MESHIF": ["wlan0"], "DLNNAME": "base00"})
        end_config(args.dryrun, 'base', 'base00', 'wlan0', 'eth0')
    elif args.operation == 'service':
        start_config(args.dryrun)
        manage.service_mode(args.dryrun)
