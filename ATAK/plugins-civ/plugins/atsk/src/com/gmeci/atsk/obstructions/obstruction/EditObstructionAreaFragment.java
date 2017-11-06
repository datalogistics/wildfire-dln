
package com.gmeci.atsk.obstructions.obstruction;

import android.view.View;

import com.gmeci.atsk.obstructions.ObstructionType;

public class EditObstructionAreaFragment extends EditObstructionFragment {

    private static final String TAG = "EditObstructionAreaFragment";

    @Override
    protected void UpdateSpinnerAdapter() {
        _typeSpinner.setup(ObstructionType.AREAS);
    }

    @Override
    protected void HideShowFields(String type) {
        super.HideShowFields(type);
        setVisibility(WIDTH_POSITION, View.GONE);
    }
}
