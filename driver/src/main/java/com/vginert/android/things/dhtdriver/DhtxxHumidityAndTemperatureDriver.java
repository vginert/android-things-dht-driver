package com.vginert.android.things.dhtdriver;

import android.hardware.Sensor;

import com.google.android.things.userdriver.UserDriverManager;
import com.google.android.things.userdriver.UserSensor;
import com.google.android.things.userdriver.UserSensorDriver;
import com.google.android.things.userdriver.UserSensorReading;

import java.io.IOException;
import java.util.UUID;

/**
 * @author Vicente Giner Tendero
 */

public class DhtxxHumidityAndTemperatureDriver implements AutoCloseable  {

    private static final String TAG = DhtxxHumidityAndTemperatureDriver.class.getSimpleName();

    // DRIVER parameters
    // documented at https://source.android.com/devices/sensors/hal-interface.html#sensor_t
    private static final String DRIVER_NAME = "Dhtxx";
    private static final String DRIVER_VENDOR = "Aosong";
    private static final int DRIVER_MIN_DELAY_US = Math.round(1000000.f / Dhtxx.MAX_FREQ_HZ);
    private static final int DRIVER_MAX_DELAY_US = Math.round(1000000.f / Dhtxx.MIN_FREQ_HZ);

    private Dhtxx mDevice;
    private TemperatureUserDriver mTemperatureUserDriver;
    private HumidityUserDriver mHumidityUserDriver;

    /**
     * Create a new framework humidity driver connected to the given GPIO pin.
     * The driver emits {@link android.hardware.Sensor} with acceleration data when registered.
     * @param gpioPinName the GPIO pin name where the device is connected
     * @throws IOException
     * @see #registerHumiditySensor()
     * @see #registerTemperatureSensor()
     */
    public DhtxxHumidityAndTemperatureDriver(String gpioPinName) throws IOException {
        mDevice = new Dhtxx(gpioPinName);
    }

    /**
     * Close the driver and the underlying device.
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        unregisterTemperatureSensor();
        unregisterHumiditySensor();
        if (mDevice != null) {
            try {
                mDevice.close();
            } finally {
                mDevice = null;
            }
        }
    }

    /**
     * Register a {@link UserSensor} that pipes temperature readings into the Android SensorManager.
     * @see #unregisterTemperatureSensor()
     */
    public void registerTemperatureSensor() {
        if (mDevice == null) {
            throw new IllegalStateException("cannot register closed driver");
        }

        if (mTemperatureUserDriver == null) {
            mTemperatureUserDriver = new TemperatureUserDriver();
            UserDriverManager.getManager().registerSensor(mTemperatureUserDriver.getUserSensor());
        }
    }

    /**
     * Register a {@link UserSensor} that pipes pressure readings into the Android SensorManager.
     * @see #unregisterHumiditySensor()
     */
    public void registerHumiditySensor() {
        if (mDevice == null) {
            throw new IllegalStateException("cannot register closed driver");
        }

        if (mHumidityUserDriver == null) {
            mHumidityUserDriver = new HumidityUserDriver();
            UserDriverManager.getManager().registerSensor(mHumidityUserDriver.getUserSensor());
        }
    }

    /**
     * Unregister the temperature {@link UserSensor}.
     */
    public void unregisterTemperatureSensor() {
        if (mTemperatureUserDriver != null) {
            UserDriverManager.getManager().unregisterSensor(mTemperatureUserDriver.getUserSensor());
            mTemperatureUserDriver = null;
        }
    }

    /**
     * Unregister the pressure {@link UserSensor}.
     */
    public void unregisterHumiditySensor() {
        if (mHumidityUserDriver != null) {
            UserDriverManager.getManager().unregisterSensor(mHumidityUserDriver.getUserSensor());
            mHumidityUserDriver = null;
        }
    }

    private class HumidityUserDriver extends UserSensorDriver {
        // DRIVER parameters
        // documented at https://source.android.com/devices/sensors/hal-interface.html#sensor_t
        private static final float DRIVER_MAX_RANGE = Dhtxx.MAX_HUMIDITY;
        private static final float DRIVER_RESOLUTION = .0262f;
        private static final float DRIVER_POWER = Dhtxx.MAX_POWER_CONSUMPTION_HUMIDITY_UA / 1000.f;
        private static final int DRIVER_VERSION = 1;
        private static final String DRIVER_REQUIRED_PERMISSION = "";

        private boolean mEnabled;
        private UserSensor mUserSensor;

        private UserSensor getUserSensor() {
            if (mUserSensor == null) {
                mUserSensor = UserSensor.builder()
                        .setType(Sensor.TYPE_PRESSURE)
                        .setName(DRIVER_NAME)
                        .setVendor(DRIVER_VENDOR)
                        .setVersion(DRIVER_VERSION)
                        .setMaxRange(DRIVER_MAX_RANGE)
                        .setResolution(DRIVER_RESOLUTION)
                        .setPower(DRIVER_POWER)
                        .setMinDelay(DRIVER_MIN_DELAY_US)
                        .setRequiredPermission(DRIVER_REQUIRED_PERMISSION)
                        .setMaxDelay(DRIVER_MAX_DELAY_US)
                        .setUuid(UUID.randomUUID())
                        .setDriver(this)
                        .build();
            }
            return mUserSensor;
        }

        @Override
        public UserSensorReading read() throws IOException {
            return new UserSensorReading(new float[]{mDevice.readHumidity()});
        }
    }

    private class TemperatureUserDriver extends UserSensorDriver {
        // DRIVER parameters
        // documented at https://source.android.com/devices/sensors/hal-interface.html#sensor_t
        private static final float DRIVER_MAX_RANGE = Dhtxx.MAX_TEMP_C;
        private static final float DRIVER_RESOLUTION = 0.005f;
        private static final float DRIVER_POWER = Dhtxx.MAX_POWER_CONSUMPTION_TEMP_UA / 1000.f;
        private static final int DRIVER_VERSION = 1;
        private static final String DRIVER_REQUIRED_PERMISSION = "";

        private boolean mEnabled;
        private UserSensor mUserSensor;

        private UserSensor getUserSensor() {
            if (mUserSensor == null) {
                mUserSensor = UserSensor.builder()
                        .setType(Sensor.TYPE_AMBIENT_TEMPERATURE)
                        .setName(DRIVER_NAME)
                        .setVendor(DRIVER_VENDOR)
                        .setVersion(DRIVER_VERSION)
                        .setMaxRange(DRIVER_MAX_RANGE)
                        .setResolution(DRIVER_RESOLUTION)
                        .setPower(DRIVER_POWER)
                        .setMinDelay(DRIVER_MIN_DELAY_US)
                        .setRequiredPermission(DRIVER_REQUIRED_PERMISSION)
                        .setMaxDelay(DRIVER_MAX_DELAY_US)
                        .setUuid(UUID.randomUUID())
                        .setDriver(this)
                        .build();
            }
            return mUserSensor;
        }

        @Override
        public UserSensorReading read() throws IOException {
            return new UserSensorReading(new float[]{mDevice.readTemperature()});
        }
    }
}
