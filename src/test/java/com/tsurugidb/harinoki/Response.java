package com.tsurugidb.harinoki;

class Response {

    final int code;

    final String token;

    final String message;

    Response(int code, String token, String message) {
        this.code = code;
        this.token = token;
        this.message = message;
    }

    @Override
    public String toString() {
        return String.format("Response [code=%s, token=%s, message=%s]", code, token, message);
    }
}
