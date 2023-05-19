package org.modbus.device;

import com.intelligt.modbus.jlibmodbus.serial.SerialPort;

public class App {
    static public void main(String[] argv) {
        ModbusMasterDevice device = new ModbusMasterDevice(
            SerialPort.BaudRate.BAUD_RATE_9600,
            8,
            SerialPort.Parity.NONE,
            1
        );
        device.connect();
    }
}

