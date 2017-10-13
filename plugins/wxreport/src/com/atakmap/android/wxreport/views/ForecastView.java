package com.atakmap.android.wxreport.views;


import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.atakmap.android.wxreport.data.Forecast;
import com.atakmap.android.wxreport.plugin.R;
import com.atakmap.android.wxreport.util.WeatherUtils;

/**
 * Created by Scott Auman @Par Government on 9/19/2017.
 * Custom view impl that displays as a traditional forecast view
 * given graphical representation of the projected forecast condition
 * with High / Low temperatures
 */

public class ForecastView extends LinearLayout {

    private Context _pluginContext;

    public ForecastView(Context context,Context pluginContext, Forecast forecast) {
        super(context);
        _pluginContext = pluginContext;
        init(forecast);
    }

    public ForecastView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ForecastView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    private void init(Forecast forecast){
        inflate(_pluginContext, R.layout.forecast_day_view,this);

        //map UI elements to java
        ImageView conditionImageView = (ImageView)findViewById(R.id.conditionImageView);
        TextView conditionTextView = (TextView) findViewById(R.id.conditionTextView);
        TextView highTempTextView = (TextView)findViewById(R.id.highTempTextView);
        TextView lowTempTextView = (TextView) findViewById(R.id.lowTempTextView);
        TextView dayOfWeek = (TextView) findViewById(R.id.dayOfWeekTextView);
        TextView dateTextView = (TextView) findViewById(R.id.dateTextView);

        dayOfWeek.setText(WeatherUtils.getDayOfWeek(forecast.getSelected_date()));
        dateTextView.setText(WeatherUtils.getDate(forecast.getSelected_date()));
        conditionImageView.setImageDrawable(forecast.get_conditionImage(_pluginContext));
        conditionTextView.setText(forecast.getDescription());
        highTempTextView.setText("High: " + WeatherUtils.convertTempToCurrentFormat(getContext(),forecast.getHiTemperature()));
        lowTempTextView.setText("Low: " + WeatherUtils.convertTempToCurrentFormat(getContext(),forecast.getLoTemperature()));
    }
}
