package com.android.socket.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.io.RandomAccessFile;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.android.socket.utils.StreamTool;

public class SocketServer {
	
	private String uploadPath="C:/hangyjx/android_resource";
	private ExecutorService executorService;// �̳߳�
	private ServerSocket ss = null;
	private int port;// �����˿�
	private boolean quit;// �Ƿ��˳�
	private Map<Long, FileLog> datas = new HashMap<Long, FileLog>();// ��Ŷϵ����ݣ���ø�Ϊ���ݿ���

	public SocketServer(int port) {
		this.port = port;
		// ��ʼ���̳߳�
		executorService = Executors.newFixedThreadPool(Runtime.getRuntime()
				.availableProcessors() * 50);
	}

	// ��������
	public void start() throws Exception {
		ss = new ServerSocket(port);
		while (!quit) {
			Socket socket = ss.accept();// ���ܿͻ��˵�����
			// Ϊ֧�ֶ��û��������ʣ������̳߳ع���ÿһ���û�����������
			executorService.execute(new SocketTask(socket));// ����һ���߳�����������
		}
	}

	// �˳�
	public void quit() {
		this.quit = true;
		try {
			ss.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws Exception {
		SocketServer server = new SocketServer(7878);
		server.start();
	}

	private class SocketTask implements Runnable {
		private Socket socket;

		public SocketTask(Socket socket) {
			this.socket = socket;
		}

		@Override
		public void run() {
			try {
				System.out.println("accepted connenction from "
						+ socket.getInetAddress() + " @ " + socket.getPort());
				PushbackInputStream inStream = new PushbackInputStream(
						socket.getInputStream());
				// �õ��ͻ��˷����ĵ�һ��Э�����ݣ�Content-Length=143253434;filename=xxx.3gp;sourceid=
				// ����û������ϴ��ļ���sourceid��ֵΪ�ա�
				String head = StreamTool.readLine(inStream);
				System.out.println(head);
				if (head != null) {
					// �����Э�������ж�ȡ���ֲ���ֵ
					String[] items = head.split(";");
					String filelength = items[0].substring(items[0].indexOf("=") + 1);
					String filename = items[1].substring(items[1].indexOf("=") + 1);
					String sourceid = items[2].substring(items[2].indexOf("=") + 1);
					Long id = System.currentTimeMillis();
					FileLog log = null;
					if (null != sourceid && !"".equals(sourceid)) {
						id = Long.valueOf(sourceid);
						log = find(id);//�����ϴ����ļ��Ƿ�����ϴ���¼
					}
					File file = null;
					int position = 0;
					if(log==null){//����ϴ����ļ��������ϴ���¼,Ϊ�ļ���Ӹ��ټ�¼
						String path = new SimpleDateFormat("yyyy/MM/dd/HH/mm").format(new Date());
//						File dir = new File(uploadPath+ path);
						File dir = new File(uploadPath);
						if(!dir.exists()) dir.mkdirs();
						file = new File(dir, filename);
						if(file.exists()){//����ϴ����ļ�����������Ȼ����и���
							filename = filename.substring(0, filename.indexOf(".")-1)+ dir.listFiles().length+ filename.substring(filename.indexOf("."));
							file = new File(dir, filename);
						}
						save(id, file);
					}else{// ����ϴ����ļ������ϴ���¼,��ȡ�ϴεĶϵ�λ��
						file = new File(log.getPath());//���ϴ���¼�еõ��ļ���·��
						if(file.exists()){
							File logFile = new File(file.getParentFile(), file.getName()+".log");
							if(logFile.exists()){
								Properties properties = new Properties();
								properties.load(new FileInputStream(logFile));
								position = Integer.valueOf(properties.getProperty("length"));//��ȡ�ϵ�λ��
							}
						}
					}
					
					OutputStream outStream = socket.getOutputStream();
					String response = "sourceid="+ id+ ";position="+ position+ "\r\n";
					//�������յ��ͻ��˵�������Ϣ�󣬸��ͻ��˷�����Ӧ��Ϣ��sourceid=1274773833264;position=0
					//sourceid�ɷ������ɣ�Ψһ��ʶ�ϴ����ļ���positionָʾ�ͻ��˴��ļ���ʲôλ�ÿ�ʼ�ϴ�
					outStream.write(response.getBytes());
					
					RandomAccessFile fileOutStream = new RandomAccessFile(file, "rwd");
					if(position==0) fileOutStream.setLength(Integer.valueOf(filelength));//�����ļ�����
					fileOutStream.seek(position);//�ƶ��ļ�ָ����λ�ÿ�ʼд������
					byte[] buffer = new byte[1024];
					int len = -1;
					int length = position;
					while( (len=inStream.read(buffer)) != -1){//���������ж�ȡ����д�뵽�ļ���
						fileOutStream.write(buffer, 0, len);
						length += len;
						Properties properties = new Properties();
						properties.put("length", String.valueOf(length));
						FileOutputStream logFile = new FileOutputStream(new File(file.getParentFile(), file.getName()+".log"));
						properties.store(logFile, null);//ʵʱ��¼�ļ�����󱣴�λ��
						logFile.close();
					}
					if(length==fileOutStream.length()) delete(id);
					fileOutStream.close();					
					inStream.close();
					outStream.close();
					file = null;
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
	                if(socket != null && !socket.isClosed()) socket.close();
	            } catch (IOException e) {}
			}
		}

	}

	public FileLog find(Long sourceid) {
		return datas.get(sourceid);
	}

	// �����ϴ���¼
	public void save(Long id, File saveFile) {
		// �պ���Ըĳ�ͨ�����ݿ���
		datas.put(id, new FileLog(id, saveFile.getAbsolutePath()));
	}

	// ���ļ��ϴ���ϣ�ɾ����¼
	public void delete(long sourceid) {
		if (datas.containsKey(sourceid))
			datas.remove(sourceid);
	}

	private class FileLog {
		private Long id;
		private String path;
		
		public FileLog(Long id, String path) {
			super();
			this.id = id;
			this.path = path;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getPath() {
			return path;
		}

		public void setPath(String path) {
			this.path = path;
		}

	}
}
