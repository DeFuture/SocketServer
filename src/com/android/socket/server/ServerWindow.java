package com.android.socket.server;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Label;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

public class ServerWindow extends Frame{
	private SocketServer server;
	private Label label;
	
	public ServerWindow(String title){
		super(title);
		server = new SocketServer(7070);
		label = new Label();
		add(label, BorderLayout.PAGE_START);
		label.setText("服务器已经启动");
		this.addWindowListener(new WindowListener() {
			@Override
			public void windowOpened(WindowEvent e) {
				new Thread(new Runnable() {			
					@Override
					public void run() {
						try {
							server.start();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}).start();
			}
			
			@Override
			public void windowIconified(WindowEvent e) {
			}
			
			@Override
			public void windowDeiconified(WindowEvent e) {
			}
			
			@Override
			public void windowDeactivated(WindowEvent e) {
			}
			
			@Override
			public void windowClosing(WindowEvent e) {
				 server.quit();
				 System.exit(0);
			}
			
			@Override
			public void windowClosed(WindowEvent e) {
			}
			
			@Override
			public void windowActivated(WindowEvent e) {
			}
		});
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			System.out.println("===========>文件上传服务端已启动");
			SocketServer server = new SocketServer(7070);
			server.start();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		ServerWindow window = new ServerWindow("文件上传服务端"); 
//		window.setSize(300, 300); 
//		window.setVisible(true);
	}

}
