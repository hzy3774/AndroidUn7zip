package com.hzy.un7zip.event;

public class MessageEvent {
    public static final int SHOW_MSG = 0;
    public static final int DISMISS_MSG = 1;

    public int type;
    public String message;

    public MessageEvent(int type) {
        this.type = type;
    }

    public MessageEvent(int type, String message) {
        this.type = type;
        this.message = message;
    }
}
