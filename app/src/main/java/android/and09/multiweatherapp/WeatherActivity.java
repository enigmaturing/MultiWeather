package android.and09.multiweatherapp;

import android.and09.weatherapi.*;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import org.w3c.dom.Text;

public class WeatherActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);
        // Test of the weather-api in a thread.
        // In order to do that, we create an instance of the inner class WeatherRequestTask, that
        // extends the abstract class AsyncTask
        WeatherRequestTask task = new WeatherRequestTask();
        // And then we call its method execute(), to fire the thread
        task.execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_weather, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public class WeatherRequestTask extends AsyncTask<Void, Void, IWeatherAPI> {

        @Override
        protected IWeatherAPI doInBackground(Void... voids) {
            // We collect the location back from the SharedPrefrences
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this);
            String locationName = prefs.getString("location_name", "");
            String weatherProviderClass = prefs.getString("weather_provider_class", "OpenWeatherMapAPI");
            // We create an instance of our api here, not in the main thread
            IWeatherAPI api = null;
            try{
                // Solution exposed in  AND10D S.13, using a fix provider:
                // api = OpenWeatherMapAPI.fromLocationName(locationName);
                // Solution exposed in AND10D S.36, in order to get the corresponding api depending on the selected provider:
                api = WeatherAPIFactory.fromLocationName(weatherProviderClass, locationName);
            }catch (Exception ex){
                Log.e(getClass().getSimpleName(), ex.toString());
            }
            return api;
        }

        @Override
        protected void onPostExecute(IWeatherAPI api){
            // It is possible that the method doInBackground returns no api object, caused for example
            // by a missing internet connection. We have to contemplate that case:
            if (api == null){
                Log.e(getClass().getSimpleName(), "Rückgabe von doInBackground ist null: gibt es keine Interneverbindung?");
                return;
            }
            // In case an api object was sucessfully created by doInBackground:
            try{
                //Show data collected by the api on logcat
                Log.d(getClass().getSimpleName(), "Temperatur: " + api.getTemperature());
                Log.d(getClass().getSimpleName(), "Beschreibung: " + api.getDescription());
                Log.d(getClass().getSimpleName(), "Provider: " + api.getProviderInfo());
                Log.d(getClass().getSimpleName(), "Icon: " + api.getIconPath());
                //Show data collected by te api on the fragment
                TextView textViewTemperature = (TextView) WeatherActivity.this.findViewById(R.id.textview_temperature);
                textViewTemperature.setText((int) api.getTemperature() + "ºC");
                TextView textViewDescription = (TextView) WeatherActivity.this.findViewById(R.id.textview_description);
                textViewDescription.setText(api.getDescription());
                TextView textViewProvider = (TextView) WeatherActivity.this.findViewById(R.id.textview_weatherprovider);
                textViewProvider.setText(api.getProviderInfo());
            }catch(Exception ex){
                Log.e(getClass().getSimpleName(), ex.toString());
                try {
                    Log.e(getClass().getSimpleName(), api.getError());
                }catch (Exception innerEx){
                    Log.e(getClass().getSimpleName(), innerEx.toString());
                }
            }
        }
    }
}
