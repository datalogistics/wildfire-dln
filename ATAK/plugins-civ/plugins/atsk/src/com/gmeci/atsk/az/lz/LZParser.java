
package com.gmeci.atsk.az.lz;

import com.gmeci.core.Criteria;
import com.gmeci.core.CriteriaParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Collections;
import java.util.Comparator;
import com.atakmap.coremap.log.Log;

public class LZParser {
    public static final String TAG = "LZParser";

    private final CriteriaParser acParser = new CriteriaParser();

    private final Map<String, Criteria> map = new HashMap<String, Criteria>();
    private List<String> aircraftNames;

    static LZParser _instance = null;

    private LZParser() {
    }

    synchronized public static final LZParser getInstance() {
        if (_instance == null) {
            _instance = new LZParser();
            _instance.init();
        }
        return _instance;
    }

    private void init() {

        try {
            acParser.parseFile();
            List<Criteria> builtin = acParser.GetAircraftList();
            for (Criteria ac : builtin) {
                Log.d(TAG, "loading builtin aircraft data for: " + ac.getName());
                synchronized (this) {
                    map.put(ac.getName(), ac);
                }
            }
        } catch (IOException e) {
            Log.d(TAG, "error loading aircraft data", e);
        }

    }

    synchronized public List<String> GetAircraftNames() {
        if (aircraftNames == null) {
            Iterator<String> it = map.keySet().iterator();
            aircraftNames = new ArrayList<String>();
            while (it.hasNext()) {
                aircraftNames.add(it.next());
            }
            Collections.sort(aircraftNames, new Comparator<String>() {
                public int compare(String s1, String s2) {
                    if (s1.equals("NONE")) {
                        if (s1.equals(s2))
                            return 0;
                        return -1;
                    } else if (s2.equals("NONE")) {
                        return 1;
                    }
                    return s1.compareToIgnoreCase(s2);
                }
            });

        }
        return aircraftNames;
    }

    synchronized public Criteria GetAircraftObject(final String name) {
        return map.get(name);
    }

    synchronized public Criteria GetAircraftByName(final String name) {
        return map.get(name);
    }

}
