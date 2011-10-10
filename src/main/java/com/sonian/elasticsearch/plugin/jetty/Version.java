package com.sonian.elasticsearch.plugin.jetty;

import org.elasticsearch.monitor.jvm.JvmInfo;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;

/**
 * @author imotov
 */
public class Version {

    private static final String number;
    private static final String date;
    private static final boolean snapshotBuild;


    static {
        Properties props = new Properties();
        try {
            InputStream stream = Version.class.getClassLoader().getResourceAsStream("com/sonian/elasticsearch/plugin/jetty/version.properties");
            props.load(stream);
            stream.close();
        } catch (Exception e) {
            // ignore
        }

        number = props.getProperty("number", "0.0.0");
        snapshotBuild = number.contains("-SNAPSHOT");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        date = props.getProperty("date", sdf.format(new Date()));
    }

    public static String number() {
        return number;
    }

    public static String date() {
        return date;
    }

    public static boolean snapshotBuild() {
        return snapshotBuild;
    }

    public static String full() {
        StringBuilder sb = new StringBuilder("elasticsearch-jetty/");
        sb.append(number);
        if (snapshotBuild) {
            sb.append("/").append(date);
        }
        return sb.toString();
    }

    public static String description() {
        return "ElasticSearch Jetty Plugin Version: " + number + " (" + date() + ")";
    }

    public static void main(String[] args) {
        System.out.println("ElasticSearch Jetty Plugin Version: " + number + " (" + date() + "), JVM: " + JvmInfo.jvmInfo().vmVersion());
    }
}
