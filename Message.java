package com.server;

import java.io.Serializable;

// Serializable interface'i bu sýnýfýn nesnelerini byte olarak I/O Stream a yazdýðýmýzda nesne içindeki deðiþkenlerin tiplerini korur.
public class Message implements Serializable
{
    
	/**
	 * serializable version
	 */
	private static final long serialVersionUID = 1L;

    // mesaj yapýsý : tip - gönderici - alýcý - mesaj içeriði
    public String type, sender, content, recipient;
    
    // Message Constructor
    public Message(String type, String sender, String content, String recipient){
        this.type = type;
        this.sender = sender;
        this.content = content;
        this.recipient = recipient;
    }
    
    // Bu sýnýfýn objesine toString() dediðimizde aþaðýdaki formatta String döndürür.
    @Override
    public String toString(){
        return "{type='"+type+"', sender='"+sender+"', content='"+content+"', recipient='"+recipient+"'}";
    }
}