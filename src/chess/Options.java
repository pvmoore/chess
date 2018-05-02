package chess;

import juice.types.Int2;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

final public class Options {
    private String filename = "chess.props";
    private Properties props;


    public Options() {
        load();
    }
    public void save() {
        try {
            props.store(new FileOutputStream(filename), null);
        }catch(IOException e) {
            // ignore
            e.printStackTrace();
        }
    }
    public String getString(String key) {
        return (String)props.get(key);
    }
    public boolean getBool(String key, boolean defaultValue) {
        var b = getString(key);
        if(b==null) return defaultValue;
        return Boolean.valueOf(b);
    }
    public int getInt(String key, int defaultValue) {
        String i = getString(key);
        if(i==null) return defaultValue;
        return Integer.valueOf(i);
    }
    public Int2 getInt2(String key) {
        String x = getString(key+"x");
        String y = getString(key+"y");
        if(x==null || y==null) return null;
        return new Int2(Integer.valueOf(x), Integer.valueOf(y));
    }
    public void set(String key, String value) {
        props.put(key, value);
    }
    public void set(String key, boolean value) {
        props.put(key, ""+value);
    }
    public void set(String key, int value) {
        props.put(key, ""+value);
    }
    public void set(String key, Int2 i) {
        props.put(key+"x", ""+i.getX());
        props.put(key+"y", ""+i.getY());
    }
    //===============================================================================
    private void load() {
        props = new Properties();
        try {
            props.load(new FileInputStream(filename));
        }catch(IOException e) {
            // ignore
        }
    }
}
