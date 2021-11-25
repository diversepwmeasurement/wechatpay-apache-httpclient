package com.wechat.pay.contrib.apache.httpclient.auth;

import com.wechat.pay.contrib.apache.httpclient.Credentials;
import com.wechat.pay.contrib.apache.httpclient.cert.CertManagerSingleton;
import java.security.cert.X509Certificate;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 在原有 CertificatesVerifier 基础上，增加定时更新证书功能（默认1天）
 */
public class ScheduleUpdateCertificatesVerifier implements Verifier {

    protected static final int UPDATE_INTERVAL_MINUTE = 1440;
    private final ReentrantLock lock;
    private final CertManagerSingleton certManagerSingleton;
    private final CertificatesVerifier verifier;

    public ScheduleUpdateCertificatesVerifier(Credentials credentials, byte[] apiv3Key) {
        lock = new ReentrantLock();
        certManagerSingleton = CertManagerSingleton.getInstance();
        initCertManager(credentials, apiv3Key);
        verifier = new CertificatesVerifier(certManagerSingleton.getCertificates());
    }

    /**
     * 初始化平台证书管理器
     *
     * @param credentials
     * @param apiv3Key
     */
    public void initCertManager(Credentials credentials, byte[] apiv3Key) {
        if (credentials == null || apiv3Key.length == 0) {
            throw new IllegalArgumentException("credentials 或 apiv3Key 为空");
        }
        certManagerSingleton.init(credentials, apiv3Key, UPDATE_INTERVAL_MINUTE);
    }

    @Override
    public X509Certificate getLatestCertificate() {
        return certManagerSingleton.getLatestCertificate();
    }

    @Override
    public boolean verify(String serialNumber, byte[] message, String signature) {
        if (serialNumber.isEmpty() || message.length == 0 || signature.isEmpty()) {
            throw new IllegalArgumentException("serialNumber 或 message 或 signature 为空");
        }
        if (lock.tryLock()) {
            try {
                verifier.updateCertificates(certManagerSingleton.getCertificates());
            } finally {
                lock.unlock();
            }
        }
        return verifier.verify(serialNumber, message, signature);
    }

    /**
     * 该方法已废弃，请勿使用
     *
     * @return null
     */
    @Deprecated
    @Override
    public X509Certificate getValidCertificate() {
        return null;
    }


    /**
     * 停止定时更新，停止无法再重新启动
     */
    public void stopScheduleUpdate() {
        certManagerSingleton.close();
    }

}
