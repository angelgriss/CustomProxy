package com.company;

import sun.net.www.http.HttpCaptureInputStream;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

public class ProxyThread extends Thread  {

    private Socket socket = null;

    private String metodohttp = "";
    private Logger log=null;


    private static final int BUFFER_SIZE = 32768;
    public ProxyThread(Socket socket,Logger lg) {
        super("ProxyThread");
        this.socket = socket;
        this.log=lg;
    }

    public void run() {
        StringBuilder strb=new StringBuilder();
        strb.append("\n");
        strb.append("#####################################INICIO TRANSACCION#####################################");

        try {
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            Map<String, String> mapHeaders=new HashMap<String, String>();

            String inputLine, outputLine;
            int cnt = 0;
            String urlToCall = "";

            //INICIO Obtener request del cliente
            while ((inputLine = in.readLine()) != null) {

                if(cnt<1)
                {
                    try {
                        StringTokenizer tok = new StringTokenizer(inputLine);
                        System.out.println("InputLine: " + inputLine);

                        tok.nextToken();
                    } catch (Exception e) {
                        break;
                    }

                    if (cnt == 0) {
                        String[] tokens = inputLine.split(" ");

                        metodohttp = tokens[0];
                        urlToCall = tokens[1];

                        System.out.println("Request para : " + urlToCall);
                    }
                }
                else
                {
                    //headers
                    if(inputLine != null && !inputLine.isEmpty())
                    {
                        String header=inputLine.substring(0,inputLine.indexOf(":"));
                        String valor=inputLine.substring(inputLine.indexOf(":")+1,inputLine.length());

                        mapHeaders.put(header,valor);
                    }
                    else
                    {
                        break;
                    }
                }
                cnt++;
            }
            //FIN Obtener request del cliente

            //INICIO Consumir server destino
            strb.append("\n");
            strb.append("-----------------------INICIO REQUEST-----------------------");
            strb.append("\n");
            strb.append("URL Entrada: " + urlToCall);
            strb.append("\n");
            strb.append("Metodo Http:" +metodohttp);

            BufferedReader rd = null;

            URL obj = new URL(urlToCall);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            try {

                InputStream is = null;

                con.setRequestMethod(metodohttp);

                strb.append("\n");
                strb.append("Request Headers:");

                for (Map.Entry<String, String> entry : mapHeaders.entrySet()) {
                    con.setRequestProperty(entry.getKey(),entry.getValue());
                    System.out.println("HeadersRQ: " + entry.getKey()+":"+entry.getValue());
                    strb.append("\n");
                    strb.append(entry.getKey()+":" +entry.getValue());
                }

                strb.append("\n");
                strb.append("-----------------------FIN REQUEST-----------------------");

                strb.append("\n");
                strb.append("-----------------------INICIO RESPONSE-----------------------");

                con.setDoOutput(true);
                con.setDoInput(true);


                int responseCode = con.getResponseCode();
                strb.append("\n");
                strb.append("Response Code:"+ String.valueOf(responseCode));

                Map mp=con.getHeaderFields();

                System.out.println("GET Response Code :: " + responseCode);
                if (responseCode == HttpURLConnection.HTTP_OK) {

                    if (con.getContentLength() > 0) {
                        try {
                            is = con.getInputStream();
                            boolean comprimido=false;

                            strb.append("\n");
                            strb.append("Response Headers:");

                            Map<String,List<String>> mapHR=con.getHeaderFields();
                            Set<Map.Entry<String, List<String>>> entrySet =mapHR.entrySet();

                            for (Map.Entry<String, List<String>> entry : entrySet) {
                                String headerName = entry.getKey();
                                strb.append("\n");
                                strb.append(headerName +": ");

                                List<String> headerValues = entry.getValue();
                                for (String value : headerValues) {
                                    strb.append(value);

                                    if(headerName!=null && headerName.contains("Content-Encoding") && value.contains("gzip"))
                                        comprimido=true;
                                }
                            }

                            if(comprimido)
                                is = new GZIPInputStream(is);

                            strb.append("\n");
                            strb.append("-----------------------FIN RESPONSE-----------------------");
                            //InputStream bodyStream = new GZIPInputStream(is);
                            /*rd = new BufferedReader(new InputStreamReader(is));
                            String rspline="";
                            while ((rspline = in.readLine()) != null) {
                                strb.append("\n");
                                strb.append(rspline);
                            }*/
                                //rdb = new BufferedReader(new InputStreamReader(bodyStream));
                        } catch (IOException ioe) {
                            System.out.println("********* IO EXCEPTION **********: " + ioe);
                        }
                    }

                }

                //FIN Consumir server destino

                //INICIO retornar response al cliente

                byte by[] = new byte[ BUFFER_SIZE ];
                int index = is.read( by, 0, BUFFER_SIZE );
                while ( index != -1 )
                {
                    out.write( by, 0, index );
                    index = is.read( by, 0, BUFFER_SIZE );
                }
                out.flush();


                //FIN retornar response al cliente
            } catch (Exception e) {
                System.err.println("Exception: " + e);
                strb.append("\n");
                strb.append("Exception: " + e);
                out.writeBytes("");
            }
            finally {
                if(con!=null)
                {
                    con.disconnect();
                }
            }

            //cerramos recursos
            if (rd != null) {
                rd.close();
            }
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
            if (socket != null) {
                socket.close();
            }

            strb.append("\n");
            strb.append("#####################################FIN TRANSACCION#####################################");
            log.info(strb.toString());
        } catch (IOException e) {
            e.printStackTrace();
            strb.append(e.getMessage());
            strb.append("\n");
            strb.append("#####################################FIN TRANSACCION#####################################");
            log.info(strb.toString());
        }
    }

}