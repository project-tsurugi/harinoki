package com.tsurugidb.harinoki;

class Response {

    final int code;

    final MessageType type;

    final String token;

    final String message;

    Response(int code, MessageType type, String token, String message) {
        this.code = code;
        this.type = type;
        this.token = token;
        this.message = message;
    }

    @Override
    public String toString() {
        return String.format("Response [code=%s, type=%s, token=%s, message=%s]", code, type, token, message);
    }
}
