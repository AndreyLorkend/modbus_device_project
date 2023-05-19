package org.modbus.device;

import com.intelligt.modbus.jlibmodbus.Modbus;
import com.intelligt.modbus.jlibmodbus.data.DataHolder;
import com.intelligt.modbus.jlibmodbus.data.FifoQueue;
import com.intelligt.modbus.jlibmodbus.data.SimpleDataHolderBuilder;
import com.intelligt.modbus.jlibmodbus.data.comm.ModbusCommEventSend;
import com.intelligt.modbus.jlibmodbus.data.mei.ReadDeviceIdentificationInterface;
import com.intelligt.modbus.jlibmodbus.exception.IllegalDataAddressException;
import com.intelligt.modbus.jlibmodbus.exception.IllegalDataValueException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusIOException;
import com.intelligt.modbus.jlibmodbus.master.ModbusMaster;
import com.intelligt.modbus.jlibmodbus.master.ModbusMasterFactory;
import com.intelligt.modbus.jlibmodbus.master.ModbusMasterTCP;
import com.intelligt.modbus.jlibmodbus.msg.base.mei.MEIReadDeviceIdentification;
import com.intelligt.modbus.jlibmodbus.msg.base.mei.ReadDeviceIdentificationCode;
import com.intelligt.modbus.jlibmodbus.serial.SerialParameters;
import com.intelligt.modbus.jlibmodbus.serial.SerialPort;
import com.intelligt.modbus.jlibmodbus.serial.SerialPortFactoryJSSC;
import com.intelligt.modbus.jlibmodbus.serial.SerialUtils;
import com.intelligt.modbus.jlibmodbus.slave.ModbusSlave;
import com.intelligt.modbus.jlibmodbus.slave.ModbusSlaveFactory;
import com.intelligt.modbus.jlibmodbus.tcp.TcpParameters;
import jssc.SerialPortList;

import java.net.InetAddress;
import java.nio.charset.Charset;

public class App implements Runnable {
    private ModbusSlave slave;
    private long timeout = 0;

    static private <T> T initParameter(String title, T parameter, String arg, ParameterInitializer<T> pi) {
        try {
            parameter = pi.init(arg);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            System.out.format("Invalid %s value:%s%n", title, arg);
        }
        return parameter;
    }

