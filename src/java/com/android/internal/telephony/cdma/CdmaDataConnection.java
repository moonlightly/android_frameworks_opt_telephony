/*
 * Copyright (C) 2006 The Android Open Source Project
 * Copyright (c) 2006, 2012, The Linux Foundation. All rights reserved.
 * Not a Contribution, Apache license notifications and license are retained
 * for attribution purposes only.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.telephony.cdma;

import android.os.Message;
import android.util.Log;

import com.android.internal.telephony.DataConnection;
import com.android.internal.telephony.DataConnectionTracker;
import com.android.internal.telephony.DataProfile;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.RILConstants;
import com.android.internal.telephony.RetryManager;

import java.io.FileDescriptor;
import java.io.PrintWriter;

/**
 * {@hide}
 */
public class CdmaDataConnection extends DataConnection {

    private static final String LOG_TAG = "CDMA";

    // ***** Constructor
    private CdmaDataConnection(CDMAPhone phone, String name, int id, RetryManager rm,
            DataConnectionTracker dct) {
        super(phone, name, id, rm, dct);
    }

    /**
     * Create the connection object
     *
     * @param phone the Phone
     * @param id the connection id
     * @param rm the RetryManager
     * @return CdmaDataConnection that was created.
     */
    static CdmaDataConnection makeDataConnection(CDMAPhone phone, int id, RetryManager rm,
            DataConnectionTracker dct) {
        CdmaDataConnection cdmaDc = new CdmaDataConnection(phone,
                "CdmaDC-" + mCount.incrementAndGet(), id, rm, dct);
        cdmaDc.start();
        if (DBG) cdmaDc.log("Made " + cdmaDc.getName());
        return cdmaDc;
    }

    /**
     * Begin setting up a data connection, calls setupDataCall
     * and the ConnectionParams will be returned with the
     * EVENT_SETUP_DATA_CONNECTION_DONE AsyncResul.userObj.
     *
     * @param cp is the connection parameters
     */
    @Override
    protected void onConnect(ConnectionParams cp) {
        if (DBG) log("CdmaDataConnection Connecting...");

        mApn = cp.apn;
        createTime = -1;
        lastFailTime = -1;
        lastFailCause = FailCause.NONE;

        // TODO: The data profile's profile ID must be set when it is created.
        int dataProfile;
        if ((cp.apn != null) && (cp.apn.types.length > 0) && (cp.apn.types[0] != null) &&
                (cp.apn.types[0].equals(PhoneConstants.APN_TYPE_DUN))) {
            if (DBG) log("CdmaDataConnection using DUN");
            dataProfile = RILConstants.DATA_PROFILE_TETHERED;
        } else {
            dataProfile = RILConstants.DATA_PROFILE_DEFAULT;
        }

        ((DataProfileCdma)mApn).setProfileId(dataProfile);

        // msg.obj will be returned in AsyncResult.userObj;
        Message msg = obtainMessage(EVENT_SETUP_DATA_CONNECTION_DONE, cp);
        msg.obj = cp;
        phone.mCM.setupDataCall(
                Integer.toString(getRilRadioTechnology(RILConstants.SETUP_DATA_TECH_CDMA)),
                Integer.toString(((DataProfileCdma)mApn).getProfileId()),
                null, mApn.user, mApn.password,
                Integer.toString(mApn.getAuthType()),
                mApn.protocol, msg);
    }

    @Override
    public String toString() {
        return "State=" + getCurrentState().getName() + " create=" + createTime + " lastFail="
                + lastFailTime + " lastFasilCause=" + lastFailCause;
    }

    @Override
    protected boolean isDnsOk(String[] domainNameServers) {
        if (NULL_IP.equals(domainNameServers[0])
                && NULL_IP.equals(domainNameServers[1])
                && !phone.isDnsCheckDisabled()) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    protected void log(String s) {
        Log.d(LOG_TAG, "[" + getName() + "] " + s);
    }

    @Override
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("CdmaDataConnection extends:");
        super.dump(fd, pw, args);
    }
}
