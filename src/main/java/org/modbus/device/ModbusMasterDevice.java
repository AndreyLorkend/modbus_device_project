package org.modbus.device;

import com.intelligt.modbus.jlibmodbus.exception.ModbusIOException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusNumberException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusProtocolException;
import com.intelligt.modbus.jlibmodbus.master.ModbusMaster;
import com.intelligt.modbus.jlibmodbus.master.ModbusMasterFactory;
import com.intelligt.modbus.jlibmodbus.serial.*;
import jssc.SerialPortList;

public class ModbusMasterDevice implements BaseDevice{
    private SerialParameters serialParameters_ = new SerialParameters();
    private SerialPort.BaudRate baudRate_;
    private int dataBits_;
    private SerialPort.Parity parity_;
    private int stopBits_;
    private ModbusMaster modbusMasterRTU;

    ModbusMasterDevice(SerialPort.BaudRate baudRate, int dataBits, SerialPort.Parity parity, int stopBits) {
        baudRate_ = baudRate;
        dataBits_ = dataBits;
        parity_ = parity;
        stopBits_ = stopBits;
        initConnectionSettings();
    }

    @Override
    public void connect() {
        try {
            modbusMasterRTU.connect();
            int[] timeSeries = modbusMasterRTU.readInputRegisters(1, 1, 2);
            if(timeSeries.length > 1) {
                System.out.println("Temperature: " + timeSeries[0]);
                System.out.println("Humidity: "+ timeSeries[1]);
            }
            modbusMasterRTU.disconnect();
        } catch (ModbusIOException e) {
            throw new RuntimeException(e);
        } catch (ModbusProtocolException e) {
            throw new RuntimeException(e);
        } catch (ModbusNumberException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void initConnectionSettings() {
        try{
            String[] devicesList = SerialPortList.getPortNames();
            serialParameters_.setBaudRate(baudRate_);
            if(devicesList.length > 0) {
                serialParameters_.setDevice(devicesList[0]);
                serialParameters_.setBaudRate(baudRate_);
                serialParameters_.setDataBits(dataBits_);
                serialParameters_.setParity(parity_);
                serialParameters_.setStopBits(stopBits_);
                SerialUtils.setSerialPortFactory(new SerialPortFactoryJSSC());
                modbusMasterRTU = ModbusMasterFactory.createModbusMasterRTU(serialParameters_);
            }
        } catch (SerialPortException e) {
            throw new RuntimeException(e);
        }

    }
}
