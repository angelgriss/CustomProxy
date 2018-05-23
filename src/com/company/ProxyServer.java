package com.company;

import java.lang.management.ManagementFactory;
import java.net.*;
import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class ProxyServer {
    public static void main(String[] args) throws IOException {

        String dir = System.getProperty("user.dir");
        Logger logger = Logger.getLogger("MyLog");
        FileHandler fh;

        String vmName = ManagementFactory.getRuntimeMXBean().getName();
        int p = vmName.indexOf("@");
        String pid = vmName.substring(0, p);

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();

        fh = new FileHandler(dir +"/CP-"+ dateFormat.format(date)+".log");

        logger.addHandler(fh);
        SimpleFormatter formatter = new SimpleFormatter();
        fh.setFormatter(formatter);

        ServerSocket serverSocket = null;
        boolean listening = true;

        int port = 10000;	//puerto por defecto
        try {
            port = Integer.parseInt(args[0]);
        } catch (Exception e) {
            //ignorar
        }

        logger.info("Inicio CustomProxy en puerto:"+ String.valueOf(port) + " - idProceso:"+ pid);

        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Inicio en puerto: " + port + " - idProceso:"+ pid);
            System.out.println("Directorio aplicacion: " + dir);
        } catch (IOException e) {
            System.err.println("No se puede escuchar en puerto: " + args[0]);
            System.exit(-1);
        }

        while (listening) {

            new ProxyThread(serverSocket.accept(),logger).start();
        }
        serverSocket.close();
    }
}