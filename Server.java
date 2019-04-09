package com.server;

import com.ui.*;
import java.io.*;
import java.net.*;

// server s�n�f� ve o s�n�f�n implement etti�i Runnable aray�z�
// bu aray�z�n sahip oldu�u run() metodu implement edildi.
public class Server implements Runnable //sanal class sanal fonksiyon yazd�k run diye.
{
    
    public ServerThread clients[]; 				// server threads array for users.
    public ServerSocket server = null;			// java.net.socket object
    public Thread thread = null;				// server thread object
    public int clientCount = 0, port = 13000; 	// client count and server port 
    public ServerForm ui;					  	// server user interface
    public Users user;						 	// users 

    // Server Constructor 1 
    public Server(ServerForm form)
    {//bir dizi tan�mlad�k 50 client baglanabilir. 
        clients = new ServerThread[50];
        ui = form; 
        user = new Users();
        
		try{ //portu verdi ,
		    server = new ServerSocket(port); 
	        port = server.getLocalPort();
		  //aray�zde baglanan k�s�m
		  ui.jTextArea1.append("Server startet. IP : " + InetAddress.getLocalHost() + ", Port : " + server.getLocalPort());
		    start(); 
	    }catch(IOException ioe){  
	            ui.jTextArea1.append("Can not bind to port : " + port + "\nRetrying"); 
	            ui.RetryStart(0);
		}
    }
    
    // Server Constructor 2
    public Server(ServerForm form, int Port)
    {       
        clients = new ServerThread[50];
        ui = form;
        port = Port;
        user = new Users();
        
		try{  
		    server = new java.net.ServerSocket(port);
	        port = server.getLocalPort();
		    ui.jTextArea1.append("Server started. IP : " + InetAddress.getLocalHost() + ", Port : " + server.getLocalPort());
		    start(); 
	    }catch(IOException ioe){  
	        ui.jTextArea1.append("\nCan not bind to port " + port + ": " + ioe.getMessage()); 
		}
    }
    
    // override Runnable interface method. Server s�n�f�n� thread olarak �al��t�r�r.
	@Override //runnable methodunu yeniden �al��t�r�r
    public void run()
    {
		while (thread != null)
		{
	        try{  //java.net server socket isimli s�n�f�ndan 
	        	ui.jTextArea1.append("\nWaiting for a client ..."); 
		        addThread(server.accept()); //server a bir socket baglanmas�n� kabul ederim. 
		    }
		    catch(Exception ioe){ //kendi server�n �zerinden 
                ui.jTextArea1.append("\nServer accept error: \n");
                ui.RetryStart(0);
		    }
	    }
    }
	
	// thread ba�lat.Server a ait ise ba�lat
    public void start()
    {  
    	if (thread == null)
    	{
            thread = new Thread(this); //server.java y� bir thread a att�k ve paketleyip cal��t�racak
            thread.start();//thread i�ine at�lan nesnenin i�ine gider i�indeki nesnenin run metodunu cag�r�r.
    	}
    }
    
    // thread durdur.
    @SuppressWarnings("deprecation")
	public void stop()
    {
        if (thread != null)
        {
            thread.stop(); 
            thread = null;
        }
    }
    
