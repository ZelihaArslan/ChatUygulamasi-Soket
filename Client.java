package com.client;

import com.server.Message;
import com.ui.ClientForm;

import java.io.*;
import java.net.*;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

// Client sýnýfý Runnable arayüzünü implement ederek
// bu arayüzün sahip olduðu run() metodu implement edildi.
public class Client implements Runnable
{
    
    public int port;				// baðlanýlacak port
    public String serverAddr;		// server adresi
    public Socket socket;			// baðlantýyý yapacak java.net.Socket api si objesi
    public ClientForm ui;			// Client arayüz nesnesi
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
            	
            	// Gelen mesaj Input Stream den sürekli okunur.
                Message msg = (Message) In.readObject();
                System.out.println("Incoming : "+msg.toString());
                
                // mesaj türü "message" ise
                if(msg.type.equals("message"))
                {
                	// gelen mesaj bu Client'e aitse arayüzde gösterir.
                    if(msg.recipient.equals(ui.username))
                    {
                        ui.jTextArea1.append("["+msg.sender +" > Me] : " + msg.content + "\n");
                    }// baþka client ise
                    else
                    {
                        ui.jTextArea1.append("["+ msg.sender +" > "+ msg.recipient +"] : " + msg.content + "\n");
                    }
                                            
                }// mesaj tipi login
                else if(msg.type.equals("login"))
                {
                	// mesaj içeriði "TRUE" ise
                    if(msg.content.equals("TRUE"))
                    {//send ile send message butonlarý çalýþýr
                        ui.jButton2.setEnabled(false); ui.jButton3.setEnabled(false);                       
                        ui.jButton4.setEnabled(true); ui.jButton5.setEnabled(true);
                        ui.jTextArea1.append("[SERVER > Me] : Login Successful\n");
                        ui.jTextField3.setEnabled(false); ui.jPasswordField1.setEnabled(false);
                    }// mesaj içeriði "FALSE" ise
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
                	// ilgili client'e ait user deðilse. baþka bir user ise.
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
                	// signup server tarafýndan kabul edilmiþse
                    if(msg.content.equals("TRUE"))
                    {
                        ui.jButton2.setEnabled(false); ui.jButton3.setEnabled(false);
                        ui.jButton4.setEnabled(true); ui.jButton5.setEnabled(true);
                        ui.jTextArea1.append("[SERVER > Me] : Singup Successful\n");
                    }// signup server tarafýndan kabul edilmemiþse
                    else{
                        ui.jTextArea1.append("[SERVER > Me] : Signup Failed\n");
                    }
                } // mesaj tipi "signout"
                else if(msg.type.equals("signout"))
                {
                	// signout yapacak kullanýcý bu kullanýcý ise
                    if(msg.content.equals(ui.username))
                    {
                        ui.jTextArea1.append("["+ msg.sender +" > Me] : Bye\n");
                        ui.jButton1.setEnabled(true); ui.jButton4.setEnabled(false); 
                        ui.jTextField1.setEditable(true); ui.jTextField2.setEditable(true);
                        
                        // Diðer kullanýcýlar için arayüzdeki kullanýcý listesinden bu kullanýcýnýn adý signout olduðu için silinir.
                        for(int i = 1; i < ui.model.size(); i++)
                        {
                            ui.model.removeElementAt(i);
                        }
                        // bu kullanýcýya ait thread sonlandýrýlýr.
                        ui.clientThread.stop();
                    }// bu kullanýcý signout olmamýþsa
                    else{
                    	// kullanýcý listesinden sgnout olan kullanýcý silinir.
                        ui.model.removeElement(msg.content);
                        ui.jTextArea1.append("["+ msg.sender +" > All] : "+ msg.content +" has signed out\n");
                    }
                } // mesaj tipi "upload_req" -> baþka bir kullanýcýdan dosya gelmesi durumu
                else if(msg.type.equals("upload_req"))
                {
                	// baþka bir kullanýcýdan bu kullanýcýya bir dosya geliyorsa ekrana dosya kabul edilsin mi popup'ý "JOptionPane" açýlýr.
                    if(JOptionPane.showConfirmDialog(ui, ("Accept '"+msg.content+"' from "+msg.sender+" ?")) == 0) //kabul ettiysen
                    {
                    	// file chooser ile dosyanýn kayýt edileceði yer seçilir.
                        JFileChooser jf = new JFileChooser(); //dosyayý sececegim ekran
                        jf.setSelectedFile(new File(msg.content));
                        int returnVal = jf.showSaveDialog(ui);
                       
                        // alýnan dosyanýn kayýt edileceði yer saveTo deðiþkeninde tutulur.
                        String saveTo = jf.getSelectedFile().getPath();
                        if(saveTo != null && returnVal == JFileChooser.APPROVE_OPTION)
                        {
                        	// Download iþini yapacak sýnýfýfn nesnesi dosyanýn  kayýt edileceði yol ve ui nesnelerini alarak oluþturulur.
                            Download dwn = new Download(saveTo, ui);
                            // Download nesnesi ile thread oluþturulup baþlatýlýr ve dosyayý gönderen client'e upload_res mesajý bilgi(dosya download ediliyor) olarak gönderilir.
                            Thread t = new Thread(dwn);
                            t.start();
                            send(new Message("upload_res", ui.username, (""+dwn.port), msg.sender));
                        }// eðer dosya alýmý kabul edilmemiþse dosyayý gönderene "NO" mesajý gönderilir.
                        else
                        {
                            send(new Message("upload_res", ui.username, "NO", msg.sender));
                        }
                    }
                    else
                    { // dosya geldiðinde ekranda açýlan kabul etme popup ý red edilmiþse.
                        send(new Message("upload_res", ui.username, "NO", msg.sender));
                    }
                } // mesaj tipi "upload_res" -> baþka bir kullanýcýya dosya gönderilmesi sonrasý dosyayý alan kullanýcýnýn verdiði cevap.
                else if(msg.type.equals("upload_res"))
                {
                	// dosyayý alan dosyayý kabul etmiþse
                    if(!msg.content.equals("NO"))
                    {
                    	// dosyayý kabul eden kullanýcýnýn dosyayý alacaðý port ve kullanýcý adý alýnýr.
                        int port  = Integer.parseInt(msg.content);
                        String addr = msg.sender;
                        //upload tanýmlama bilgileri
                        ui.jButton5.setEnabled(false); 
                        ui.jButton6.setEnabled(false);
                        // gönderilecek dosya, kullanýcý adý ve port kullanýlarak dosyayý gönderecek Upload sýnýfý bir thread ile baþlatýlýr.
                        Upload upl = new Upload(addr, port, ui.file, ui);
                        Thread t = new Thread(upl);
                        t.start();
                    } // kullanýcý dosyayý almayý kabul etmemiþse
                    else
                    {
                        ui.jTextArea1.append("[SERVER > Me] : "+msg.sender+" rejected file request\n");
                    }
                } // gelen mesaj belirli mesaj tiplerinden hiçbiri ile eþleþmiyorsa
                else{
                    ui.jTextArea1.append("[SERVER > Me] : Unknown message type\n");
                }
            }
            catch(Exception ex) // Tüm bu iþlemler sýrasýnda hata oluþmuþsa 
            {
            	// isRunning false yapýlarak run metodu durdurulur ve ilgili hata bildirimleri yapýlýr.
                isRunning = false;
                ui.jTextArea1.append("[Application > Me] : Connection Failure\n");
                ui.jButton1.setEnabled(true); ui.jTextField1.setEditable(true); ui.jTextField2.setEditable(true);
                ui.jButton4.setEnabled(false); ui.jButton5.setEnabled(false); ui.jButton5.setEnabled(false);
                
                // hata oluþan client online client listesinden silinir.
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
    
    // client'ýn mesaj göndermek için kullandýðý metod
    public void send(Message msg)
    {
        try 
        {
        	// mesaj output stream'a yazýlýr.
            Out.writeObject(msg);
            Out.flush();
            System.out.println("Outgoing : "+msg.toString());
    
        } 
        catch (IOException ex) {
            System.out.println("Exception Client send()");
        }
    }
      
    
    // Thread üzerinde çalýþabilir Download sýnýfý
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

        // Thread olarak baþlatýldýðýn iþleme alýnan metod
        @Override
        public void run() 
        {
            try {
                socket = server.accept();
                System.out.println("Download : "+socket.getRemoteSocketAddress());
                
                // Sockete gelen byte'larýn tutulduðu "In" InputStream nesnesi.
                In = socket.getInputStream();
                // gelen byte'larýn kayýt edileceði "Out" OutputStream nesnesi. Out nesnesi dosyanýn kayýt edileceði yer ile baþlatýlýr. Bu þekilde write ile ilgili yere yazýlýr
                Out = new FileOutputStream(saveTo); //kaydedilen yer
                
                // byte'larýn tutulacaðý buffer
                byte[] buffer = new byte[1024];
                int count;
                
                // Socketten byte geldi -> "In" nesnesine düþtü -> "In" nesnesinden okundu -> "Out" nesnesine yazýldý. out=kaydedilecek dosya yolu
                while((count = In.read(buffer)) >= 0){
                    Out.write(buffer, 0, count);//dosyayý kaydedilecek yer
                }
                // Socket'ten alýnan byte'larýn kayýt edildiði ve yeni byte'larýn gelmesi için "Out" u temizler.
                Out.flush();
                
                ui.jTextArea1.append("[Application > Me] : Download complete\n");
                // Download tamamlanmasýnýn ardýndan socket, In ve Out nesneleri kapatýlýr.
                if(Out != null){ Out.close(); }
                if(In != null){ In.close(); }
                if(socket != null){ socket.close(); }
            } 
            catch (Exception ex) {
                System.out.println("Exception [Download : run(...)]");
            }
        }
    }
    
    // Thread üzerinde çalýþabilir Upload sýnýfý
    public class Upload implements Runnable
    {

        public String addr;
        public int port;
        public Socket socket;
        public FileInputStream In;
        public OutputStream Out;
        public File file;
        public ClientForm ui;
        
        // Upload sýnýfý constructor
        public Upload(String addr, int port, File filepath, ClientForm form)
        {
            super();
            try {
                file = filepath;
                ui = form;
                socket = new Socket(InetAddress.getByName(addr), port);		// dosyanýn  gideceði kullanýcýya socket açýlýr.
                Out = socket.getOutputStream();								// socket'in outputstream nesnesi veri yazmak için (gönderilecek dosyanýn byte'larý) ayarlanýr.
                In = new FileInputStream(filepath);							// gönderilecek dosya okumak için ayarlanýr.
            } 
            catch (Exception ex) {
                System.out.println("Exception [Upload : Upload(...)]");
            }
        }
        
        // Upload sýnýfýný thread olarak çalýþtýðýnda kullanýlan metod
        @Override
        public void run() 
        {
            try {
            	// buffer
                byte[] buffer = new byte[1024];
                int count;
                
                // filePath ile oluþturulan In objesinden okunan byte buffer'a alýnýr Out a yazýlýr.
                while((count = In.read(buffer)) >= 0){
                    Out.write(buffer, 0, count); //dosyayý alan kiþi okuyacak
                }
                Out.flush();
                
                // Upload tamamlandý bilgileri
                ui.jTextArea1.append("[Applcation > Me] : File upload complete\n");
                ui.jButton5.setEnabled(true);
                ui.jButton6.setEnabled(true);
                ui.jTextField5.setVisible(true);
                
                // Upload tamamlanmasýnýn ardýndan socket, In ve Out nesneleri kapatýlýr.
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
