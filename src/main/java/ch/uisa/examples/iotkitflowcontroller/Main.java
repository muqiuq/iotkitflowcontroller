package ch.uisa.examples.iotkitflowcontroller;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class Main {

    public static void log(String s) {
        System.out.println(s);
    }

    public static void log(Exception e) {
        e.printStackTrace();
    }

    public static void main(String[] args) {

        int qos             = 2;
        String broker       = "tcp://cloud.tbz.ch:1883";
        String clientId     = "WorkflowControllerGroupN";
        String username     = null;
        String password     = null;
        MemoryPersistence persistence = new MemoryPersistence();

        try {
            final MqttClient mqttClient = new MqttClient(broker, clientId, persistence);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            if(username != null) {
                connOpts.setUserName(username);;
                connOpts.setPassword(password.toCharArray());;
                connOpts.setConnectionTimeout(4);
                connOpts.setAutomaticReconnect(true);
            }
            connOpts.setCleanSession(true);
            log("Connecting to broker: "+broker);
            mqttClient.connect(connOpts);
            log("Connected");
            mqttClient.subscribe("#");
            mqttClient.setCallback(new CustomMqttCallback());

            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    try {
                        mqttClient.disconnect();
                    } catch (MqttException e) {
                        log(e);
                    }
                    System.out.println("Disconnected");
                }
            });

            // Example Turn On Led1 two seconds on and off
            (new Thread() {
                public void run() {
                    try {
                        publishLed("11", mqttClient);
                        Thread.sleep(3000);
                        publishLed("10", mqttClient);
                    } catch (MqttException | InterruptedException e) {
                        log(e);
                    }
                }
            }).start();

            boolean connectionLost = false;
            try {
                while(true) {
                    if(!mqttClient.isConnected() && !connectionLost) {
                        log("Connection lost");
                        connectionLost = true;
                    }else if(mqttClient.isConnected() && connectionLost) {
                        connectionLost = false;
                    }
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                log(e);
            }

        } catch(MqttException me) {
            System.out.println("reason "+me.getReasonCode());
            System.out.println("msg "+me.getMessage());
            System.out.println("loc "+me.getLocalizedMessage());
            System.out.println("cause "+me.getCause());
            System.out.println("excep "+me);
            me.printStackTrace();
        }
    }

    private static void publishLed(String content, MqttClient mqttClient) throws MqttException {
        MqttMessage message = new MqttMessage(content.getBytes());
        mqttClient.publish("iotkit/actors/led", message);
    }

}
