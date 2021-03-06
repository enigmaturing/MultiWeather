package android.and09.multiweatherapp;

import android.and09.weatherapi.*;
import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.ConditionVariable;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONException;
import java.io.InputStream;

public class WeatherActivity extends AppCompatActivity {

    // AND10D S.54 What follows is an instancevariable of the class android.os.ConditionVariable.
    // We will need that in order to avoid bugs cause by Race Conditions related to the Worker-Thread
    public ConditionVariable fragmentReady;
    // AND10D S.58 What follows is  a Instancevariable, to save an instance of type:
    // OnSharedPreferenceChangeListener
    // We need it as a Strong Reference that will not be deleted by the Garbage Collector (see AND10D S.58)
    // in order to listen to changes on the SharedPreferences
    private SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceListener;
    private boolean prefsChanged = false;

    // AND10D Einsendeaufg. 4: Define an object of the inner class WeatherLocationListener. We
    // defined this as an inner class of the WeatherActivity, and it implements the interface
    // android.location.LocationListener, in order to be able to ask for the position of the user
    private final LocationListener locationListener = new WeatherLocationListener();
    private int minTime = 5000; //Minimum time between two sets of positions (in ms)
    private int minDistance = 5; //Minimum distance between two sets of positions (in m)
    private Location lastLocation = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //2nd variant of implemeting Tab-Navigation: With TabLayout (AND10D S46)
        fragmentReady = new ConditionVariable();
        setContentView(R.layout.activity_weather_tablayout);
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        tabLayout.addTab(tabLayout.newTab().setText("Wetter"));
        tabLayout.addTab(tabLayout.newTab().setText("Einstellungen"));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        //AND10D S.51:
        WeatherPagerAdapter adapter = new WeatherPagerAdapter(getSupportFragmentManager());
        final ViewPager viewPager = (ViewPager) findViewById(R.id.view_pager);
        viewPager.setAdapter(adapter);
        //EventHandler to identify the selection of a new tab AND10D Abschn.2.3.2
        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
                // We check if before changing the Tab to weather (position 0), the also
                // the preferences where changed. Only in that case, we renew the data displayed
                // the fragment by calling the Working-Thread
                if (tab.getPosition() == 0 && prefsChanged == true){
                    WeatherRequestTask task = new WeatherRequestTask();
                    task.execute();
                    prefsChanged = false;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
        // Solving problem described on AND10D S.52 -> Swipe should also change the status of the button
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));

        // AND10D Einsendaufg. 4: Depending on the value of the checkbox "use-gps", enable or disable LocationUpdates
        getCheckBoxStatusAndEnableOrDisableLocationUpdates();

        // AND10D S.58 Creating an instance of the interface OnSharedPreferenceChangeListener, in
        // order to be able to catch on changes on the SharedPreferences
        sharedPreferenceListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                Log.d(getClass().getSimpleName(), "Einstellungen wurden gändert");
                prefsChanged = true;
                // We check here if it was the Checkbox "AutomatischeStandrotbestimmung", the option
                // that was changed, in order to get geocordinates in that case
                if (key.equals("use_gps")){
                    // AND10D Einsendaufg. 4: Start location updates depending on the selected value of the checkbox "use gps"
                    getCheckBoxStatusAndEnableOrDisableLocationUpdates();
                }
            }
        };
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceListener);


        /*1st variant of implementing Tab-Navigation: With action bar
        setContentView(R.layout.activity_weather_actionbar_tabs);
        //AND10D S42: Implementing Tab-Navigation with actionbar (1st variant)
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        //AND10D S42: Adding a first tab
        actionBar.addTab(actionBar.newTab().setText("Wetter").setTabListener(new ActionBar.TabListener(){

            @Override
            public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
                FragmentManager manager = WeatherActivity.this.getSupportFragmentManager();
                Fragment fragment = manager.findFragmentByTag(WeatherFragment.class.getName());
                if (fragment == null){
                    //add fragment to the FragmentTransaction, if the fragment was not still created
                    ft.add(R.id.content_frame, new WeatherFragment(), WeatherFragment.class.getName());
                }else{
                    //just show the fragment in the FragmentTransaction, if the fragment was already created
                    ft.show(fragment);
                }
            }

            @Override
            public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
                FragmentManager manager = WeatherActivity.this.getSupportFragmentManager();
                Fragment fragment = manager.findFragmentByTag(WeatherFragment.class.getName());
                ft.hide(fragment);
            }

            @Override
            public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {

            }
        }));
        //AND10D S42: Adding a second tab
        actionBar.addTab(actionBar.newTab().setText("Einstellungen").setTabListener(new ActionBar.TabListener(){

            @Override
            public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
                FragmentManager manager = WeatherActivity.this.getSupportFragmentManager();
                Fragment fragment = manager.findFragmentByTag(WeatherPreferenceFragment.class.getName());
                if (fragment == null){
                    //add fragment to the FragmentTransaction, if the fragment was not still created
                    ft.add(R.id.content_frame, new WeatherPreferenceFragment(), WeatherPreferenceFragment.class.getName());
                }else{
                    //just show the fragment in the FragmentTransaction, if the fragment was already created
                    ft.show(fragment);
                }
            }

            @Override
            public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
                FragmentManager manager = WeatherActivity.this.getSupportFragmentManager();
                Fragment fragment = manager.findFragmentByTag(WeatherPreferenceFragment.class.getName());
                ft.hide(fragment);
            }

            @Override
            public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {

            }
        }));

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

    */

    }

    // AND10D S.67 Auf.3.1. Retrieve weather data from the server on the Background-Thread. We call
    // the Background-Thread from the onResume() Lebenszyklusmethode, so that we refresh the weather data
    // each time the user brings this activity to the foreground. This way he sees always actual weather
    // data
    @Override
    public void onResume(){
        super.onResume();
        // Retrieve data form server in the Background-Thread
        // Create an instance of the inner class WeatherRequestTask, that
        // extends the abstract class AsyncTask nd then we call its method execute(), to fire the Background-Thread
        WeatherRequestTask task = new WeatherRequestTask();
        task.execute();
    }

    // AND10D. Einsendaufg.2
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu. This adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_weather, menu);
        return true;
    }
    // AND10D. Einsendaufg.2
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here
        int id = item.getItemId();
        if (id == R.id.menu_refresh_weather_data) {
            // Retrieve data form server in the Background-Thread. Create an instance of the
            // inner class WeatherRequestTask, that extends the abstract class AsyncTask and then
            // we call its method execute(), to fire the Background-Thread
            WeatherRequestTask task = new WeatherRequestTask();
            task.execute();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class WeatherPagerAdapter extends FragmentStatePagerAdapter {

        static  final int NUM_TABS = 2;

        public WeatherPagerAdapter(FragmentManager fm){
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position){
                case 0:
                    return new WeatherFragment();
                case 1:
                    return new WeatherPreferenceFragment();
                default:
                    return null;
            }

        }

        @Override
        public int getCount() {
            return NUM_TABS;
        }
    }

     public class WeatherRequestTask extends AsyncTask<Void, Void, IWeatherAPI> {

            @Override
            protected IWeatherAPI doInBackground(Void... voids) {
                // We collect the location back from the SharedPrefrences
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this);
                String locationName = prefs.getString("location_name", "");
                String weatherProviderClass = prefs.getString("weather_provider_class", "OpenWeatherMapAPI");
                // AND10D Einsendaufg. 3: We collect the ip address for the private server
                String privateIpAddress = prefs.getString("ip_address_private_server", "http://192.168.2.108:8080");
                // We create an instance of our api here, not in the main thread
                IWeatherAPI api = null;
                try {
                    // Solution exposed in  AND10D S.13, using a fix provider:
                    // api = OpenWeatherMapAPI.fromLocationName(locationName);
                    // Solution exposed in AND10D S.36, in order to get the corresponding api depending on the selected provider:
                    // AND10D Einsendaufg. 4: Check if there is a last location available and in that case, call the fromLongLat method
                    // instead of the fromLocationName method. Every time we disable the use_gps checkbox in the settings tab, we
                    // make lastLocation = null. Therefore, the method fromLocationName will be called always in case the
                    // user doesn't want to use his actual position.
                    if (lastLocation != null) {
                        // AND10D Einsendaufg. 3: Provide the IP Address of our private server too
                        api = WeatherAPIFactory.fromLatLon(weatherProviderClass, lastLocation.getLatitude(), lastLocation.getLongitude(), privateIpAddress);
                    }else{
                        // AND10D Einsendaufg. 3: Provide the IP Address of our private server too
                        api = WeatherAPIFactory.fromLocationName(weatherProviderClass, locationName, privateIpAddress);
                    }
                } catch (Exception ex) {
                    Log.e(getClass().getSimpleName(), ex.toString());
                }
                //AND10D S.54 -> We block the Worker-Thread till the Wetter-Fragment has been initialized
                fragmentReady.block();
                return api;
            }

            @Override
            protected void onPostExecute(IWeatherAPI api) {
                // It is possible that the method doInBackground returns no api object, caused for example
                // by a missing internet connection. We have to contemplate that case:
                if (api == null) {
                    Log.e(getClass().getSimpleName(), "Rückgabe von doInBackground ist null: gibt es keine Interneverbindung?");
                    Toast.makeText(WeatherActivity.this, R.string.error_no_answer, Toast.LENGTH_LONG).show();
                    return;
                }
                // In case an api object was sucessfully created by doInBackground:
                try {
                    // Show data collected by the api on logcat
                    Log.d(getClass().getSimpleName(), "Temperatur: " + api.getTemperature());
                    Log.d(getClass().getSimpleName(), "Beschreibung: " + api.getDescription());
                    Log.d(getClass().getSimpleName(), "Provider: " + api.getProviderInfo());
                    Log.d(getClass().getSimpleName(), "Icon: " + api.getIconPath());
                    // Show data collected by te api on the fragment
                    TextView textViewTemperature = (TextView) WeatherActivity.this.findViewById(R.id.textview_temperature);
                    textViewTemperature.setText((int) api.getTemperature() + "ºC");
                    TextView textViewDescription = (TextView) WeatherActivity.this.findViewById(R.id.textview_description);
                    textViewDescription.setText(api.getDescription());
                    TextView textViewProvider = (TextView) WeatherActivity.this.findViewById(R.id.textview_weatherprovider);
                    textViewProvider.setText(api.getProviderInfo());
                    // AND10D S.60 Update the weather image
                    InputStream bitmapStream = getAssets().open(api.getIconPath());
                    Bitmap bitmap = BitmapFactory.decodeStream(bitmapStream);
                    ImageView imageView = (ImageView) WeatherActivity.this.findViewById(R.id.imageview_weathericon);
                    imageView.setImageBitmap(bitmap);
                // AND10D S.66 In case there was a exception on populating the data from server:
                // Catch for Exceptions of type JSONException:
                } catch (JSONException ex) {
                    Log.e(getClass().getSimpleName(), ex.toString());
                    try {
                        Log.e(getClass().getSimpleName(), api.getError());
                        Toast.makeText(WeatherActivity.this, api.getError(), Toast.LENGTH_LONG).show();
                    } catch (Exception innerEx) {
                        Log.e(getClass().getSimpleName(), innerEx.toString());
                        Toast.makeText(WeatherActivity.this, R.string.error_unknown, Toast.LENGTH_LONG).show();
                    }
                // Catch for general Exceptions, not of type JSONException:
                } catch (Exception ex){
                    Log.e(getClass().getSimpleName(), ex.toString());
                }
            }
        }

    // AND10D Einsendeaufg. 4: Implement in an inner class the interface LocationListener, in order
    // to create an instance of this class to ask for the position of the user.
    class WeatherLocationListener implements LocationListener {
        // Do not forget to activate for this app the access to GPS Position on the mobile phone,
        // after installing it in emulator or the real device. In order to do that, install the app
        // and then go to Settings (Ajustes) -> Permissions (Permisos) -> Permissions for apps
        // (Permisos de aplicaciones) -> Y alli activar ubicacion para esta applicacion
        @Override
        public void onLocationChanged(Location location) {
            Log.d(WeatherActivity.this.getClass().getSimpleName(), "Empfangene Geodaten:\n" + location.toString());
            getLastKnownLocation();
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    }

    @SuppressLint("MissingPermission")
    private void getLastKnownLocation(){
        LocationManager locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
        try {
            lastLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }catch(SecurityException ex){
            Log.d(getClass().getSimpleName(), "Permission fehlt. " + ex.toString());
            Toast.makeText(WeatherActivity.this, R.string.error_no_permissions, Toast.LENGTH_LONG).show();
            lastLocation = null;
        }
    }

    @SuppressLint("MissingPermission")
    private void getCheckBoxStatusAndEnableOrDisableLocationUpdates(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this);
        boolean checkBoxUseGps = prefs.getBoolean("use_gps", false);
        // Activate or deactivate the Location-Updates accroding to the value of the checkbox
        LocationManager locationManager = (LocationManager) WeatherActivity.this.getSystemService(LOCATION_SERVICE);
        if (checkBoxUseGps){
            // We call the method requestLocationUpdate of our instance locationManager in a try-catch
            // in order to prevent a crash of the app in case the user has deactivated the positionin permission
            try {
                // Because we are using NETWORK_PROVIDER as the location provider, we need to declare
                // two uses-permissions on the AndroidManifest.xml
                // <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
                // <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
                // We also have to activate the position permision on our smartphone for this app,
                // in order not to rise a SecurityException
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, minTime,minDistance, locationListener);
            } catch (SecurityException ex){
                Log.d(getClass().getSimpleName(), "Permission fehlt. " + ex.toString());
                Toast.makeText(WeatherActivity.this, R.string.error_no_permissions, Toast.LENGTH_LONG).show();
            }
        }else{
            locationManager.removeUpdates(locationListener);
            lastLocation = null;
        }
    }
}



