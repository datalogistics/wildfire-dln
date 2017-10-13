
package com.gmeci.atsk;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.atakmap.android.atsk.plugin.R;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.atsk.resources.HostFragmentBase;
import com.gmeci.core.SurveyData.AZ_TYPE;

public class ATSKNavigationFragment extends HostFragmentBase implements
        OnClickListener {

    public static final String TAG = "ATSKNavigationFragment";
    private boolean _created = false;
    private ImageView _obsBtn;
    private ImageView _gradBtn;
    private ImageView _azCriteria, _azRemark, _azExport,
            _azVehicle, _vizBtn, _galleryBtn;
    private String _azType;
    private View _root;
    private ATSKFragmentManager _manager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        Context pluginContext = ATSKApplication
                .getInstance().getPluginContext();

        LayoutInflater pluginInflater = LayoutInflater.from(pluginContext);

        _root = pluginInflater.inflate(R.layout.atsk_navigation_frame,
                container,
                false);

        _created = false;

        return _root;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        _obsBtn = (ImageView) _root.findViewById(R.id.atsk_main_image_obs);
        _obsBtn.setOnClickListener(this);
        _gradBtn = (ImageView) _root.findViewById(R.id.atsk_main_image_grad);
        _gradBtn.setOnClickListener(this);
        _azCriteria = (ImageView) _root.findViewById(R.id.az_criteria);
        _azCriteria.setOnClickListener(this);
        _azRemark = (ImageView) _root.findViewById(R.id.az_remark);
        _azRemark.setOnClickListener(this);
        _azExport = (ImageView) _root.findViewById(R.id.az_export);
        _azExport.setOnClickListener(this);
        _azVehicle = (ImageView) _root.findViewById(R.id.az_vehicle);
        _azVehicle.setOnClickListener(this);
        _vizBtn = (ImageView) _root.findViewById(R.id.az_visibility);
        _vizBtn.setOnClickListener(this);
        _galleryBtn = (ImageView) _root.findViewById(R.id.az_gallery);
        _galleryBtn.setOnClickListener(this);

        _created = true;
        if (_manager != null)
            highlightButton(_manager.getCurrentFragmentTag());

        setAZType(_azType);
    }

    // Single (home) fragment implementation
    private void setAZButtons() {
        if (!_created || _azType == null)
            return;
        // Only show gradient button for Landing Zone
        if (_azType.equals(AZ_TYPE.LZ.toString()))
            _gradBtn.setVisibility(View.VISIBLE);
        else
            _gradBtn.setVisibility(View.GONE);
    }

    public void setFragmentManager(ATSKFragmentManager manager) {
        _manager = manager;
    }

    public void setAZType(String az) {
        _azType = az;
        setAZButtons();
        //we should update the map here too.
    }

    @Override
    public void onClick(View v) {
        if (_azType != null)
            _manager.fragmentSelected(getButtonType(v), _azType);
    }

    public void highlightButton(String type) {
        // Can't highlight button if it doesn't exist yet
        if (!_created || type == null)
            return;
        // Ignore highlight on remarks button
        if (type.equals(ATSKFragment.REMARKS))
            return;
        _obsBtn.setImageResource(0);
        _gradBtn.setImageResource(0);
        _azCriteria.setImageResource(0);
        _azRemark.setImageResource(0);
        _azExport.setImageResource(0);
        _azVehicle.setImageResource(0);
        _vizBtn.setImageResource(0);
        _galleryBtn.setImageResource(0);
        ImageView iv = getNavButton(type);
        if (iv != null) {
            iv.setImageResource(R.drawable.navigation_highlight);
        }
    }

    public ImageView getNavButton(String type) {
        if (type.equals(ATSKFragment.CRITERIA))
            return _azCriteria;
        if (type.equals(ATSKFragment.REMARKS))
            return _azRemark;
        if (type.equals(ATSKFragment.EXPORT))
            return _azExport;
        if (type.equals(ATSKFragment.VEHICLE))
            return _azVehicle;
        if (type.equals(ATSKFragment.GRAD))
            return _gradBtn;
        if (type.equals(ATSKFragment.OBS))
            return _obsBtn;
        if (type.equals(ATSKFragment.VIZ))
            return _vizBtn;
        if (type.equals(ATSKFragment.IMG))
            return _galleryBtn;
        return null;
    }

    public String getButtonType(View v) {
        if (v.equals(_azCriteria))
            return ATSKFragment.CRITERIA;
        else if (v.equals(_azRemark))
            return ATSKFragment.REMARKS;
        else if (v.equals(_azExport))
            return ATSKFragment.EXPORT;
        else if (v.equals(_azVehicle))
            return ATSKFragment.VEHICLE;
        else if (v.equals(_gradBtn))
            return ATSKFragment.GRAD;
        else if (v.equals(_obsBtn))
            return ATSKFragment.OBS;
        else if (v.equals(_vizBtn))
            return ATSKFragment.VIZ;
        else if (v.equals(_galleryBtn))
            return ATSKFragment.IMG;
        return null;
    }
}
