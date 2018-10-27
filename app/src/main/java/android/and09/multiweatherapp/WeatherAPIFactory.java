package android.and09.multiweatherapp;

import android.and09.weatherapi.*;

// AND10D S.34
// This class serves as Fatory-Class to create objects of type IWeatherAPI, depending on which
// provider was slected on the options menu. This is a more elegant solution to the one presented
// on AND10D S.33, which consisted on a switch.
public class WeatherAPIFactory {
    // we make the constructor of this class private, like expected on a Factory-Class
    private WeatherAPIFactory() {}

    // we provide a method to generate instances of thype IWeatherAPI, based on LocationNames
    // we make the method static, like expected on a Factory-Class, in order to be able to call
    // it directly by using the name of the class (WeatherAPIFactory) followed to the name of the
    // method (fromLocationName())
    // AND10D Einsendeaufg. 3: Pass the ip of our private server ae parameter too
    public static IWeatherAPI fromLocationName(String className, String locationName, String privateIpAddress) throws Exception {
        // Create a class object depending on the desired weather provider
        Class c = Class.forName("android.and09.weatherapi." + className);
        IWeatherAPI api = (IWeatherAPI) c.getMethod("fromLocationName", String.class, String.class).invoke(null, locationName, privateIpAddress);
        return api;
    }

    // We do the same like before but this time based on coordinates
    // AND10D Einsendeaufg. 3: Pass the ip of our private server ae parameter too
    public static IWeatherAPI fromLatLon(String className, double lat, double lon,  String privateIpAddress) throws Exception {
        // Create a class object depending on the desired weather provider
        Class c = Class.forName("android.and09.weatherapi." + className);
        IWeatherAPI api = (IWeatherAPI) c.getMethod("fromLatLon", double.class, double.class, String.class).invoke(null, lat, lon, privateIpAddress);
        return api;
    }
}


