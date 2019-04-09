package com.server;

import com.ui.*;
import java.io.*;
import java.net.*;

// server sýnýfý ve o sýnýfýn implement ettiði Runnable arayüzü
// bu arayüzün sahip olduðu run() metodu implement edildi.
public class Server implements Runnable //sanal class sanal fonksiyon yazdýk run diye.
{
    
    public ServerThread clients[]; 				// server threads array for users.
    public ServerSocket server = null;			// java.net.socket object
    public Thread thread = null;				// server thread object
    public int clientCount = 0, port = 13000; 	// client count and server port 
    public ServerForm ui;					  	// server user interface
    public Users user;						 	// users 

    // Server Constructor 1 
    public Server(ServerForm form)
    {//bir dizi tanýmladýk 50 client baglanabilir. 
        clients = new ServerThread[50];
        ui = form; 
        user = new Users();
        
		try{ //portu verdi ,
		    server = new ServerSocket(port); 
	        port = server.getLocalPort();
		  //arayüzde baglanan kýsým
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
    
    // override Runnable interface method. Server sýnýfýný thread olarak çalýþtýrýr.
	@Override //runnable methodunu yeniden çalýþtýrýr
    public void run()
    {
		while (thread != null)
		{
	        try{  //java.net server socket isimli sýnýfýndan 
	        	ui.jTextArea1.append("\nWaiting for a client ..."); 
		        addThread(server.accept()); //server a bir socket baglanmasýný kabul ederim. 
		    }
		    catch(Exception ioe){ //kendi serverýn üzerinden 
                ui.jTextArea1.append("\nServer accept error: \n");
                ui.RetryStart(0);
		    }
	    }
    }
	
	// thread baþlat.Server a ait ise baþlat
    public void start()
    {  
    	if (thread == null)
    	{
            thread = new Thread(this); //server.java yý bir thread a attýk ve paketleyip calýþtýracak
            thread.start();//thread içine atýlan nesnenin içine gider içindeki nesnenin run metodunu cagýrýr.
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
    
    // Servere baðlanan bir istemcinin id sine göre istemciyi arama. istemci servere baðlý ise id si deðil ise -1 döner.
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
	
    
    // belirli bir istemciden gelen mesajlara senkronlaþtýrma(bknz. synchronized) yaparak cevap verir. mesajlarý iþler.
    // mesajýn türüne göre iþlemi devam ettirir. ID parametresi mesajýn gideceði istemciyi ifade eder. Message msg parametresi de iletilecek mesajdýr.
    public synchronized void handle(int ID, Message msg)
    {    	
    	if (msg.content.equals(".bye")) //
    	{
	            Announce("signout", "SERVER", msg.sender);//cýkan kiþiyi duyurur herkese
	            remove(ID); //uygulamadan kendi kullanýcýný kaldýrabilirisin 
		}else //çýkmak istemezse
		{
            if(msg.type.equals("login")) // login isteði
            {
            	if(findUserThread(msg.sender) == null)//login olmak isteyen kullanýcý o anda sistemde login mi?2. defa login olmasýn
                {                                     //type ý login olan mesajýn içeriði pass oluyor
                    if(user.checkLogin(msg.sender, msg.content)) // login iþleminde msg.sender=username msg.content=password
                    {
                        clients[findClient(ID)].username = msg.sender;
                        clients[findClient(ID)].send(new Message("login", "SERVER", "TRUE", msg.sender)); // login baþarýlý
                        Announce("newuser", "SERVER", msg.sender); 
                        sendUserList(msg.sender);
                    }else
                    {
                        clients[findClient(ID)].send(new Message("login", "SERVER", "FALSE", msg.sender)); // login baþarýsýz
                    } 
                }else
                {
                    clients[findClient(ID)].send(new Message("login", "SERVER", "FALSE", msg.sender)); // client in thread'i yoksa(null) login baþarýsýz
                }
            }else if(msg.type.equals("message")) // yeni mesaj gönderme
            {
                if(msg.recipient.equals("All")) // mesaj herkese gidecekse
                {
                    Announce("message", msg.sender, msg.content);
                }else // mesaj belirli bir kiþiye gidecekse
                {
                    findUserThread(msg.recipient).send(new Message(msg.type, msg.sender, msg.content, msg.recipient));
                    clients[findClient(ID)].send(new Message(msg.type, msg.sender, msg.content, msg.recipient));
                }
            }else if(msg.type.equals("test")) // test mesajý
            {
                clients[findClient(ID)].send(new Message("test", "SERVER", "OK", msg.sender));
            }else if(msg.type.equals("signup")) // yeni kullanýcý kaydý mesajý
            {
                if(findUserThread(msg.sender) == null)
                {
                    if(!user.userExists(msg.sender)) // kullanýcý kayýtlý deðilse
                    {
                        user.addUser(msg.sender, msg.content); // kullanýcýyý username ve pass ile kaydet.
                        clients[findClient(ID)].username = msg.sender;
                        clients[findClient(ID)].send(new Message("signup", "SERVER", "TRUE", msg.sender)); // önce kullanýcý kaydý yap
                        clients[findClient(ID)].send(new Message("login", "SERVER", "TRUE", msg.sender));  // sonra login ol
                        Announce("newuser", "SERVER", msg.sender);
                        sendUserList(msg.sender);
                    }else // kullanýcý sistemde kayýtlýysa
                    {
                        clients[findClient(ID)].send(new Message("signup", "SERVER", "FALSE", msg.sender)); // kullanýcý kayýtlýysa kayýt baþarýsýz.
                    }
                }else  // client in thread'i yoksa(null) signup baþarýsýz.
                {
                    clients[findClient(ID)].send(new Message("signup", "SERVER", "FALSE", msg.sender));
                }
            }else if(msg.type.equals("upload_req")) // dosya gönderme isteði
            {
                if(msg.recipient.equals("All")) // tüm kullanýcýlara
                {	// tüm kullanýcýlara dosya gönderme yasak mesajý SERVER tarafýndan dosya göndermek isteyen kullanýcýya gönderilir. -> "Uploading to 'All' forbidden"
                    clients[findClient(ID)].send(new Message("message", "SERVER", "Uploading to 'All' forbidden", msg.sender));
                }else // belirli bir kullanýcýya
                {	// dosya alýcýsýna dosya gönderme isteði(upload_req) türünden mesaj gönderilir.
                    findUserThread(msg.recipient).send(new Message("upload_req", msg.sender, msg.content, msg.recipient));
                }
            }else if(msg.type.equals("upload_res")) // dosya kabulü
            {
                if(!msg.content.equals("NO")) // dosya kabul edilmiþse.
                {
                    String IP = findUserThread(msg.sender).socket.getInetAddress().getHostAddress(); // gönderenin ip adresi
                    findUserThread(msg.recipient).send(new Message("upload_res", IP, msg.content, msg.recipient));
                }else
                {
                    findUserThread(msg.recipient).send(new Message("upload_res", msg.sender, msg.content, msg.recipient));
                }
            }
		}
    }
    
    // anons -> bütün kullanýcýlara mesaj gönderme metodu
    public void Announce(String type, String sender, String content)
    {
        Message msg = new Message(type, sender, content, "All");
        for(int i = 0; i < clientCount; i++)
        {
            clients[i].send(msg);
        }
    }
    
    // sisteme yeni bir kullanýcýnýn geldiðini diðer kullanýcýlara server haber verir.
    public void sendUserList(String toWhom)
    {
        for(int i = 0; i < clientCount; i++)
        {
            findUserThread(toWhom).send(new Message("newuser", "SERVER", clients[i].username, toWhom));
        }
    }
    
    // sistemdeki kullanýcýnýn thread'ini username kullanarak bulur.
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
	
    // sistemden logout olan kullanýcýnýn thread objesini sonlandýrýr ve siler.
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
    
    // sisteme gelen client'ýn thread'ini sistemde oluþturur ve baþlatýr.
    private void addThread(Socket socket)
    {   //50 adet client kabul edebilirim.Dinamiktir degisebilir
		if (clientCount < clients.length)
		{  
	        ui.jTextArea1.append("\nClient accepted: " + socket);
		    clients[clientCount] = new ServerThread(this, socket); //yeni bir serverthread oluþturduk socket nesnesi verdik socket hangi ýp den baglanacak o bilgileri tutar
		    try{  
		      	clients[clientCount].open();  
		        clients[clientCount].start();  
		        clientCount++; 
		        ui.jTextArea1.append("\nClient count: " + clientCount);

		    }
		    catch(IOException ioe) //baðlamazsam
		    {
		      	ui.jTextArea1.append("\nError opening thread: " + ioe); 
		    } 
		}
		else{
	            ui.jTextArea1.append("\nClient refused: maximum " + clients.length + " reached.");
		}
    }


}


// server'da her client için baþlatýlacak threadlerin sýnýfýdýr. clientlarýn bilgilerini tutar ve iþlerini yapar.
class ServerThread extends Thread //runnuble implement etseydik direk serverthread ý çalýþtýrdýgýmýz ilk anda calýsýr. thread extend edince istediðimiz zaman baslatmýþ oluyoruz
{
	
    public Server server = null;	// Baðlanýlacak Server'in objesi
    public Socket socket = null;	// Java.net.Socket kütüphanesinin Socket nesnesi.
    public int ID = -1;				// Client'in ID'si
    public String username = "";	// Clienti'in kullanýcý adý
    public ObjectInputStream streamIn  =  null;		// Client'in kullacaðý input stream objesi, gelecek mesajlar
    public ObjectOutputStream streamOut = null;		// Client'in kullacaðý output stream objesi, giden mesajlar
    public ServerForm ui;							// Server'ýn arayüz nesnesi

    // ServerThread sýnýfýnýn constructor(yapýcý) metodu
    public ServerThread(Server _server, Socket _socket)
    {  
    	super(); //javada extend edilen class ýn yapýcý fonksiyonunu cagýrýr.
        server = _server;
        socket = _socket;
        ID     = socket.getPort();
        ui = _server.ui;
        
    }
    
    // mesaj gönderme
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
    
    // client'ýn ID'sini getirme.
    public int getID()
    {
	    return ID;
    }
   
    // extend edilen Thread sýnýfýndan override edilmiþ run metodu
    // sonsuz döngüde client'e gelen mesajlarý streamIn den okur ve handle metodu ile iþler.
    @Override
    @SuppressWarnings("deprecation")//deprecation:kullanýlmýyor anlamýnda javanýnýn warningi almamak için yapýlan birsey
	public void run()
    {
    	ui.jTextArea1.append("\nServer Thread " + ID + " running.");
        while (true) //sürekli baglý kalsýn
        {  
    	    try{  
                Message msg = (Message) streamIn.readObject();//yeni bir mesaj geldi ve okundu
    	    	server.handle(ID, msg);                       //diger clientin ýd sini aldý mesaj server a gitti oradan da alýcýya gecer
            }
            catch(Exception ioe)
    	    {  
            	System.out.println(ID + " ERROR reading: " + ioe.getMessage());
                server.remove(ID);
                stop();
            }
        }
    }
    
    // Socket'in Input/Output streamlerini oluþturur.
    public void open() throws IOException 
    {
        streamOut = new ObjectOutputStream(socket.getOutputStream());
        streamOut.flush();
        streamIn = new ObjectInputStream(socket.getInputStream());
    }
    
    // Socket'in Input/Output streamlerini sonlandýrýr.
    public void close() throws IOException 
    {
    	if (socket != null)    socket.close();
        if (streamIn != null)  streamIn.close();
        if (streamOut != null) streamOut.close();
    }
}