    // Servere ba�lanan bir istemcinin id sine g�re istemciyi arama. istemci servere ba�l� ise id si de�il ise -1 d�ner.
    private int findClient(int ID)
    { 
    	for (int i = 0; i < clientCount; i++)
    	{
        	if (clients[i].getID() == ID)
        	{
                return i;
            }
    	}
    	return -1;
    }
	
    
    // belirli bir istemciden gelen mesajlara senkronla�t�rma(bknz. synchronized) yaparak cevap verir. mesajlar� i�ler.
    // mesaj�n t�r�ne g�re i�lemi devam ettirir. ID parametresi mesaj�n gidece�i istemciyi ifade eder. Message msg parametresi de iletilecek mesajd�r.
    public synchronized void handle(int ID, Message msg)
    {    	
    	if (msg.content.equals(".bye")) //
    	{
	            Announce("signout", "SERVER", msg.sender);//c�kan ki�iyi duyurur herkese
	            remove(ID); //uygulamadan kendi kullan�c�n� kald�rabilirisin 
		}else //��kmak istemezse
		{
            if(msg.type.equals("login")) // login iste�i
            {
            	if(findUserThread(msg.sender) == null)//login olmak isteyen kullan�c� o anda sistemde login mi?2. defa login olmas�n
                {                                     //type � login olan mesaj�n i�eri�i pass oluyor
                    if(user.checkLogin(msg.sender, msg.content)) // login i�leminde msg.sender=username msg.content=password
                    {
                        clients[findClient(ID)].username = msg.sender;
                        clients[findClient(ID)].send(new Message("login", "SERVER", "TRUE", msg.sender)); // login ba�ar�l�
                        Announce("newuser", "SERVER", msg.sender); 
                        sendUserList(msg.sender);
                    }else
                    {
                        clients[findClient(ID)].send(new Message("login", "SERVER", "FALSE", msg.sender)); // login ba�ar�s�z
                    } 
                }else
                {
                    clients[findClient(ID)].send(new Message("login", "SERVER", "FALSE", msg.sender)); // client in thread'i yoksa(null) login ba�ar�s�z
                }
            }else if(msg.type.equals("message")) // yeni mesaj g�nderme
            {
                if(msg.recipient.equals("All")) // mesaj herkese gidecekse
                {
                    Announce("message", msg.sender, msg.content);
                }else // mesaj belirli bir ki�iye gidecekse
                {
                    findUserThread(msg.recipient).send(new Message(msg.type, msg.sender, msg.content, msg.recipient));
                    clients[findClient(ID)].send(new Message(msg.type, msg.sender, msg.content, msg.recipient));
                }
            }else if(msg.type.equals("test")) // test mesaj�
            {
                clients[findClient(ID)].send(new Message("test", "SERVER", "OK", msg.sender));
            }else if(msg.type.equals("signup")) // yeni kullan�c� kayd� mesaj�
            {
                if(findUserThread(msg.sender) == null)
                {
                    if(!user.userExists(msg.sender)) // kullan�c� kay�tl� de�ilse
                    {
                        user.addUser(msg.sender, msg.content); // kullan�c�y� username ve pass ile kaydet.
                        clients[findClient(ID)].username = msg.sender;
                        clients[findClient(ID)].send(new Message("signup", "SERVER", "TRUE", msg.sender)); // �nce kullan�c� kayd� yap
                        clients[findClient(ID)].send(new Message("login", "SERVER", "TRUE", msg.sender));  // sonra login ol
                        Announce("newuser", "SERVER", msg.sender);
                        sendUserList(msg.sender);
                    }else // kullan�c� sistemde kay�tl�ysa
                    {
                        clients[findClient(ID)].send(new Message("signup", "SERVER", "FALSE", msg.sender)); // kullan�c� kay�tl�ysa kay�t ba�ar�s�z.
                    }
                }else  // client in thread'i yoksa(null) signup ba�ar�s�z.
                {
                    clients[findClient(ID)].send(new Message("signup", "SERVER", "FALSE", msg.sender));
                }
            }else if(msg.type.equals("upload_req")) // dosya g�nderme iste�i
            {
                if(msg.recipient.equals("All")) // t�m kullan�c�lara
                {	// t�m kullan�c�lara dosya g�nderme yasak mesaj� SERVER taraf�ndan dosya g�ndermek isteyen kullan�c�ya g�nderilir. -> "Uploading to 'All' forbidden"
                    clients[findClient(ID)].send(new Message("message", "SERVER", "Uploading to 'All' forbidden", msg.sender));
                }else // belirli bir kullan�c�ya
                {	// dosya al�c�s�na dosya g�nderme iste�i(upload_req) t�r�nden mesaj g�nderilir.
                    findUserThread(msg.recipient).send(new Message("upload_req", msg.sender, msg.content, msg.recipient));
                }
            }else if(msg.type.equals("upload_res")) // dosya kabul�
            {
                if(!msg.content.equals("NO")) // dosya kabul edilmi�se.
                {
                    String IP = findUserThread(msg.sender).socket.getInetAddress().getHostAddress(); // g�nderenin ip adresi
                    findUserThread(msg.recipient).send(new Message("upload_res", IP, msg.content, msg.recipient));
                }else
                {
                    findUserThread(msg.recipient).send(new Message("upload_res", msg.sender, msg.content, msg.recipient));
                }
            }
		}
    }
    
    // anons -> b�t�n kullan�c�lara mesaj g�nderme metodu
    public void Announce(String type, String sender, String content)
    {
        Message msg = new Message(type, sender, content, "All");
        for(int i = 0; i < clientCount; i++)
        {
            clients[i].send(msg);
        }
    }
    
    // sisteme yeni bir kullan�c�n�n geldi�ini di�er kullan�c�lara server haber verir.
    public void sendUserList(String toWhom)
    {
        for(int i = 0; i < clientCount; i++)
        {
            findUserThread(toWhom).send(new Message("newuser", "SERVER", clients[i].username, toWhom));
        }
    }
    
    // sistemdeki kullan�c�n�n thread'ini username kullanarak bulur.
    public ServerThread findUserThread(String usr)
    {
        for(int i = 0; i < clientCount; i++)
        {
            if(clients[i].username.equals(usr))
            {
                return clients[i];
            }
        }
        return null;
    }
	
