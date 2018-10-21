package android.and09.multiweatherapp;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class WeatherFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        return inflater.inflate(R.layout.weather_fragment, container, false);
    }

    //AND10D S55 -> We unblock the ConditionVariable from the class WeatherActivity, once the Weather
    //Fragment is initialized. That is detected when Android calls the Method onViewCreated()
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState){
        ((WeatherActivity) getActivity()).fragmentReady.open();
    }
}
