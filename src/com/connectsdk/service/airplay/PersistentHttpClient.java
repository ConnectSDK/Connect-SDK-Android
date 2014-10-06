package com.connectsdk.service.airplay;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.protocol.HTTP;

public class PersistentHttpClient {
	private Socket socket;
	private BufferedReader reader;
	private BufferedWriter writer;
	private final InetAddress inetAddress;
	private final int port;
	
	private final static int bufferLength=1024;
	private final char [] buffer=new char[bufferLength];
	private final String HTTP_PREFIX="HTTP/1";
	private final String HTTP_STATUS="__HTTP_STATUS__";
	
	public class Response {
		public final String headers;
		public final String content;
		public final Map<String, String> headerMap;
		public final int statusCode;
		private Response(String headers, Map<String, String> headerMap, String content, int statusCode) {
			this.headers=headers;
			this.content=content;
			this.headerMap=headerMap;
			this.statusCode=statusCode;
		}
	}
	
	public interface ResponseReceiver {
		void receiveResponse(Response response);
	}
	
	public PersistentHttpClient(InetAddress inetAddress, int port) throws UnknownHostException, IOException {
		this.inetAddress=inetAddress;
		this.port=port;
		connect();
	}

	public void close() {
		try {
			if(!socket.isClosed()) {
				reader.close();
				writer.close();
				socket.close();	
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private void connect() throws IOException {
		socket = new Socket(inetAddress, port);
		reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));		
	}
	
	public static String convertStreamToString(java.io.InputStream is) {
	    java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
	    return s.hasNext() ? s.next() : "";
	}
	
	public void asynRequest(final String reqestData, final ResponseReceiver responseReceiver) {
		new Thread() {
			public void run() {
				try {
					Response response=requestSync(reqestData);
					responseReceiver.receiveResponse(response);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}.start();
	}
	
	private synchronized Response requestSync(String reqestData) throws IOException {
		return request(reqestData);
	}
	
	public Response request(String reqestData) throws IOException {
		writer.write(reqestData);
		writer.flush();
		String headerData=readHeaders(reader);
		Map<String, String> headers=parseHeaders(headerData);
		int statusCode=0;
		int contentLength=-1;
		if(headers.get(HTTP.CONTENT_LEN)!=null) {
			contentLength=Integer.parseInt(headers.get(HTTP.CONTENT_LEN));
		}
		if(headers.get(HTTP_STATUS)!=null) {
			statusCode=Integer.parseInt(headers.get(HTTP_STATUS));
			headers.remove(HTTP_STATUS);
		}

		if(contentLength<0) {
			throw new IOException("Invalid content length in responsem header: "+headerData);
		}
		return new Response(headerData, headers, readContent(reader, contentLength).toString(), statusCode);
	}
	
	private String readHeaders(BufferedReader reader) throws IOException {
		String line;
		StringBuilder sb=new StringBuilder();
		while ((line = reader.readLine()) !=null) {
			sb.append(line);
			sb.append('\n');
			if(line.trim().length()==0) {
				break;
			}
		}
		return sb.toString();
	}
	
	private Map<String, String> parseHeaders(String headerData) throws IOException {
		BufferedReader reader=new BufferedReader(new StringReader(headerData));
		Map<String, String> headers=new HashMap<String, String>();
		String line;
		while ((line = reader.readLine()) !=null) {
			if(line.trim().length()==0) {
				break;
			}
			int pos=line.indexOf(":");
			if(pos>0) {
				headers.put(line.substring(0, pos).trim(), line.substring(pos+1).trim());
			} else if(line.startsWith(HTTP_PREFIX)) {
				String [] tokens=line.split(" ");
				if(tokens.length>1) {
					headers.put(HTTP_STATUS, tokens[1]);
				}
			}
		}
		return headers;
	}
	
	private StringBuilder readContent(BufferedReader reader, int length) throws IOException {
		StringBuilder sb=new StringBuilder(length);
		int read;
		int totalRead=0;
		do {
			read = reader.read(buffer, 0, Math.min(bufferLength, length-totalRead));
			if(read>0) {
				totalRead += read;
				sb.append(buffer, 0, read);
			}
		} while (read != -1 && totalRead < length);
		return sb;
	}
}
