package com.lmit.jenkins.android.networking;

import java.io.IOException;

public class ClientDisconnectedException extends IOException {
	private static final long serialVersionUID = -4489995879554657007L;

	public ClientDisconnectedException() {
		super("Client in OFF-LINE Mode: go on-line and try again");
	}
}
