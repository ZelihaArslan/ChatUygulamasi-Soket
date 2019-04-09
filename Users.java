package com.server;

import java.util.HashMap;

public class Users {
	
	public  HashMap<String,String> users;
	
	// constructor
	public Users() 
	{
		// �nceden tan�ml� kullan�c�lar
		users = new HashMap<String, String>();
		//users.put("Reyhan", "123");
	   //users.put("Zeliha", "234");
	}
	
	// bu kullan�c� sistemde kay�tl� m� ?
	public boolean userExists(String username)
	{
		if(users.containsKey(username))
		{
			return true;
		}else {
			return false;
		}	
	}
	
	
	// kullan�c� ad� ve parola kontrol�
	public boolean checkLogin(String username, String password)
	{
		if(users.containsKey(username) && users.containsValue(password))
		{
			return true;
		}else {
			return false;
		}
	}
	
	//kullan�c� ekle
	public void addUser(String username, String password)
	{
		users.put(username, password);
	}
	
}

