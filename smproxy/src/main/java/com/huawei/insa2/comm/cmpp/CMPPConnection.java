// Decompiled by DJ v2.9.9.60 Copyright 2000 Atanas Neshkov Date: 2005-7-11 17:43:33
// Home Page : http://members.fortunecity.com/neshkov/dj.html - Check often for new version!
// Decompiler options: packimports(3)
// Source File Name: CMPPConnection.java

package com.huawei.insa2.comm.cmpp;

import com.huawei.insa2.comm.*;
import com.huawei.insa2.comm.cmpp.message.CMPPActiveMessage;
import com.huawei.insa2.comm.cmpp.message.CMPPActiveRepMessage;
import com.huawei.insa2.comm.cmpp.message.CMPPConnectMessage;
import com.huawei.insa2.comm.cmpp.message.CMPPConnectRepMessage;
import com.huawei.insa2.comm.cmpp.message.CMPPMessage;
import com.huawei.insa2.comm.cmpp.message.CMPPTerminateMessage;
import com.huawei.insa2.util.Args;
import com.huawei.insa2.util.Resource;

import java.io.*;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

// Referenced classes of package com.huawei.insa2.comm.cmpp:
// CMPPWriter, CMPPReader, CMPPTransaction, CMPPConstant
/**
 * PCMPP协议的连接层。
 */
public class CMPPConnection extends PSocketConnection {

    private final AtomicInteger degree          = new AtomicInteger(); // 计数器,计算连续发出的心跳消息没有响应的个数
    private final AtomicInteger hbnoResponseOut = new AtomicInteger(); // 连续发出心跳消息没有响应达到该数，需要重连
    private String              source_addr     = null;
    private int                 version;                              // 双方协商的版本号
    private String              shared_secret;                        // 事先商定的值，用于生成SP认证码

    public CMPPConnection(Args args) {
        degree.set(0);
        hbnoResponseOut.set(args.get("heartbeat-noresponseout", RECONNECT_THESHOLD));
        source_addr = args.get("source-addr", "huawei");
        version = args.get("version", 1);
        shared_secret = args.get("shared-secret", "");
        CMPPConstant.debug = args.get("debug", false);

        // 加载CMPP定义的错误码信息至JVM
        CMPPConstant.initConstant(getResource());

        // 初始化线程参数信息
        init(args);
    }

    protected PWriter getWriter(OutputStream out) {
        return new CMPPWriter(out);
    }

    protected PReader getReader(InputStream in) {
        return new CMPPReader(in);
    }

    public int getChildId(PMessage message) {
        CMPPMessage mes = (CMPPMessage) message;
        int sequenceId = mes.getSequenceId();
        if (mes.getCommandId() == 5 || mes.getCommandId() == 8 || mes.getCommandId() == 2) return -1;
        else return sequenceId;
    }

    public PLayer createChild() {
        return new CMPPTransaction(this);
    }

    public int getTransactionTimeout() {
        return super.transactionTimeout;
    }

    public Resource getResource() {
        try {
            Resource resource = new Resource(getClass(), "resource");
            return resource;
        } catch (IOException e) {
            e.printStackTrace();
        }
        Resource resource1 = null;
        return resource1;
    }

    public synchronized void waitAvailable() {
        try {
            if (getError() == PSocketConnection.NOT_INIT) wait(super.transactionTimeout);
        } catch (InterruptedException interruptedexception) {
        }
    }

    /**
     * 关闭线程，发送关闭数据包通知停止
     * 
     * @see com.huawei.insa2.comm.PSocketConnection#close()
     */
    @Override
    public void close() {
        try {
            CMPPTerminateMessage msg = new CMPPTerminateMessage();
            send(msg);
        } catch (PException pexception) {
        }
        super.close();
    }

    /**
     * 心跳包检测，如果达到错误阈值，则需要重连
     * 
     * @throws IOException
     * @see com.huawei.insa2.comm.PSocketConnection#heartbeat()
     */
    @Override
    protected void heartbeat() throws IOException {
        CMPPTransaction t = (CMPPTransaction) createChild();
        CMPPActiveMessage hbmes = new CMPPActiveMessage();
        t.send(hbmes);
        t.waitResponse();
        CMPPActiveRepMessage rsp = (CMPPActiveRepMessage) t.getResponse();
        if (rsp == null) {
            if (degree.incrementAndGet() == hbnoResponseOut.get()) {
                // 重新清零，并抛出重连异常
                degree.set(0);
                throw new IOException(CMPPConstant.HEARTBEAT_ABNORMITY);
            }
        } else {
            // 如果回执信息不为空，表明连接正常，置计数器为0
            degree.set(0);
        }
        t.close();
    }

    /**
     * 连接
     * @see com.huawei.insa2.comm.PSocketConnection#connect()
     */
    protected synchronized void connect() {
        super.connect();
        if (!available()) return;
        CMPPConnectMessage request = null;
        CMPPConnectRepMessage rsp = null;
        try {
            request = new CMPPConnectMessage(source_addr, version, shared_secret, new Date());
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            close();
            setError(CMPPConstant.CONNECT_INPUT_ERROR);
        }
        CMPPTransaction t = (CMPPTransaction) createChild();
        try {
            t.send(request);
            PMessage m = super.in.read();
            onReceive(m);
        } catch (IOException e) {
            e.printStackTrace();
            close();
            setError(String.valueOf(CMPPConstant.LOGIN_ERROR) + String.valueOf(explain(e)));
        }
        rsp = (CMPPConnectRepMessage) t.getResponse();
        if (rsp == null) {
            close();
            setError(CMPPConstant.CONNECT_TIMEOUT);
        }
        t.close();
        if (rsp != null && rsp.getStatus() != 0) {
            close();
            if (rsp.getStatus() == 1) setError(CMPPConstant.STRUCTURE_ERROR);
            else if (rsp.getStatus() == 2) setError(CMPPConstant.NONLICETSP_ID);
            else if (rsp.getStatus() == 3) setError(CMPPConstant.SP_ERROR);
            else if (rsp.getStatus() == 4) setError(CMPPConstant.VERSION_ERROR);
            else setError(CMPPConstant.OTHER_ERROR);
        }
        notifyAll();
    }
}
