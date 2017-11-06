
package com.gmeci.atskservice.form;

import com.gmeci.atskservice.R;

import com.gmeci.atskservice.form.formobjects.TextField;
import com.gmeci.atskservice.form.formobjects.CheckField;
import com.gmeci.atskservice.form.formobjects.RadioField;
import com.gmeci.atskservice.form.formobjects.FormObject;
import com.gmeci.atskservice.form.formobjects.Break;
import com.gmeci.atskservice.form.formobjects.SimpleNameValue;
import com.gmeci.atskservice.form.formobjects.NestBlock;
import com.gmeci.atskservice.form.formobjects.Block;
import com.gmeci.atskservice.form.formobjects.Label;

import java.util.*;

public class LTFWLZForm extends LZForm
{

    @Override
    protected String[] getRestrictions() {
        return lusa(R.array.restrictionsltfw);
    }

    @Override
    protected String getLeftShoulderName() {
        return "Left A-ZONE";
    }

    @Override
    protected String getRightShoulderName() {
        return "Right A-ZONE";
    }

    @Override
    protected String getLeftGradedAreaName() {
        return "Left B-ZONE";
    }

    @Override
    protected String getRightGradedAreaName() {
        return "Right B-ZONE";
    }

    @Override
    protected String getLeftMaintainedAreaName() {
        return null;
    }

    @Override
    protected String getRightMaintainedAreaName() {
        return null;
    }

}
