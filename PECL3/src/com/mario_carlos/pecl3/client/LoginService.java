package com.mario_carlos.pecl3.client;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import com.mario_carlos.pecl3.shared.Libro;

@RemoteServiceRelativePath("login")
public interface LoginService extends RemoteService{
	public LoginInfo login(String requestUri);
	public Libro insert(Libro libro);
}
