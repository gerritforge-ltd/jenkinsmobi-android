package com.lmit.jenkins.android.networking;

import android.content.Context;

import com.lmit.jenkins.android.activity.R;

public class HttpStatusCode {

	public static String STATUS_ABORTED_FOR_TIMEOUT_DESC = "Connection timeout";
	public static final int STATUS_ABORTED_FOR_TIMEOUT = 504; // Gateway Timeout

	public static String STATUS_200 = "OK";
	public static String STATUS_500 = "Internal server error";

	// 400 -> 417
	public static String[] HTTP_ERROR_DESC_400_417 = {
			"Bad Request Syntax error in the client's request.",
			"Unauthorized", "Payment Required", "Forbidden",
			"Resource Not Found", "Method Not Allowed", "Not Acceptable",
			"Proxy Authentication Required", "Request Timeout", "Conflict",
			"URI no longer exists", "Length Required", "Precondition Failed",
			"Request Entity Too Large", "Request-URI Too Long",
			"Unsupported Media Type", "Requested Range Not Satisfiable",
			"Expectation Failed" };

	public static void initLocalizedHttpErrors(Context ctx) {

		int i = 0;
		HTTP_ERROR_DESC_400_417[i] = ctx.getText(R.string.http_error_400)
				.toString();
		i++;
		HTTP_ERROR_DESC_400_417[i] = ctx.getText(R.string.http_error_401)
				.toString();
		i++;
		HTTP_ERROR_DESC_400_417[i] = ctx.getText(R.string.http_error_402)
				.toString();
		i++;
		HTTP_ERROR_DESC_400_417[i] = ctx.getText(R.string.http_error_403)
				.toString();
		i++;
		HTTP_ERROR_DESC_400_417[i] = ctx.getText(R.string.http_error_404)
				.toString();
		i++;
		HTTP_ERROR_DESC_400_417[i] = ctx.getText(R.string.http_error_405)
				.toString();
		i++;
		HTTP_ERROR_DESC_400_417[i] = ctx.getText(R.string.http_error_406)
				.toString();
		i++;
		HTTP_ERROR_DESC_400_417[i] = ctx.getText(R.string.http_error_407)
				.toString();
		i++;
		HTTP_ERROR_DESC_400_417[i] = ctx.getText(R.string.http_error_408)
				.toString();
		i++;
		HTTP_ERROR_DESC_400_417[i] = ctx.getText(R.string.http_error_409)
				.toString();
		i++;
		HTTP_ERROR_DESC_400_417[i] = ctx.getText(R.string.http_error_410)
				.toString();
		i++;
		HTTP_ERROR_DESC_400_417[i] = ctx.getText(R.string.http_error_411)
				.toString();
		i++;
		HTTP_ERROR_DESC_400_417[i] = ctx.getText(R.string.http_error_412)
				.toString();
		i++;
		HTTP_ERROR_DESC_400_417[i] = ctx.getText(R.string.http_error_413)
				.toString();
		i++;
		HTTP_ERROR_DESC_400_417[i] = ctx.getText(R.string.http_error_414)
				.toString();
		i++;
		HTTP_ERROR_DESC_400_417[i] = ctx.getText(R.string.http_error_415)
				.toString();
		i++;
		HTTP_ERROR_DESC_400_417[i] = ctx.getText(R.string.http_error_416)
				.toString();
		i++;
		HTTP_ERROR_DESC_400_417[i] = ctx.getText(R.string.http_error_417)
				.toString();

		STATUS_200 = ctx.getText(R.string.http_error_200).toString();
		STATUS_500 = ctx.getText(R.string.http_error_500).toString();

		STATUS_ABORTED_FOR_TIMEOUT_DESC = ctx.getText(R.string.http_error_504)
				.toString();
	}
}
