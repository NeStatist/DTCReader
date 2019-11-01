package com.example.dtcreader.net;


import java.util.HashMap;
import java.util.Map;

public class ObdReading {
    private  String vin;
    private Map<String,String> readings;

    public  ObdReading(){readings = new HashMap<>(); }
    public ObdReading(String vin,Map<String, String> readings){
        this.vin = vin;
        this.readings = readings;
    }
    public String getVin(){return vin;}
    public void setVin(String vehicle){this.vin = vehicle;}
    public Map<String,String> getReadings(){return readings;}
    public void setReadings (Map<String,String> readings){this.readings = readings;}
    public String toString(){
        return "vin:"+vin+";"+
                "readings:"+readings.toString().substring(10).replace("}","").replace(",",";");
    }

}
