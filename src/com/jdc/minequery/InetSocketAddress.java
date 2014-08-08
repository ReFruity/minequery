package com.jdc.minequery;

public class InetSocketAddress {
    private String host;
    private int port;
    
    public InetSocketAddress (String host, int port) 
            throws InvalidHostException, InvalidPortException {
        if (host.equals("")) {
            throw new InvalidHostException("Host string cannot be empty");
        } else if (1 > port || port > 65535) {
            throw new InvalidPortException("Port must be in range 1-65535");
        }
        else {
            this.host = host;
            this.port = port;
        }
    }
    
    public String getHostName() {
        return host;
    }
    
    public int getPort () {
        return port;
    }

    public class InvalidHostException extends Exception {
        public InvalidHostException (String message) {
            super(message);
        }
    }

    public class InvalidPortException extends Exception {
        public InvalidPortException (String message) {
            super(message);
        }
    }
}
