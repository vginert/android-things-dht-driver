package com.vginert.android.things.dhtdriver;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;

/**
 * Driver for the DHTXX humidity sensor.
 * @author Vicente Giner Tendero
 */

public class Dhtxx implements AutoCloseable  {

    private static final String TAG = Dhtxx.class.getSimpleName();

    public static final float MAX_FREQ_HZ = 1;
    public static final float MIN_FREQ_HZ = 1;
    public static final float MAX_HUMIDITY = 1;
    public static final float MAX_POWER_CONSUMPTION_HUMIDITY_UA = 1;
    public static final float MAX_TEMP_C = 1;
    public static final float MAX_POWER_CONSUMPTION_TEMP_UA = 1;

    private final Gpio mGpio;

    /**
     * Create a new DHTXX driver connected to the given GPIO pin.
     * @param gpioPinName the GPIO pin name where the device is connected
     */
    public Dhtxx(String gpioPinName) throws IOException {
        PeripheralManagerService manager = new PeripheralManagerService();
        mGpio = manager.openGpio(gpioPinName);
    }

    /**
     * Create a new DHTXX driver connected to the given GPIO port.
     * @param gpio the GPIO port where the device is connected
     */
    public Dhtxx(Gpio gpio){
        mGpio = gpio;
    }

    /**
     * Close the driver and the GPIO port.
     */
    @Override
    public void close() throws IOException {
        // TODO implement
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Read the current temperature.
     *
     * @return the current temperature in degrees Celsius
     */
    public float readTemperature() throws IOException, IllegalStateException {
        // TODO implement
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Read the current humidity. If you also intend to use temperature readings, prefer
     * {@link #readTemperatureAndHumidity()} instead since sampling the current humidity already
     * requires sampling the current temperature.
     *
     * @return the humidity percentage
     * @throws IOException
     */
    public float readHumidity() throws IOException, IllegalStateException {
        // TODO implement
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Read the current temperature and humidity.
     *
     * @return a 2-element array. The first element is temperature in degrees Celsius, and the
     * second is humidity percentage.
     * @throws IOException
     */
    public float[] readTemperatureAndHumidity() throws IOException, IllegalStateException {
        // TODO implement
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Reads 40 bits from the given address.
     * @throws IOException
     */
    private int readSample() throws IOException, IllegalStateException {
        // TODO implement
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