    // sistemden logout olan kullan�c�n�n thread objesini sonland�r�r ve siler.
    @SuppressWarnings("deprecation")
    public synchronized void remove(int ID)
    {  
	    int pos = findClient(ID);
        if (pos >= 0)
        {  
            ServerThread toTerminate = clients[pos];
            ui.jTextArea1.append("\nRemoving client thread " + ID + " at " + pos);
		    if (pos < clientCount-1)
		    {
                for (int i = pos+1; i < clientCount; i++)
                {
                    clients[i-1] = clients[i];
                }
		    }
		    clientCount--;
		    try{  
		      	toTerminate.close(); 
		    }
		    catch(IOException ioe)
		    {  
		      	ui.jTextArea1.append("\nError closing thread: " + ioe); 
		    }
		    toTerminate.stop(); 
        }
    }
    
    // sisteme gelen client'�n thread'ini sistemde olu�turur ve ba�lat�r.
    private void addThread(Socket socket)
    {   //50 adet client kabul edebilirim.Dinamiktir degisebilir
		if (clientCount < clients.length)
		{  
	        ui.jTextArea1.append("\nClient accepted: " + socket);
		    clients[clientCount] = new ServerThread(this, socket); //yeni bir serverthread olu�turduk socket nesnesi verdik socket hangi �p den baglanacak o bilgileri tutar
		    try{  
		      	clients[clientCount].open();  
		        clients[clientCount].start();  
		        clientCount++; 
		        ui.jTextArea1.append("\nClient count: " + clientCount);

		    }
		    catch(IOException ioe) //ba�lamazsam
		    {
		      	ui.jTextArea1.append("\nError opening thread: " + ioe); 
		    } 
		}
		else{
	            ui.jTextArea1.append("\nClient refused: maximum " + clients.length + " reached.");
		}
    }


}


// server'da her client i�in ba�lat�lacak threadlerin s�n�f�d�r. clientlar�n bilgilerini tutar ve i�lerini yapar.
class ServerThread extends Thread //runnuble implement etseydik direk serverthread � �al��t�rd�g�m�z ilk anda cal�s�r. thread extend edince istedi�imiz zaman baslatm�� oluyoruz
{
	
    public Server server = null;	// Ba�lan�lacak Server'in objesi
    public Socket socket = null;	// Java.net.Socket k�t�phanesinin Socket nesnesi.
    public int ID = -1;				// Client'in ID'si
    public String username = "";	// Clienti'in kullan�c� ad�
    public ObjectInputStream streamIn  =  null;		// Client'in kullaca�� input stream objesi, gelecek mesajlar
    public ObjectOutputStream streamOut = null;		// Client'in kullaca�� output stream objesi, giden mesajlar
    public ServerForm ui;							// Server'�n aray�z nesnesi

    // ServerThread s�n�f�n�n constructor(yap�c�) metodu
    public ServerThread(Server _server, Socket _socket)
    {  
    	super(); //javada extend edilen class �n yap�c� fonksiyonunu cag�r�r.
        server = _server;
        socket = _socket;
        ID     = socket.getPort();
        ui = _server.ui;
        
    }
    
    // mesaj g�nderme
    public void send(Message msg)
    {
        try {
            streamOut.writeObject(msg);
            streamOut.flush();
        }
        catch (IOException ex) 
        {
            System.out.println("Exception [Client : send(...)]");
        }
    }
    
    // client'�n ID'sini getirme.
    public int getID()
    {
	    return ID;
    }
   
    // extend edilen Thread s�n�f�ndan override edilmi� run metodu
    // sonsuz d�ng�de client'e gelen mesajlar� streamIn den okur ve handle metodu ile i�ler.
    @Override
    @SuppressWarnings("deprecation")//deprecation:kullan�lm�yor anlam�nda javan�n�n warningi almamak i�in yap�lan birsey
	public void run()
    {
    	ui.jTextArea1.append("\nServer Thread " + ID + " running.");
        while (true) //s�rekli bagl� kals�n
        {  
    	    try{  
                Message msg = (Message) streamIn.readObject();//yeni bir mesaj geldi ve okundu
    	    	server.handle(ID, msg);                       //diger clientin �d sini ald� mesaj server a gitti oradan da al�c�ya gecer
            }
            catch(Exception ioe)
    	    {  
            	System.out.println(ID + " ERROR reading: " + ioe.getMessage());
                server.remove(ID);
                stop();
            }
        }
    }
    
    // Socket'in Input/Output streamlerini olu�turur.
    public void open() throws IOException 
    {
        streamOut = new ObjectOutputStream(socket.getOutputStream());
        streamOut.flush();
        streamIn = new ObjectInputStream(socket.getInputStream());
    }
    
    // Socket'in Input/Output streamlerini sonland�r�r.
    public void close() throws IOException 
    {
    	if (socket != null)    socket.close();
        if (streamIn != null)  streamIn.close();
        if (streamOut != null) streamOut.close();
    }
}