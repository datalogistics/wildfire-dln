
package com.gmeci.atsk.gradient;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

import com.atakmap.android.atsk.plugin.R;
import com.atakmap.coremap.log.Log;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.atskservice.resolvers.ObstructionProviderClient;

public class EditSurfaceDistressFragment extends GradientTabBase {
    private static final String TAG = "EditSurfaceDistressFragment";
    GradientTabHost parentFragment;
    Button SaveButton, CancelButton;
    ObstructionProviderClient opc;

    public void setParentInterface(GradientTabHost gradientInterface) {
        this.parentFragment = gradientInterface;
    }

    void UpdateSpinnerAdapter() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        Context pluginContext = ATSKApplication
                .getInstance().getPluginContext();
        return LayoutInflater.from(pluginContext).inflate(
                R.layout.gradient_surface_distress_edit,
                container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "CreateView");
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated");
        SaveButton = (Button) view.findViewById(R.id.save);
        SaveButton.setVisibility(View.VISIBLE);
        SaveButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                SaveEditedPoint();
                //        boolean PointDeleted = opc.DeletePoint(ATSKConstants.DEFAULT_GROUP, ATSKConstants.TEMP_POINT_UID, true);

            }

        });
        CancelButton = (Button) view.findViewById(R.id.cancel);
        CancelButton.setVisibility(View.VISIBLE);
        CancelButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                parentFragment.CloseEditWindow(true);
                //        boolean PointDeleted = opc.DeletePoint(ATSKConstants.DEFAULT_GROUP, ATSKConstants.TEMP_POINT_UID, true);

            }

        });
        notifyTabHost();
    }

    boolean SaveEditedPoint() { //Store this obstruction
        /*    String CurrentType= TypeSpinner.getSelectedItem().toString();
            CurrentObstruction.type =CurrentType;
            CurrentObstruction.uid =clickedUID;
            
            CurrentObstruction.remark = CurrentRemark;
                
            parentFragment.AddPointObstruction(this.CurrentObstruction);*/
        parentFragment.CloseEditWindow(true);
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");

        opc = new ObstructionProviderClient(this.getActivity());
        opc.Start();

        if (opc != null) {//use the opc to set up the screen
            //            InitializeScreen();
        }
    }//onResume 

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        opc.Stop();
        //when tabs switch it goes to onPause.....
        //DeleteTemporaryPoints();

    }
    /*
        double StartingLat, StartingLon;
        PointObstruction origionalPoint;
        private void InitializeScreen() {
            origionalPoint = opc.GetPointObstruction(this.clickedGroup,this.clickedUID);
            this.CurrentObstruction = new PointObstruction(origionalPoint);
            
            TypeSpinner.setSelection(TypeSpinner.GetPosition(origionalPoint.type));
            origionalPoint.uid = clickedUID;
            origionalPoint.group = clickedGroup;
            CurrentRemark = origionalPoint.remark;
            UpdateDisplayMeasurements();
            
        }


        public void UpdateType(String NewType)
        {
            AddTemporaryPointAllSources();
        }
        
        boolean AddTemporaryPointAllSources()
        {    //Store this obstruction
            PointObstruction newPointObstruction= new PointObstruction(CurrentObstruction);
            String CurrentType= TypeSpinner.getSelectedItem().toString();
            newPointObstruction.type =CurrentType;
            newPointObstruction.uid = ATSKConstants.TEMP_POINT_UID;
            newPointObstruction.remark = CurrentRemark;
            parentFragment.AddPointObstruction(newPointObstruction);
            
            LineObstruction TempLine = new LineObstruction();
            TempLine.uid = ATSKConstants.TEMP_LINE_UID;
            TempLine.group = ATSKConstants.DEFAULT_GROUP;
            TempLine.type=Constants.LO_GENERIC_ROUTE;
            TempLine.points.add(new SurveyPoint(CurrentObstruction.lat, CurrentObstruction.lon));
            TempLine.points.add(new SurveyPoint(StartingLat, StartingLon));
            opc.NewLine(TempLine);
            
            
            return false;
        }
        
        @Override
        boolean NewPosition(double lat, double lon, double elevation_m, double ce, float le, boolean TopCollected) {

            CurrentCollectingTop =TopCollected;
            switch (CurrentlyEditedIndex)
            {
                case LOCATION_POSITION:
                {
                    CurrentObstruction.lat = lat;
                    CurrentObstruction.lon = lon;
                    StoredPositionIndex =0;
                    CurrentObstruction.TopCollected = TopCollected;
                    //if in top mode - set height from this mode also?
                    if(TopCollected)
                    {
                        CurrentObstruction.alt = elevation_m- CurrentObstruction.height;
                    }
                    else
                    {
                        CurrentObstruction.alt = elevation_m;
                    }
                    CurrentObstruction.circularError=ce;
                    CurrentObstruction.linearError=le;
                    //draw temp point AND temp line????
                    //delete temp point and temp line in cancel/save
                    AddTemporaryPointAllSources();
                    break;
                }
                case ALT_POSITION:
                {
                    CurrentObstruction.TopCollected = TopCollected;
                    if(TopCollected)
                    {
                        CurrentObstruction.alt = elevation_m- CurrentObstruction.height;
                    }
                    else
                    {
                        CurrentObstruction.alt = elevation_m;
                    }
                    break;
                }
                case NAME_POSITION:
                {
                    //throw the position away or show RAB?
                    StoredPositionIndex=0;
                    break;
                }
                case ROTATION_POSITION:
                {
                    if(StoredPositionIndex==0)
                    {
                        StoredPositionIndex++;
                        StoredPosition.lat = lat;
                        StoredPosition.lon = lon;
                        StoredPosition.alt = elevation_m;
                        this.parentFragment.UpdateNotification("ROTATION COLLECTION", "", "WAITING FOR 2nd ROTATION POINT", "");
                    }
                    else
                    {
                        StoredPositionIndex = 0;
                        double Angle = (float) Conversions.CalculateAngledeg(StoredPosition.lat, StoredPosition.lon, lat, lon);
                        StoreMeasurement(Angle);
                        this.parentFragment.UpdateNotification("ROTATION COLLECTION", "", "COLLECITON COMPLETECOMPLETE", "");
                    }
                    break;
                }
                case HEIGHT_POSITION:
                {
                    
                    if(this.StoredPositionIndex==0)
                    {
                        //save first elevation (Top)
                        StoredPosition.alt = elevation_m;
                        StoredPositionIndex++;
                        this.parentFragment.UpdateNotification("HEIGHT COLLECTION", "", "WAITING FOR BOTTOM ELEVATION", "");
                    }
                    else 
                    {
                        double NewHeight = StoredPosition.alt- elevation_m;
                        //second point - save the height and we're done
                        if(CurrentObstruction.TopCollected)
                        {
                            CurrentObstruction.alt = CurrentObstruction.alt+ CurrentObstruction.height - NewHeight;
                        }
                        StoredPositionIndex =0;
                        UpdateDisplayMeasurements();
                    }
                    if(this.ObstructionBarVisible())
                    {
                        this.setOBState(ATSKIntentConstants.OB_STATE_HIDDEN);
                    }
                    else
                    {
                        this.setOBState(ATSKIntentConstants.OB_STATE_POINT);
                    }
                    //height positon can be 1 position required or 2...
                    break;
                }
                default:
                {
                    //these are 2 point kinda values
                    if(StoredPositionIndex==0)
                    {
                        StoredPositionIndex++;
                        StoredPosition.lat = lat;
                        StoredPosition.lon = lon;
                        StoredPosition.alt = elevation_m;
                    }
                    else
                    {
                        StoredPositionIndex = 0;
                        double Range_m = (float) Conversions.CalculateRangem(StoredPosition.lat, StoredPosition.lon, lat, lon);
                        StoreMeasurement(Range_m);
                    }
                }
            }
            UpdateDisplayMeasurements();
            return false;
        }

        @Override
        protected boolean TVCLicked(int HeightPoint2Collect) {
            if(parentFragment==null)
                return false;
            StoredPositionIndex = 0;
            
            if(this.ObstructionBarVisible())
            {
                this.setOBState(ATSKIntentConstants.OB_STATE_HIDDEN);
                return false;
            }
            else
            {
                this.setOBState(ATSKIntentConstants.OB_STATE_REQUESTED_POINT);
                return true;
            }
        }

        private boolean PointHasLW(String TypeSelected) {

            for(int i=0;i<Constants.POINT_TYPES_WITH_LW.length;i++)
            {
                if(Constants.POINT_TYPES_WITH_LW[i].compareTo(TypeSelected)==0)
                {
                    return true;
                }
            }
            return false;
        }
        
        @Override
        protected boolean HideShowFields(String TypeSelected) {
            if(PointHasLW(TypeSelected))
            {
                //hide diameter
                StaticTVs[DIAMETER_POSITION].setVisibility(View.GONE);
                LiveTVs[DIAMETER_POSITION].setVisibility(View.GONE);
                UnitsTV[DIAMETER_POSITION].setVisibility(View.GONE);

                StaticTVs[LENGTH_POSITION].setVisibility(View.VISIBLE);
                StaticTVs[WIDTH_POSITION].setVisibility(View.VISIBLE);
                StaticTVs[ROTATION_POSITION].setVisibility(View.VISIBLE);
                LiveTVs[LENGTH_POSITION].setVisibility(View.VISIBLE);
                LiveTVs[WIDTH_POSITION].setVisibility(View.VISIBLE);
                LiveTVs[ROTATION_POSITION].setVisibility(View.VISIBLE);
                
                UnitsTV[LENGTH_POSITION].setVisibility(View.VISIBLE);
                UnitsTV[WIDTH_POSITION].setVisibility(View.VISIBLE);
                UnitsTV[ROTATION_POSITION].setVisibility(View.VISIBLE);
            }
            else
            {
                //hide l/w/rotation
                StaticTVs[DIAMETER_POSITION].setVisibility(View.VISIBLE);
                LiveTVs[DIAMETER_POSITION].setVisibility(View.VISIBLE);
                UnitsTV[DIAMETER_POSITION].setVisibility(View.VISIBLE);

                StaticTVs[LENGTH_POSITION].setVisibility(View.GONE);
                StaticTVs[WIDTH_POSITION].setVisibility(View.GONE);
                StaticTVs[ROTATION_POSITION].setVisibility(View.GONE);
                LiveTVs[LENGTH_POSITION].setVisibility(View.GONE);
                LiveTVs[WIDTH_POSITION].setVisibility(View.GONE);
                LiveTVs[ROTATION_POSITION].setVisibility(View.GONE);
                
                UnitsTV[LENGTH_POSITION].setVisibility(View.GONE);
                UnitsTV[WIDTH_POSITION].setVisibility(View.GONE);
                UnitsTV[ROTATION_POSITION].setVisibility(View.GONE);
                //    UnitsTV[ROTATION_POSITION].setVisibility(View.GONE);
            }

            return false;
        }

        @Override
        String getSettingModifier() {
            
            return "Point";
        }

        ObstructionProviderClient opc;
        String clickedGroup, clickedUID;
        public void setBaseOPC(ObstructionProviderClient opc, String clickedGroup,String clickedUID) {
            this.opc = opc;
            this.clickedGroup = clickedGroup;
            this.clickedUID = clickedUID;
            if(this.StaticTVs[DIAMETER_POSITION]==null)
            {
                //don't use it
            }
            else
            {
                //use the opc to set up the screen
                InitializeScreen();
            }
            
        }
    */
}
