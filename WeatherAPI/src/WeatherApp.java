import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class WeatherApp {
    // Get data using API

    public static JSONObject getWeatherData(String locationName){
        // Gets location coordinates using the geolocation API
        JSONArray locationData = getLocationData(locationName);

        JSONObject location = (JSONObject) locationData.get(0);
        double latitude = (double) location.get("latitude");
        double longitude = (double) location.get("longitude");

        String urlString = "https://api.open-meteo.com/v1/forecast?" 
            + "latitude=" + latitude + "&longitude=" + longitude +"&hourly=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m&timezone=America%2FNew_York";
        
        try {
            HttpURLConnection conn = fetchApiResponse(urlString);

            if (conn.getResponseCode() != 200){
                System.out.println("Error: Could not connect to API");
                return null;
           }

           StringBuilder resultJson = new StringBuilder();
           Scanner scanner = new Scanner(conn.getInputStream());
           while(scanner.hasNext()){
                resultJson.append(scanner.nextLine());
           }

           scanner.close();
           conn.disconnect();

           JSONParser parser = new JSONParser();
           JSONObject resultJsonObj = (JSONObject) parser.parse(String.valueOf(resultJson));

           // retrieve hourly data
           JSONObject hourly = (JSONObject) resultJsonObj.get("hourly");
           JSONArray time = (JSONArray) hourly.get("time");
           int index = findIndexOfCurrentTime(time);

           // get temperature
           JSONArray temperatureData = (JSONArray) hourly.get("temperature_2m");
           double temperature = (double) temperatureData.get(index);

           // get weather code
           JSONArray weathercode = (JSONArray) hourly.get("weather_code");
           String weatherCondition = convertWeatherCode((long) weathercode.get(index));

           // get humidity
           JSONArray relativeHumidity = (JSONArray) hourly.get("relative_humidity_2m");
           long humidity = (long) relativeHumidity.get(index);

           // get windspeed
           JSONArray windspeedData = (JSONArray) hourly.get("wind_speed_10m");
           double windspeed = (double) windspeedData.get(index);

           // build the weather json data that will be accessed on the frontend
           JSONObject weatherData = new JSONObject();
           weatherData.put("temperature", temperature);
           weatherData.put("weather_condition", weatherCondition);
           weatherData.put("humidity", humidity);
           weatherData.put("windspeed", windspeed);

            return weatherData;
        } catch (Exception e){
            e.printStackTrace();
        }
    
        return null;
    }

    public static JSONArray getLocationData(String locationName){
        // Format data for whitespace compatibility with API
        locationName = locationName.replaceAll(" ", "+");

        // URL description 
        String urlString = "https://geocoding-api.open-meteo.com/v1/search?name="
        + locationName + "&count=10&language=en&format=json";

        try{
            // call api and get response
            HttpURLConnection conn = fetchApiResponse(urlString);

            // check response type
            if (conn.getResponseCode() != 200){
                System.out.println("Error: Could not connect to API");
                return null;
            } else {
                StringBuilder resultJson = new StringBuilder();
                Scanner scanner = new Scanner(conn.getInputStream());

                while(scanner.hasNext()){
                    resultJson.append(scanner.nextLine());
                }

                scanner.close();

                conn.disconnect();

                // To parse the JSON string into a JSON obj
                JSONParser parser = new JSONParser();
                JSONObject resultsJsonObj = (JSONObject) parser.parse(String.valueOf(resultJson));

                // gets the list of location data the API generated
                JSONArray locationData = (JSONArray) resultsJsonObj.get("results");
                return locationData;
            }

        } catch(Exception e){
            e.printStackTrace();
        }

        return null;
    }

    private static HttpURLConnection fetchApiResponse(String urlString){
        try {
            // attempting to create connection
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            // set request method to get
            conn.setRequestMethod("GET");

            // connect to API
            conn.connect();
            return conn;
        } catch (IOException e){
            e.printStackTrace();
        }

        // could not make a connection
        return null;
    }
    
    private static int findIndexOfCurrentTime(JSONArray timeList){
        String currentTime = getCurrentTime();

        for (int i = 0; i < timeList.size(); i++){
            String time = (String) timeList.get(i);
            if (time.equalsIgnoreCase(currentTime)){
                return i;
            }

        }

        return 0;
    }

    private static String getCurrentTime(){
        LocalDateTime currentDateTime = LocalDateTime.now();
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH':00'");
        String formattedDateTime = currentDateTime.format(formatter);
        return formattedDateTime;
    }

    private static String convertWeatherCode(long weathercode){
        String weatherCondition = "";
        if (weathercode == 0L){
            weatherCondition = "Clear";
        } else if (weathercode > 0L && weathercode <= 3L){
            weatherCondition = "Cloudy";
        } else if (weathercode >= 51L && weathercode <= 67L
                || (weathercode >= 80L && weathercode <= 99L)){
            weatherCondition = "Rain";
        } else if (weathercode >= 71L && weathercode <= 77L){
            weatherCondition = "Snow";
        } 

        return weatherCondition;
    }
}
