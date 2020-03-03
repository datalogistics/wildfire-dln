import argparse, configparser, copy, os, logging


def _expandvar(x, default):
    v = os.path.expandvars(x)
    return default if v == x else v

class MultiConfig(object):
    CONFIG_FILE_VAR = "$PYTHON_CONFIG_FILENAME"

    def __init__(self, defaults, desc=None, *, filevar=None):
        self.CONFIG_FILE_VAR = filevar or self.CONFIG_FILE_VAR
        self.defaults, self._desc = defaults, (desc or "")
        self.loglevels = {'NOTSET': logging.NOTSET, 'ERROR': logging.ERROR,
                          'WARN': logging.WARNING, 'INFO': logging.INFO,
                          'DEBUG': logging.DEBUG}

    def _from_file(self, path):
        result, tys = {}, { "true": True, "false": False, "none": None, "": None }
        if path:
            parser = configparser.ConfigParser(allow_no_value=True)
            try:
                parser.read(path)
                for section,body in parser.items():
                    result[section] = {}
                    for k,v in body.items():
                        result[section][k] = tys.get(v, v)
            except OSError: pass
        return result

    def _setup_logging(self, filename):
        logging.config.fileConfig(filename)

    def add_loglevel(self, n, v): self.loglevels[n] = v

    def from_parser(self, parsers, *, include_logging=False, general_tag="general"):
        parsers = parsers if isinstance(parsers, list) else [parsers]
        internal = argparse.ArgumentParser(self._desc, parents=parsers, add_help=False)
        internal.add_argument('-c', '--configfile', type=str, help='Path to the program configuration file')
        if include_logging:
            internal.add_argument('--logfile', type=str, help='Path to the logging configuration file')
            internal.add_argument('--loglevel', type=str, default='NOTSET', choices=self.loglevels.keys(), help='Set the log level of the root logger')

        args = internal.parse_args()
        filepath = args.configfile or _expandvar(self.CONFIG_FILE_VAR, "")
        result = copy.deepcopy(self.defaults)
        for section,body in self._from_file(filepath).items():
            if section not in result: result[section] = {}
            if section == general_tag: [result.__setitem__(k, v) for k,v in body.items()]
            else: [result[section].__setitem__(k, v) for k,v in body.items()]
        for k,v in args.__dict__.items():
            block, path = result, k.split('.')
            for section in path[:-1]:
                if section not in block: block[section] = {}
                block = block[section]
            if v is not None: block[path[-1]] = v

        if include_logging:
            logging.getLogger().setLevel(self.loglevels[args.loglevel])
            if args.logfile: self._setup_logging(args.logfile)
        return result
