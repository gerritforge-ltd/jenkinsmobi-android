package com.lmit.jenkins.android.networking;

import com.lmit.jenkinscloud.commons.SyncCallback;

public class SyncCallbackWrapper<T> implements SyncCallback<T> {
	private SyncCallback<T> wrappedCallback;

	public SyncCallbackWrapper(SyncCallback<T> wrappedCallback) {
		this.wrappedCallback = wrappedCallback;
	}

	@Override
	public void onSuccess(T result) {
		wrappedCallback.onSuccess(result);
	}

	@Override
	public void onFailure(Throwable e) {
		wrappedCallback.onFailure(e);
	}
}
