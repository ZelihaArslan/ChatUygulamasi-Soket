package com.client;

import com.server.Message;
import com.ui.ClientForm;

import java.io.*;
import java.net.*;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

// Client s�n�f� Runnable aray�z�n� implement ederek
// bu aray�z�n sahip oldu�u run() metodu implement edildi.
public class Client implements Runnable
{
    
    public int port;				// ba�lan�lacak port
    public String serverAddr;		// server adresi
    public Socket socket;			// ba�lant�y� yapacak java.net.Socket api si objesi
    public ClientForm ui;			// Client aray�z nesnesi
    public ObjectInputStream In;	// Client Input Stream
    public ObjectOutputStream Out;	// Client Output Stream
    
    // Client Constructor
    public Client(ClientForm form) throws IOException{
        ui = form;
        this.serverAddr = ui.serverAddr;
        this.port = ui.port;
        socket = new Socket(InetAddress.getByName(serverAddr), port);
            
        Out = new ObjectOutputStream(socket.getOutputStream());
        Out.flush();
        In = new ObjectInputStream(socket.getInputStream());
        
    }

    // Runnable implemented method
    @Override
    public void run() 
    {
    	boolean isRunning = true;
        while(isRunning)
        {
            try {
            	
            	// Gelen mesaj Input Stream den s�rekli okunur.
                Message msg = (Message) In.readObject();
                System.out.println("Incoming : "+msg.toString());
                
                // mesaj t�r� "message" ise
                if(msg.type.equals("message"))
                {
                	// gelen mesaj bu Client'e aitse aray�zde g�sterir.
                    if(msg.recipient.equals(ui.username))
                    {
                        ui.jTextArea1.append("["+msg.sender +" > Me] : " + msg.content + "\n");
                    }// ba�ka client ise
                    else
                    {
                        ui.jTextArea1.append("["+ msg.sender +" > "+ msg.recipient +"] : " + msg.content + "\n");
                    }
                                            
                }// mesaj tipi login
                else if(msg.type.equals("login"))
                {
                	// mesaj i�eri�i "TRUE" ise
                    if(msg.content.equals("TRUE"))
                    {//send ile send message butonlar� �al���r
                        ui.jButton2.setEnabled(false); ui.jButton3.setEnabled(false);                       
                        ui.jButton4.setEnabled(true); ui.jButton5.setEnabled(true);
                        ui.jTextArea1.append("[SERVER > Me] : Login Successful\n");
                        ui.jTextField3.setEnabled(false); ui.jPasswordField1.setEnabled(false);
                    }// mesaj i�eri�i "FALSE" ise
                    else
                    {
                        ui.jTextArea1.append("[SERVER > Me] : Login Failed\n");
                    }
                }// mesaj tipi test
                else if(msg.type.equals("test"))
                {
                    ui.jButton1.setEnabled(false);
                    ui.jButton2.setEnabled(true); ui.jButton3.setEnabled(true);
                    ui.jTextField3.setEnabled(true); ui.jPasswordField1.setEnabled(true);
                    ui.jTextField1.setEditable(false); ui.jTextField2.setEditable(false);
                }// mesaj tipi newuser
                else if(msg.type.equals("newuser"))
                {
                	// ilgili client'e ait user de�ilse. ba�ka bir user ise.
                    if(!msg.content.equals(ui.username))
                    {
                    	// model o anda sistemde online user listesini tutan nesne
                        boolean exists = false;
                        for(int i = 0; i < ui.model.getSize(); i++)
                        {
                            if(ui.model.getElementAt(i).equals(msg.content))
                            {
                                exists = true; 
                                break;
                            }
                        }
                        // user listede yoksa ekle
                        if(!exists){ ui.model.addElement(msg.content); }
                    }
                } // mesaj tipi "ekle"
                else if(msg.type.equals("signup"))
                {
                	// signup server taraf�ndan kabul edilmi�se
                    if(msg.content.equals("TRUE"))
                    {
                        ui.jButton2.setEnabled(false); ui.jButton3.setEnabled(false);
                        ui.jButton4.setEnabled(true); ui.jButton5.setEnabled(true);
                        ui.jTextArea1.append("[SERVER > Me] : Singup Successful\n");
                    }// signup server taraf�ndan kabul edilmemi�se
                    else{
                        ui.jTextArea1.append("[SERVER > Me] : Signup Failed\n");
                    }
                } // mesaj tipi "signout"
                else if(msg.type.equals("signout"))
                {
                	// signout yapacak kullan�c� bu kullan�c� ise
                    if(msg.content.equals(ui.username))
                    {
                        ui.jTextArea1.append("["+ msg.sender +" > Me] : Bye\n");
                        ui.jButton1.setEnabled(true); ui.jButton4.setEnabled(false); 
                        ui.jTextField1.setEditable(true); ui.jTextField2.setEditable(true);
                        
                        // Di�er kullan�c�lar i�in aray�zdeki kullan�c� listesinden bu kullan�c�n�n ad� signout oldu�u i�in silinir.
                        for(int i = 1; i < ui.model.size(); i++)
                        {
                            ui.model.removeElementAt(i);
                        }
                        // bu kullan�c�ya ait thread sonland�r�l�r.
                        ui.clientThread.stop();
                    }// bu kullan�c� signout olmam��sa
                    else{
                    	// kullan�c� listesinden sgnout olan kullan�c� silinir.
                        ui.model.removeElement(msg.content);
                        ui.jTextArea1.append("["+ msg.sender +" > All] : "+ msg.content +" has signed out\n");
                    }
                } // mesaj tipi "upload_req" -> ba�ka bir kullan�c�dan dosya gelmesi durumu
                else if(msg.type.equals("upload_req"))
                {
                	// ba�ka bir kullan�c�dan bu kullan�c�ya bir dosya geliyorsa ekrana dosya kabul edilsin mi popup'� "JOptionPane" a��l�r.
                    if(JOptionPane.showConfirmDialog(ui, ("Accept '"+msg.content+"' from "+msg.sender+" ?")) == 0) //kabul ettiysen
                    {
                    	// file chooser ile dosyan�n kay�t edilece�i yer se�ilir.
                        JFileChooser jf = new JFileChooser(); //dosyay� sececegim ekran
                        jf.setSelectedFile(new File(msg.content));
                        int returnVal = jf.showSaveDialog(ui);
                       
                        // al�nan dosyan�n kay�t edilece�i yer saveTo de�i�keninde tutulur.
                        String saveTo = jf.getSelectedFile().getPath();
                        if(saveTo != null && returnVal == JFileChooser.APPROVE_OPTION)
                        {
                        	// Download i�ini yapacak s�n�f�fn nesnesi dosyan�n  kay�t edilece�i yol ve ui nesnelerini alarak olu�turulur.
                            Download dwn = new Download(saveTo, ui);
                            // Download nesnesi ile thread olu�turulup ba�lat�l�r ve dosyay� g�nderen client'e upload_res mesaj� bilgi(dosya download ediliyor) olarak g�nderilir.
                            Thread t = new Thread(dwn);
                            t.start();
                            send(new Message("upload_res", ui.username, (""+dwn.port), msg.sender));
                        }// e�er dosya al�m� kabul edilmemi�se dosyay� g�nderene "NO" mesaj� g�nderilir.
                        else
                        {
                            send(new Message("upload_res", ui.username, "NO", msg.sender));
                        }
                    }
                    else
                    { // dosya geldi�inde ekranda a��lan kabul etme popup � red edilmi�se.
                        send(new Message("upload_res", ui.username, "NO", msg.sender));
                    }
                } // mesaj tipi "upload_res" -> ba�ka bir kullan�c�ya dosya g�nderilmesi sonras� dosyay� alan kullan�c�n�n verdi�i cevap.
                else if(msg.type.equals("upload_res"))
                {
                	// dosyay� alan dosyay� kabul etmi�se
                    if(!msg.content.equals("NO"))
                    {
                    	// dosyay� kabul eden kullan�c�n�n dosyay� alaca�� port ve kullan�c� ad� al�n�r.
                        int port  = Integer.parseInt(msg.content);
                        String addr = msg.sender;
                        //upload tan�mlama bilgileri
                        ui.jButton5.setEnabled(false); 
                        ui.jButton6.setEnabled(false);
                        // g�nderilecek dosya, kullan�c� ad� ve port kullan�larak dosyay� g�nderecek Upload s�n�f� bir thread ile ba�lat�l�r.
                        Upload upl = new Upload(addr, port, ui.file, ui);
                        Thread t = new Thread(upl);
                        t.start();
                    } // kullan�c� dosyay� almay� kabul etmemi�se
                    else
                    {
                        ui.jTextArea1.append("[SERVER > Me] : "+msg.sender+" rejected file request\n");
                    }
                } // gelen mesaj belirli mesaj tiplerinden hi�biri ile e�le�miyorsa
                else{
                    ui.jTextArea1.append("[SERVER > Me] : Unknown message type\n");
                }
            }
            catch(Exception ex) // T�m bu i�lemler s�ras�nda hata olu�mu�sa 
            {
            	// isRunning false yap�larak run metodu durdurulur ve ilgili hata bildirimleri yap�l�r.
                isRunning = false;
                ui.jTextArea1.append("[Application > Me] : Connection Failure\n");
                ui.jButton1.setEnabled(true); ui.jTextField1.setEditable(true); ui.jTextField2.setEditable(true);
                ui.jButton4.setEnabled(false); ui.jButton5.setEnabled(false); ui.jButton5.setEnabled(false);
                
                // hata olu�an client online client listesinden silinir.
                for(int i = 1; i < ui.model.size(); i++)
                {
                    ui.model.removeElementAt(i);
                }
                
                // o client'e ait thread durdurulur.
                ui.clientThread.stop();
                
                System.out.println("Exception Client run()");
                ex.printStackTrace();
            }
        }
    }
    
