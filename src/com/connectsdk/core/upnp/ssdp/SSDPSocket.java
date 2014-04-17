/*
 * SSDPSocket
 * Connect SDK
 * 
 * Copyright (c) 2014 LG Electronics.
 * Copyright (c) 2011 stonker.lee@gmail.com https://code.google.com/p/android-dlna/
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.connectsdk.core.upnp.ssdp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketException;

public class SSDPSocket {
    SocketAddress mSSDPMulticastGroup;

    DatagramSocket wildSocket;
    MulticastSocket mLocalSocket;
    
    NetworkInterface mNetIf;
    InetAddress localInAddress;

    int timeout = 0;
    
    public SSDPSocket(InetAddress source) throws IOException {
        localInAddress = source;
        System.out.println("Local address: " + localInAddress.getHostAddress());

        mSSDPMulticastGroup = new InetSocketAddress(SSDP.ADDRESS, SSDP.PORT);

        mLocalSocket = new MulticastSocket(SSDP.PORT);

        mNetIf = NetworkInterface.getByInetAddress(localInAddress);
        mLocalSocket.joinGroup(mSSDPMulticastGroup, mNetIf);
        
    	wildSocket = new DatagramSocket(null);
    	wildSocket.setReuseAddress(true);
    	wildSocket.bind(new InetSocketAddress(localInAddress, SSDP.SOURCE_PORT));
    }

    /** Used to send SSDP packet */
    public void send(String data) throws IOException {
        DatagramPacket dp = new DatagramPacket(data.getBytes(), data.length(), mSSDPMulticastGroup);

        wildSocket.send(dp);
    }


    /** Used to receive SSDP Response packet */
    public DatagramPacket responseReceive() throws IOException {
        byte[] buf = new byte[1024];
        DatagramPacket dp = new DatagramPacket(buf, buf.length);

        wildSocket.receive(dp);

        return dp;
    }
    
    /** Used to receive SSDP Notify packet */
    public DatagramPacket notifyReceive() throws IOException {
        byte[] buf = new byte[1024];
        DatagramPacket dp = new DatagramPacket(buf, buf.length);

        mLocalSocket.receive(dp);

        return dp;
    }
    
//    /** Starts the socket */
//    public void start() {
//    	
//    }
    
    public boolean isConnected() {
    	return wildSocket != null && mLocalSocket != null && wildSocket.isConnected() && mLocalSocket.isConnected();
    }

    /** Close the socket */
    public void close() {
        if (mLocalSocket != null) {
            try {
                mLocalSocket.leaveGroup(mSSDPMulticastGroup, mNetIf);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mLocalSocket.close();
        }
        
        if (wildSocket != null) {
            wildSocket.disconnect();
            wildSocket.close();
        }
    }
    
    public void setTimeout(int timeout) throws SocketException {
    	this.timeout = timeout;
    	wildSocket.setSoTimeout(this.timeout);
    }
}