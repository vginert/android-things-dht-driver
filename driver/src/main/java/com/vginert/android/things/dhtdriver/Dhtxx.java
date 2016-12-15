package com.vginert.android.things.dhtdriver;

import android.util.Log;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;

/**
 * Driver for the DHTXX humidity sensor.
 * @author Vicente Giner Tendero
 */

public class Dhtxx implements AutoCloseable  {

    private static final String TAG = Dhtxx.class.getSimpleName();

    static final float MAX_FREQ_HZ = 1;
    static final float MIN_FREQ_HZ = 1;
    static final float MAX_HUMIDITY = 1;
    static final float MAX_POWER_CONSUMPTION_HUMIDITY_UA = 1;
    static final float MAX_TEMP_C = 1;
    static final float MAX_POWER_CONSUMPTION_TEMP_UA = 1;
    public static final int DHT11_TYPE = 11;
    public static final int DHT21_TYPE = 21;
    public static final int DHT22_TYPE = 22;
    private final int MIN_INTERVAL = 2000;

    private final Gpio mGpio;
    private final int mType;
    private byte[] mData = new byte[5];
    private long mLastReadTime = -MIN_INTERVAL, mMaxCycles;
    private boolean mLastResult = false;

    /**
     * Create a new DHTXX driver connected to the given GPIO pin.
     * @param gpioPinName the GPIO pin name where the device is connected
     */
    public Dhtxx(String gpioPinName, int type) throws IOException {
        PeripheralManagerService manager = new PeripheralManagerService();
        mGpio = manager.openGpio(gpioPinName);
        mGpio.setDirection(Gpio.DIRECTION_IN);
        mType = type;
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
        float f = Float.NaN;

        try {
            if (readSample(true)) {
                switch (mType) {
                    case DHT11_TYPE:
                        f = mData[2];
                        break;
                    case DHT22_TYPE:
                    case DHT21_TYPE:
                        f = mData[2] & 0x7F;
                        f *= 256;
                        f += mData[3];
                        f *= 0.1;
                        if ((mData[2] & 0x80) > 0) {
                            f *= -1;
                        }
                        break;
                }
            }
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
        return f;
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
        float f = Float.NaN;
        try {
            if (readSample(false)) {
                switch (mType) {
                    case DHT11_TYPE:
                        f = mData[0];
                        break;
                    case DHT22_TYPE:
                    case DHT21_TYPE:
                        f = mData[0];
                        f *= 256;
                        f += mData[1];
                        f *= 0.1;
                        break;
                }
            }
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
        return f;
    }

    /**
     * Read the current temperature and humidity.
     *
     * @return a 2-element array. The first element is temperature in degrees Celsius, and the
     * second is humidity percentage.
     * @throws IOException
     */
    public float[] readTemperatureAndHumidity() throws IOException, IllegalStateException {
        return new float[]{readTemperature(), readHumidity()};
    }

    /**
     * Reads 40 bits from the given address.
     * @throws IOException
     */
    private boolean readSample(boolean force) throws IOException, IllegalStateException, InterruptedException {
        // Check if sensor was read less than two seconds ago and return early
        // to use last reading.
        long currentTime = System.currentTimeMillis();
        if (!force && ((currentTime - mLastReadTime) < MIN_INTERVAL)) {
            return mLastResult; // return last correct measurement
        }

        mLastReadTime = currentTime;

        // Reset 40 bits of received data to zero.
        mData[0] = mData[1] = mData[2] = mData[3] = mData[4] = 0;


        // Send start signal.  See DHT datasheet for full signal diagram:
        //   http://www.adafruit.com/datasheets/Digital%20humidity%20and%20temperature%20sensor%20AM2302.pdf

        // Go into high impedence state to let pull-up raise data line level and
        // start the reading process.

        mGpio.setValue(true);
        wait(250);

        // First set data line low for 20 milliseconds.
        mGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
        mGpio.setValue(false);
        wait(20);

        long[] cycles = new long[80];

        // Turn off interrupts temporarily because the next sections are timing critical
        // and we don't want any interruptions.
        //InterruptLock lock; TODO

        // End the start signal by setting data line high for 40 microseconds.
        mGpio.setValue(true);
        wait(0, 40000);

        // Now start reading the data line to get the value from the DHT sensor.
        mGpio.setDirection(Gpio.DIRECTION_IN);
        wait(0, 10000);  // Delay a bit to let sensor pull data line low.

        // First expect a low signal for ~80 microseconds followed by a high signal
        // for ~80 microseconds again.
        if (expectPulse(false) == 0) {
            Log.d(TAG, "Timeout waiting for start signal low pulse.");
            mLastResult = false;
            return mLastResult;
        }
        if (expectPulse(true) == 0) {
            Log.d(TAG, "Timeout waiting for start signal high pulse.");
            mLastResult = false;
            return mLastResult;
        }


        // Now read the 40 bits sent by the sensor.  Each bit is sent as a 50
        // microsecond low pulse followed by a variable length high pulse.  If the
        // high pulse is ~28 microseconds then it's a 0 and if it's ~70 microseconds
        // then it's a 1.  We measure the cycle count of the initial 50us low pulse
        // and use that to compare to the cycle count of the high pulse to determine
        // if the bit is a 0 (high state cycle count < low state cycle count), or a
        // 1 (high state cycle count > low state cycle count). Note that for speed all
        // the pulses are read into a array and then examined in a later step.
        for (int i = 0; i < cycles.length; i+=2) {
            cycles[i]   = expectPulse(false);
            cycles[i+1] = expectPulse(true);
        }

        // Timing critical code is now complete.

        for (int i=0; i<40; ++i) {
            long lowCycles  = cycles[2*i];
            long highCycles = cycles[2*i+1];
            if ((lowCycles == 0) || (highCycles == 0)) {
                Log.d(TAG, "Timeout waiting for pulse.");
                mLastResult = false;
                return mLastResult;
            }
            mData[i/8] <<= 1;
            // Now compare the low and high cycle times to see if the bit is a 0 or 1.
            if (highCycles > lowCycles) {
                // High cycles are greater than 50us low cycle count, must be a 1.
                mData[i/8] |= 1;
            }
            // Else high cycles are less than (or equal to, a weird case) the 50us low
            // cycle count so this must be a zero.  Nothing needs to be changed in the
            // stored data.
        }

        Log.d(TAG, "Received: ");
        Log.d(TAG, mData[0] + ", " + mData[1] + ", " + mData[2] + ", " + mData[3] + ", " + mData[4] + "=? ");
        Log.d(TAG, String.valueOf(mData[0] + mData[1] + mData[2] + mData[3] & 0xFF));

        // Check we read 40 bits and that the checksum matches.
        if (mData[4] == ((mData[0] + mData[1] + mData[2] + mData[3]) & 0xFF)) {
            mLastResult = true;
            return mLastResult;
        }
        else {
            Log.d(TAG, "Checksum failure!");
            mLastResult = false;
            return mLastResult;
        }
    }

    private long expectPulse(boolean level) throws IOException {
        long count = 0;
        while (mGpio.getValue() == level) {
            if (count++ >= mMaxCycles) {
                return 0; // Exceeded timeout, fail.
            }
        }
        return count;
    }
}
