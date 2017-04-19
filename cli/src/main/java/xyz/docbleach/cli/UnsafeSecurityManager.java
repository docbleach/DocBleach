package xyz.docbleach.cli;

import java.net.InetAddress;
import java.security.AccessControlException;
import java.security.Permission;

public class UnsafeSecurityManager extends SecurityManager {
    @Override
    public void checkPermission(Permission perm, Object context) {
        checkPermission(perm);
    }

    @Override
    public void checkPermission(Permission perm) {
        switch (perm.getName()) {
            case "setSecurityManager":
                throw new AccessControlException("Restricted Action", perm);
        }
    }

    @Override
    public void checkConnect(String host, int port) {
        throw new SecurityException("Connect(" + host + ":" + port + ") forbidden.");
    }

    @Override
    public void checkConnect(String host, int port, Object context) {
        throw new SecurityException("Connect(" + host + ":" + port + ") forbidden.");
    }

    @Override
    public void checkListen(int port) {
        throw new SecurityException("Listen(" + port + ") forbidden.");
    }

    @Override
    public void checkAccept(String host, int port) {
        throw new SecurityException("Accept(" + host + ":" + port + ") forbidden.");
    }

    @Override
    public void checkMulticast(InetAddress maddr) {
        throw new SecurityException("Multicast(" + maddr + ") forbidden.");
    }

    @Override
    public void checkPrintJobAccess() {
        throw new SecurityException("Print Job Access forbidden.");
    }

    @Override
    public void checkExec(String cmd) {
        throw new SecurityException("Exec forbidden.");
    }

    @Override
    public void checkSystemClipboardAccess() {
        throw new SecurityException("System Clipboard Access forbidden.");
    }
}