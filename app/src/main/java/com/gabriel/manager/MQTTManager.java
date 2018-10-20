package com.gabriel.manager;

import com.gabriel.util.Logger;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by Administrator off 2017/5/10/010.
 */
public class MQTTManager  {
    private static String userName = "cardetecter";
    private static String password = "123456";
    private static String clientId = "phone1";
    private MqttClient client;
    private MqttConnectOptions conOpt;
    private boolean clean = true;
    private ScheduledExecutorService scheduler;

    private static MQTTManager manager = new MQTTManager();
    private MQTTManager(){

    }

    public static MQTTManager getInstance(){
        return manager;
    }

    public MqttClient getClient(){
        return client;
    }

    /**
     * 释放单例, 及其所引用的资源
     * */
    public static void release() {
        try {
            if (manager != null) {
                manager.disConnect();
                manager = null;
            }
        } catch (Exception e) {
        }
    }
    /**
     * 创建Mqtt 连接
     *
     * @param brokerUrl Mqtt服务器地址(tcp://xxxx:1863)
     * @param userName  用户名
     * @param password  密码
     * @param clientId  clientId
     * @return
     */
    public boolean creatConnect(String brokerUrl, String userName, String password, String clientId) {
        Logger.I("creatConnect---url: " + brokerUrl + " ,clientId: " + clientId);
        boolean flag = false;
        String tmpDir = System.getProperty("java.io.tmpdir");
        MqttDefaultFilePersistence dataStore = new MqttDefaultFilePersistence(tmpDir);
        try {
            // Construct the connection options object that contains connection parameters
            // such as cleanSession and LWT
            conOpt = new MqttConnectOptions();
            conOpt.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
            conOpt.setCleanSession(clean);
            if (password != null) {
                conOpt.setPassword(password.toCharArray());
            }else{
                conOpt.setPassword(MQTTManager.password.toCharArray());
            }
            if (userName != null) {
                conOpt.setUserName(userName);
            }else{
                conOpt.setUserName(MQTTManager.userName);
            }
            if(clientId == null){
                clientId = MQTTManager.clientId;
            }
            client = new MqttClient(brokerUrl, clientId, dataStore);
            flag = doConnect();
        } catch (MqttException e) {
            Logger.E(e.getMessage());
        }
        return flag;
    }
    /**
     * 建立连接
     *
     * @return
     */
    private boolean doConnect() {
        boolean flag = false;
        if (client != null) {
            try {
                client.connect(conOpt);
                Logger.D("Connected to " + client.getServerURI() + " with client ID " + client.getClientId());
                flag = true;
            } catch (Exception e) {
                Logger.E("doConnect Exception: " + e.getMessage());
            }
        }
        return flag;
    }
    /**
     * Publish / send a message to an MQTT server
     *
     * @param topicName the name of the topic to publish to
     * @param qos       the quality of service to delivery the message at (0,1,2)
     * @param payload   the set of bytes to send to the MQTT server
     * @return boolean
     */
    public boolean publish(String topicName, int qos, byte[] payload) {
        boolean flag = false;
        if (client != null && client.isConnected()) {
            Logger.D("Publishing to topic \"" + topicName + "\" qos " + qos);
            // Create and configure a message
            MqttMessage message = new MqttMessage(payload);
            message.setQos(qos);
            // Send the message to the server, control is not returned until
            // it has been delivered to the server meeting the specified
            // quality of service.
            try {
                client.publish(topicName, message);
                flag = true;
            } catch (MqttException e) {
            }
        }
        return flag;
    }
    /**
     * Subscribe to a topic off an MQTT server
     * Once subscribed this method waits for the messages to arrive from the server
     * that match the subscription. It continues listening for messages until the enter key is
     * pressed.
     *
     * @param topicName to subscribe to (can be wild carded)
     * @param qos       the maximum quality of service to receive messages at for this subscription
     * @return boolean
     */
    public boolean subscribe(String topicName, int qos) {
        boolean flag = false;
        if (client != null && client.isConnected()) {
            // Subscribe to the requested topic
            // The QoS specified is the maximum level that messages will be sent to the client at.
            // For instance if QoS 1 is specified, any messages originally published at QoS 2 will
            // be downgraded to 1 when delivering to the client but messages published at 1 and 0
            // will be received at the same level they were published at.
            Logger.D("Subscribing to topic \"" + topicName + "\" qos " + qos);
            try {
                client.subscribe(topicName, qos);
                flag = true;
            } catch (MqttException e) {
            }
        }
        return flag;
    }

    /**
     * 重新链接
     */
    public void startReconnect() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(new Runnable() {
            public void run() {
                if (!client.isConnected()) {
                    try {
                        client.connect(conOpt);
                    } catch (MqttSecurityException e) {
                        e.printStackTrace();
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, 0 * 1000, 10 * 1000, TimeUnit.MILLISECONDS);
    }

    /**
     * 取消连接
     *
     * @throws MqttException
     */
    public void disConnect() throws MqttException {
        if (client != null && client.isConnected()) {
            client.disconnect();
        }
    }
}