    // client'�n mesaj g�ndermek i�in kulland��� metod
    public void send(Message msg)
    {
        try 
        {
        	// mesaj output stream'a yaz�l�r.
            Out.writeObject(msg);
            Out.flush();
            System.out.println("Outgoing : "+msg.toString());
    
        } 
        catch (IOException ex) {
            System.out.println("Exception Client send()");
        }
    }
      
    
    // Thread �zerinde �al��abilir Download s�n�f�
    public class Download implements Runnable
    {  
        public ServerSocket server;
        public Socket socket;
        public int port;
        public String saveTo = "";
        public InputStream In;
        public FileOutputStream Out;
        public ClientForm ui;
        
        public Download(String saveTo, ClientForm ui)
        {
            try {
                server = new ServerSocket(0);
                port = server.getLocalPort();
                this.saveTo = saveTo;
                this.ui = ui;
            } 
            catch (IOException ex) {
                System.out.println("Exception [Download : Download(...)]");
            }
        }

        // Thread olarak ba�lat�ld���n i�leme al�nan metod
        @Override
        public void run() 
        {
            try {
                socket = server.accept();
                System.out.println("Download : "+socket.getRemoteSocketAddress());
                
                // Sockete gelen byte'lar�n tutuldu�u "In" InputStream nesnesi.
                In = socket.getInputStream();
                // gelen byte'lar�n kay�t edilece�i "Out" OutputStream nesnesi. Out nesnesi dosyan�n kay�t edilece�i yer ile ba�lat�l�r. Bu �ekilde write ile ilgili yere yaz�l�r
                Out = new FileOutputStream(saveTo); //kaydedilen yer
                
                // byte'lar�n tutulaca�� buffer
                byte[] buffer = new byte[1024];
                int count;
                
                // Socketten byte geldi -> "In" nesnesine d��t� -> "In" nesnesinden okundu -> "Out" nesnesine yaz�ld�. out=kaydedilecek dosya yolu
                while((count = In.read(buffer)) >= 0){
                    Out.write(buffer, 0, count);//dosyay� kaydedilecek yer
                }
                // Socket'ten al�nan byte'lar�n kay�t edildi�i ve yeni byte'lar�n gelmesi i�in "Out" u temizler.
                Out.flush();
                
                ui.jTextArea1.append("[Application > Me] : Download complete\n");
                // Download tamamlanmas�n�n ard�ndan socket, In ve Out nesneleri kapat�l�r.
                if(Out != null){ Out.close(); }
                if(In != null){ In.close(); }
                if(socket != null){ socket.close(); }
            } 
            catch (Exception ex) {
                System.out.println("Exception [Download : run(...)]");
            }
        }
    }
    
