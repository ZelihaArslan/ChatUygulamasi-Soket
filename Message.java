package com.server;

import java.io.Serializable;

// Serializable interface'i bu s�n�f�n nesnelerini byte olarak I/O Stream a yazd���m�zda nesne i�indeki de�i�kenlerin tiplerini korur.
public class Message implements Serializable
{
    
	/**
	 * serializable version
	 */
	private static final long serialVersionUID = 1L;

    // mesaj yap�s� : tip - g�nderici - al�c� - mesaj i�eri�i
    public String type, sender, content, recipient;
    
    // Message Constructor
    public Message(String type, String sender, String content, String recipient){
        this.type = type;
        this.sender = sender;
        this.content = content;
        this.recipient = recipient;
    }
    
    // Bu s�n�f�n objesine toString() dedi�imizde a�a��daki formatta String d�nd�r�r.
    @Override
    public String toString(){
        return "{type='"+type+"', sender='"+sender+"', content='"+content+"', recipient='"+recipient+"'}";
    }
}