    public static void main(String[] argv) {
        SerialParameters sp;
        TcpParameters tp;
        App test = new App();
        for(String name : SerialPortList.getPortNames()) {
            System.out.println(name);
        }
        try {
            switch (TransportType.RTU) {
                case RTU:
                    sp = new SerialParameters();
                    String device_name_slave = "/dev/ttyUSB0";
                    SerialPort.BaudRate baud_rate = SerialPort.BaudRate.BAUD_RATE_9600;
                    int data_bits = 8;
                    int stop_bits = 1;
                    SerialPort.Parity parity = SerialPort.Parity.NONE;

                    try {
                        device_name_slave = initParameter("device_name_slave", device_name_slave, "/dev/ttyUSB0", new ParameterInitializer<String>() {
                            @Override
                            public String init(String arg) throws Exception {
                                return arg;
                            }
                        });
                        baud_rate = initParameter("baud_rate", baud_rate, "9600", new ParameterInitializer<SerialPort.BaudRate>() {
                            @Override
                            public SerialPort.BaudRate init(String arg) throws Exception {
                                return SerialPort.BaudRate.getBaudRate(Integer.decode(arg));
                            }
                        });
                        data_bits = initParameter("data_bits", data_bits, "8", new ParameterInitializer<Integer>() {
                            @Override
                            public Integer init(String arg) throws Exception {
                                return Integer.decode(arg);
                            }
                        });
                        stop_bits = initParameter("stop_bits", stop_bits, "1", new ParameterInitializer<Integer>() {
                            @Override
                            public Integer init(String arg) throws Exception {
                                return Integer.decode(arg);
                            }
                        });
                        parity = initParameter("parity", parity, "0", new ParameterInitializer<SerialPort.Parity>() {
                            @Override
                            public SerialPort.Parity init(String arg) throws Exception {
                                return SerialPort.Parity.getParity(Integer.decode(arg));
                            }
                        });
                    } catch (IndexOutOfBoundsException ie) {
                        //it's ok
                    }
                    System.out.format("Starting Modbus RTU with settings:%n\t%s, %s, %d, %d, %s%n",
                            device_name_slave, baud_rate.toString(), data_bits, stop_bits, parity.toString());
                    SerialUtils.setSerialPortFactory(new SerialPortFactoryJSSC());
                    sp.setParity(parity);
                    sp.setStopBits(stop_bits);
                    sp.setDataBits(data_bits);
                    sp.setBaudRate(baud_rate);
                    sp.setDevice(device_name_slave);
                    test.slave = ModbusSlaveFactory.createModbusSlaveRTU(sp);
                    break;
                default:
                    tp = new TcpParameters(InetAddress.getLocalHost(), Modbus.TCP_PORT, true);
                    test.slave = ModbusSlaveFactory.createModbusSlaveTCP(tp);

            }
            test.slave.setServerAddress(1);
            test.slave.setDataHolder(new SimpleDataHolderBuilder(1000));
            Modbus.setLogLevel(Modbus.LogLevel.LEVEL_RELEASE);

            try {
                DataHolder dataHolder = test.slave.getDataHolder();
                dataHolder.getCoils().set(1, true);
                dataHolder.getCoils().set(3, true);
                dataHolder.getDiscreteInputs().setRange(0, new boolean[]{false, true, true, false, true});
                dataHolder.getInputRegisters().setRange(0, new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10});
                dataHolder.getInputRegisters().set(11, 69);
                dataHolder.getHoldingRegisters().setRange(0, new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10});
                dataHolder.getSlaveId().set("slave implementation = jlibmodbus".getBytes(Charset.forName("UTF-8")));
                dataHolder.getExceptionStatus().set(123);
                dataHolder.getCommStatus().addEvent(ModbusCommEventSend.createExceptionSentRead());
                ReadDeviceIdentificationInterface rii = dataHolder.getReadDeviceIdentificationInterface();
                rii.setVendorName("Vendor name=\"JSC Invertor\"");
                rii.setProductCode("Product code=\"3245234658\"");
                rii.setMajorMinorRevision("Revision=\"v1.0\"");
                FifoQueue fifo = dataHolder.getFifoQueue(0);
                for (int i = 0; i < 35; i++) {
                    if (fifo.size() == Modbus.MAX_FIFO_COUNT)
                        fifo.poll();
                    fifo.add(i * 11);
                }
            }
            catch (IllegalDataAddressException e) {
                e.printStackTrace();
            } catch (IllegalDataValueException e) {
                e.printStackTrace();
            }

            test.start(10000);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static void printRegisters(String title, int[] ir) {
        for (int i : ir)
            System.out.format("%6d", i);
        System.out.format("\t%s%n", title);
    }

    private static void printBits(String title, boolean[] ir) {
        for (boolean i : ir)
            System.out.format("%6s", i);
        System.out.format("\t%s%n", title);
    }

    private void start(long timeout) {
        this.timeout = timeout;
        Thread thread = new Thread(this);
        thread.start();
        try {
            thread.join(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        long time = System.currentTimeMillis();
        try {
            slave.listen();
            Modbus.setAutoIncrementTransactionId(true);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
        }
        while ((System.currentTimeMillis() - time) < timeout) {
            try {
                Thread.sleep(1000);
                System.out.println("Slave output");
                printRegisters("Holding registers", slave.getDataHolder().getHoldingRegisters().getRange(0, 16));
                printRegisters("Input registers", slave.getDataHolder().getInputRegisters().getRange(0, 16));
                printBits("Coils", slave.getDataHolder().getCoils().getRange(0, 16));
                printBits("Discrete inputs", slave.getDataHolder().getDiscreteInputs().getRange(0, 16));
                System.out.println();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {
            slave.shutdown();
        } catch (ModbusIOException e) {
            e.printStackTrace();
        }
    }

    private enum TransportType {
        TCP("tcp"),
        RTU("rtu"),
        ASCII("ascii");

        final private String type;

        TransportType(String type) {
            this.type = type;
        }

        static public TransportType get(String s) {
            for (TransportType type : TransportType.values()) {
                if (type.toString().equalsIgnoreCase(s))
                    return type;
            }
            return TCP;
        }

        @Override
        final public String toString() {
            return type;
        }
    }

    interface ParameterInitializer<T> {
        T init(String arg) throws Exception;
    }
}