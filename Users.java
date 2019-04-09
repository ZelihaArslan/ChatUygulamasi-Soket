package com.server;

import java.util.HashMap;

public class Users {
	
	public  HashMap<String,String> users;
	
	// constructor
	public Users() 
	{
		// önceden tanýmlý kullanýcýlar
		users = new HashMap<String, String>();
		//users.put("Reyhan", "123");
	   //users.put("Zeliha", "234");
	}
	
	// bu kullanýcý sistemde kayýtlý mý ?
	public boolean userExists(String username)
	{
		if(users.containsKey(username))
		{
			return true;
		}else {
			return false;
		}	
	}
	
	
	// kullanýcý adý ve parola kontrolü
	public boolean checkLogin(String username, String password)
	{
		if(users.containsKey(username) && users.containsValue(password))
		{
			return true;
		}else {
			return false;
		}
	}
	
	//kullanýcý ekle
	public void addUser(String username, String password)
	{
		users.put(username, password);
	}
	
}

