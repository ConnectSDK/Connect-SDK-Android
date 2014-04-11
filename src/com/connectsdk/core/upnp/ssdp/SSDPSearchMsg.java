/*
 * SSDPSearchMsg
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

public class SSDPSearchMsg {
    static final String HOST = "HOST: " + SSDP.ADDRESS + ":" + SSDP.PORT;
    static final String MAN = "MAN: \"ssdp:discover\"";
    static final String UDAP = "USER-AGENT: UDAP/2.0";
    
    int mMX = 5;    /* seconds to delay response */
    String mST;     /* Search target */
    
    public SSDPSearchMsg(String ST) {
        mST = ST;
    }
    
    public int getmMX() {
        return mMX;
    }

    public void setmMX(int mMX) {
        this.mMX = mMX;
    }

    public String getmST() {
        return mST;
    }

    public void setmST(String mST) {
        this.mST = mST;
    }
    
    @Override
    public String toString() {
        StringBuilder content = new StringBuilder();
        
        content.append(SSDP.SL_MSEARCH).append(SSDP.NEWLINE);
        content.append(HOST).append(SSDP.NEWLINE);
        content.append(MAN).append(SSDP.NEWLINE);
        content.append(SSDP.ST + ": " + mST).append(SSDP.NEWLINE);
        content.append("MX: " + mMX).append(SSDP.NEWLINE);
        if ( mST.contains("udap") ) {
        	content.append(UDAP).append(SSDP.NEWLINE);
        }
        content.append(SSDP.NEWLINE);
        
        return content.toString();
    }
}