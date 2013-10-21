package com.lmit.jenkins.android.networking;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

public class TrustAllSSLSocketFactory extends SSLSocketFactory {

	private SSLContext sslcontext = null;

	private static SSLContext createEasySSLContext() throws IOException {
		try {
			SSLContext context = SSLContext.getInstance("TLS");
			context.init(null,
					new TrustManager[] { new AllowAllX509TrustManager() }, null);
			return context;
		} catch (Exception e) {
			throw new IOException(e.getMessage());
		}
	}

	private SSLContext getSSLContext() throws IOException {
		if (this.sslcontext == null) {
			this.sslcontext = createEasySSLContext();
		}
		return this.sslcontext;
	}

	/**
	 * @see org.apache.http.conn.scheme.SocketFactory#connectSocket(java.net.Socket,
	 *      java.lang.String, int, java.net.InetAddress, int,
	 *      org.apache.http.params.HttpParams)
	 */
	public Socket connectSocket(Socket sock, String host, int port,
			InetAddress localAddress, int localPort, HttpParams params)
			throws IOException, UnknownHostException, ConnectTimeoutException {
		int connTimeout = HttpConnectionParams.getConnectionTimeout(params);
		int soTimeout = HttpConnectionParams.getSoTimeout(params);

		InetSocketAddress remoteAddress = new InetSocketAddress(host, port);
		SSLSocket sslsock = (SSLSocket) ((sock != null) ? sock : createSocket());

		if ((localAddress != null) || (localPort > 0)) {
			// we need to bind explicitly
			if (localPort < 0) {
				localPort = 0; // indicates "any"
			}
			InetSocketAddress isa = new InetSocketAddress(localAddress,
					localPort);
			sslsock.bind(isa);
		}

		sslsock.connect(remoteAddress, connTimeout);
		sslsock.setSoTimeout(soTimeout);
		return sslsock;

	}

	/**
	 * @see org.apache.http.conn.scheme.SocketFactory#createSocket()
	 */
	public Socket createSocket() throws IOException {
		return getSSLContext().getSocketFactory().createSocket();
	}

	/**
	 * @see org.apache.http.conn.scheme.SocketFactory#isSecure(java.net.Socket)
	 */
	public boolean isSecure(Socket socket) throws IllegalArgumentException {
		return true;
	}

	/**
	 * @see org.apache.http.conn.scheme.LayeredSocketFactory#createSocket(java.net.Socket,
	 *      java.lang.String, int, boolean)
	 */
	public Socket createSocket(Socket socket, String host, int port,
			boolean autoClose) throws IOException, UnknownHostException {

		// <Froyo
		// return getSSLContext().getSocketFactory().createSocket();
		
		// >=Froyo
		return getSSLContext().getSocketFactory().createSocket(socket, host,
				port, autoClose);
	}

	public boolean equals(Object obj) {
		return ((obj != null) && obj.getClass().equals(
				TrustAllSSLSocketFactory.class));
	}

	public int hashCode() {
		return TrustAllSSLSocketFactory.class.hashCode();
	}

	public static void trustX509(HttpClient client, int sslPort) {

		try {

			client.getConnectionManager()
					.getSchemeRegistry()
					.register(
							new Scheme("https", new TrustAllSSLSocketFactory(
									null), sslPort));

		} catch (KeyManagementException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnrecoverableKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public TrustAllSSLSocketFactory(KeyStore truststore)
			throws NoSuchAlgorithmException, KeyManagementException,
			KeyStoreException, UnrecoverableKeyException {

		super(truststore);
		this.setHostnameVerifier(ALLOW_ALL_HOSTNAME_VERIFIER);
	}

	static class AllowAllX509TrustManager implements X509TrustManager {

		/**
		 * @see 
		 *      javax.net.ssl.X509TrustManager#checkClientTrusted(X509Certificate
		 *      [],String authType)
		 */
		public void checkClientTrusted(X509Certificate[] certificates,
				String authType) throws CertificateException {
			;
		}

		/**
		 * @see 
		 *      javax.net.ssl.X509TrustManager#checkServerTrusted(X509Certificate
		 *      [],String authType)
		 */
		public void checkServerTrusted(X509Certificate[] certificates,
				String authType) throws CertificateException {
			;
		}

		/**
		 * @see javax.net.ssl.X509TrustManager#getAcceptedIssuers()
		 */
		public X509Certificate[] getAcceptedIssuers() {
			return new X509Certificate[0];
		}
	}
}