    // Thread �zerinde �al��abilir Upload s�n�f�
    public class Upload implements Runnable
    {

        public String addr;
        public int port;
        public Socket socket;
        public FileInputStream In;
        public OutputStream Out;
        public File file;
        public ClientForm ui;
        
        // Upload s�n�f� constructor
        public Upload(String addr, int port, File filepath, ClientForm form)
        {
            super();
            try {
                file = filepath;
                ui = form;
                socket = new Socket(InetAddress.getByName(addr), port);		// dosyan�n  gidece�i kullan�c�ya socket a��l�r.
                Out = socket.getOutputStream();								// socket'in outputstream nesnesi veri yazmak i�in (g�nderilecek dosyan�n byte'lar�) ayarlan�r.
                In = new FileInputStream(filepath);							// g�nderilecek dosya okumak i�in ayarlan�r.
            } 
            catch (Exception ex) {
                System.out.println("Exception [Upload : Upload(...)]");
            }
        }
        
        // Upload s�n�f�n� thread olarak �al��t���nda kullan�lan metod
        @Override
        public void run() 
        {
            try {
            	// buffer
                byte[] buffer = new byte[1024];
                int count;
                
                // filePath ile olu�turulan In objesinden okunan byte buffer'a al�n�r Out a yaz�l�r.
                while((count = In.read(buffer)) >= 0){
                    Out.write(buffer, 0, count); //dosyay� alan ki�i okuyacak
                }
                Out.flush();
                
                // Upload tamamland� bilgileri
                ui.jTextArea1.append("[Applcation > Me] : File upload complete\n");
                ui.jButton5.setEnabled(true);
                ui.jButton6.setEnabled(true);
                ui.jTextField5.setVisible(true);
                
                // Upload tamamlanmas�n�n ard�ndan socket, In ve Out nesneleri kapat�l�r.
                if(In != null){ In.close(); }
                if(Out != null){ Out.close(); }
                if(socket != null){ socket.close(); }
            }
            catch (Exception ex) {
                System.out.println("Exception [Upload : run()]");
                ex.printStackTrace();
            }
        }

    }
    
}
