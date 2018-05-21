/* Configuration file - Configure all source info here .. remove all URL related info from other files */
var fs = require('fs'),
_ = require('underscore');

var self = {
  port : process.env.PORT || 42424,
  ENABLE_HTTPS : false,
  // Defaulting to self-signed certs 
  ssl : {
    key : './cert/server.key',
    cert : './cert/server.crt',
    ca :  './cert/ca.crt'
  },
  sslOpt : {
    // Example of domain
    'dlt.open.sice.indiana.edu' : {
      key : './cert/server.key',
      cert : './cert/server.crt' 
      // ca : "./ssl/dlt-client.csr"
    }
  },  
  nat_map_file : './misc/idms_nat_map',
  freegeoipUrl : "http://dlt.crest.iu.edu:8080",
  jnlpMap : {
    'download': {
      'template': './misc/dlt-client.jnlp.tmpl',
      'codebase': 'http://dlt.crest.iu.edu/webstart',
      'jarfile' : 'lib/dlt-client.jar'
    },
    'publish' : {
      'template': './misc/dlt-publisher.jnlp.tmpl',
      'codebase': 'http://dlt.crest.iu.edu/webstart',
      'jarfile' : 'lib/dlt-publisher.jar'
    }
  },
  // shoppingCart_logger : (function() {    
  //   var log = bunyan.createLogger({
  //     name: "dlt-web cart",
  //     streams : [
  //       {
  //         path: "./logs/bun.log"
  //       }]
  //   });
  //   return log.info;
  // })(),
  // Match exnodes using name if true , else use properties.metadata.scene_id
  exnodeMatchingFromName : true,
  exnodeParent_UsingSelfRef : true,
  // Try to login and maintain cookie for the following UNIS instances
  authArr : [],
  idms_server : "http://wdln-idms:9001",
  routeMap : {
    // Aggregate from the following by default
    'default': ['local'],
    // Empty array is ignored and goes to default , otherwise using this to aggregate
    'measurements' : [],
    'exnodes' : [],
    'nodes': [],
    'nodes_id' : [],
    'services': [],
    'services_id': [],
    'measurements': [],
    'measurements_id' : [],
    'metadata': [],
    'metadata_id' : [],
    'data': [],
    'data_id': [],
    'ports': [],
    'ports_id' : [],
    'wildfire' : ['policies']
  },
  // Add a callback to process data for various routes
  routeCb : {
    // All functions are present in routeCb.js
    'services' : "",
    'services_id' : ""
  },
  filterMap : {
    services : "serviceType=datalogistics:wdln:ferry,datalogistics:wdln:base",
    exnodes : "inline"
  },
  wsfilterMap : {
    services : '{"serviceType":{"in":["datalogistics:wdln:ferry,datalogistics:wdln:base"]}}'
  },
  serviceMap : {
    local : {
      url : "localhost",
      port : "9000",
      use_ssl : false
    },
    policies : {
      url: "wdln-idms",
      port: 9001,
      use_ssl: false
    },
    msu: {
	url : "msu-ps01.osris.org",
	port : "8888",
	use_ssl : false,
    },
    wsu: {
	url : "wsu-ps01.osris.org",
	port : "8888",
	use_ssl : false,
    },
    um: {
	url : "um-ps01.osris.org",
	port : "8888",
	use_ssl : false,
    },
    unis : {
      url : "unis.crest.iu.edu",
      port : "8888",
      use_ssl : false,
    },
    dev : {
      url : "dev.crest.iu.edu",
      port : "8888",
      key : null,
      cert : null,
      use_ssl : false
    },
    dlt : {
      url : "dlt.crest.iu.edu",
      port : "9000",
      key : "./ssl/dlt-client.pem",
      cert: "./ssl/dlt-client.pem",
      use_ssl: true
    },
    monitor : {
      url : "monitor.crest.iu.edu",
      port : "9000",
      key : null,
      cert : null,
      use_ssl : false
    },
    dlt_ms : {
      url : "dlt.crest.iu.edu",
      port : "9001",
      key : "./ssl/dlt-client.pem",
      cert : "./ssl/dlt-client.pem",
      use_ssl : true
    },
    monitor_ms : {
      url : "monitor.crest.iu.edu",
      port : "9001",
      key : null,
      cert : null,
      use_ssl : false
    }
  },
  sslOptions : {
    requestCert: true,
    rejectUnauthorized: false
  },
  usgs_row_searchurl : "http://earthexplorer.usgs.gov/EE/InventoryStream/pathrow",
  usgs_lat_searchurl : "http://earthexplorer.usgs.gov/EE/InventoryStream/latlong",
  usgs_api_credentials : {
    username : "indianadlt",
    password : "indiana2014"
  },
  db : {
    url : "mongodb://localhost:27017",
    name : "peri-auth",
    collection_name : "userDetails"
  },
  GITHUB_CLIENT: "",
  GITHUB_SECRET: "",
  SCHEMAS: {
  'networkresources': 'http://unis.crest.iu.edu/schema/20160630/networkresource#',
  'nodes': 'http://unis.crest.iu.edu/schema/20160630/node#',
  'domains': 'http://unis.crest.iu.edu/schema/20160630/domain#',
  'ports': 'http://unis.crest.iu.edu/schema/20160630/port#',
  'links': 'http://unis.crest.iu.edu/schema/20160630/link#',
  'paths': 'http://unis.crest.iu.edu/schema/20160630/path#',
  'networks': 'http://unis.crest.iu.edu/schema/20160630/network#',
  'topologies': 'http://unis.crest.iu.edu/schema/20160630/topology#',
  'services': 'http://unis.crest.iu.edu/schema/20160630/service#',
  'blipp': 'http://unis.crest.iu.edu/schema/20160630/blipp#',
  'metadata': 'http://unis.crest.iu.edu/schema/20160630/metadata#',
  'datum': 'http://unis.crest.iu.edu/schema/20160630/datum#',
  'data': 'http://unis.crest.iu.edu/schema/20160630/data#',
  'measurement': 'http://unis.crest.iu.edu/schema/20160630/measurement#'
  }
};

self.recurse_map = {
  [self.SCHEMAS.topologies]: ["domains", "networks", "paths", "nodes", "ports", "links"],
  [self.SCHEMAS.domains]: ["domains", "networks", "paths", "nodes", "ports", "links"],
  [self.SCHEMAS.networks]: ["nodes", "links"],
  [self.SCHEMAS.nodes]: ["ports"]
};

var deepObjectExtend = function(target, source) {
  for (var prop in source) {
    if (prop in target && typeof(target[prop]) == 'object' && typeof(source[prop]) == 'object')
      deepObjectExtend(target[prop], source[prop]);
    else
      target[prop] = source[prop];
  } 
  return target;
};

try {
  fs.accessSync("config.js",fs.R_OK);
  var config = require("./config");  
  self = deepObjectExtend(self,config);
} catch(e) {
  console.error("No config file exists - Create a config.js and do module.exports with JSON obj to override server properties",e);
}

module.exports = self;
