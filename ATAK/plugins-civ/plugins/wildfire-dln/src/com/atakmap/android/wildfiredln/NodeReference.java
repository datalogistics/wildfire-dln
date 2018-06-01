package com.atakmap.android.wildfiredln;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.atakmap.coremap.maps.coords.GeoPoint;

import java.util.Random;
import java.util.Vector;

public class NodeReference
{
    private String name;
    private String id;
    private Double lat;
    private Double lon;

    public NodeReference(String name, String id, Double latitude, Double longitude)
    {
        this.name = name;
        this.id = id;
        lat = latitude;
        lon = longitude;
    }

    public String getName()
    {
        return name;
    }

    public String getID()
    {
        return id;
    }

    public GeoPoint getLocation()
    {
        if(lat == Double.NEGATIVE_INFINITY || lon == Double.NEGATIVE_INFINITY)
        {
            return null;
        }

        return new GeoPoint(lat,lon);
    }

    @Override
    public boolean equals(Object obj)
    {

        if(obj == null)
        {
            return false;
        }

        if(!NodeReference.class.isAssignableFrom(obj.getClass()))
        {
            return false;
        }

        final NodeReference nr = (NodeReference)obj;

        if(!name.equals(nr.name))
        {
            return false;
        }

        if(!id.equals(nr.id))
        {
            return false;
        }

        return true;
    }

    static Vector<NodeReference> NodeParser(String host, String string2parse)
    {
        Vector<NodeReference> references = new Vector<NodeReference>();

        try
        {
            JSONArray arr = new JSONArray(string2parse);

            for(int i=0;i<arr.length();i++)
            {
                JSONObject obj = arr.getJSONObject(i);
                String name = obj.getString("name");
                String id = obj.getString("id");

                JSONObject pos = obj.getJSONObject("location");
                double lat = Double.NEGATIVE_INFINITY;
                double lon = Double.NEGATIVE_INFINITY;

                if(pos.has("latitude"))
                {
                    lat = pos.getDouble("latitude");
                }

                if(pos.has("longitude"))
                {
                    lon = pos.getDouble("longitude");
                }

                NodeReference n = new NodeReference(name,id,lat,lon);

                //Random r = new Random();
                //NodeReference n  = new NodeReference(name, id,30+r.nextDouble()*20,-70-r.nextDouble()*40);


                references.add(n);
            }


        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }


        //Log.d(TAG, Log.getStackTraceString(e));

        return references;
    }

    public NodeReference copy()
    {
        return new NodeReference(name,id,lat,lon);
    }

    static void AddUnique(Vector<NodeReference> master, Vector<NodeReference> slave)
    {
        if(master != null && slave != null)
        {
            for (int i = 0; i < slave.size(); i++)
            {
                if (!master.contains(slave.get(i)))
                {
                    master.add(slave.get(i));
                }
            }
        }
    }
}
