package com.tsurugidb.harinoki;

class Response {

    final int code;

    final MessageType type;

    final String token;

    final String message;

    final String key_type;

    final String key_data;

    Response(int code, MessageType type, String token, String message, String key_type, String key_data) {
        this.code = code;
        this.type = type;
        this.token = token;
        this.message = message;
        this.key_type = key_type;
        this.key_data = key_data;
    }

    @Override
    public String toString() {
        return String.format("Response [code=%s, type=%s, token=%s, message=%s, key_type=%s, key_data=%s]", code, type, token, message, key_type, key_data);
    }
}
