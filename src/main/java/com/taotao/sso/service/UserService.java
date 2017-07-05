package com.taotao.sso.service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.taotao.common.pojo.TaotaoResult;
import com.taotao.pojo.TbUser;

public interface UserService {
	
	
	boolean checkData(String param,int type);
	
	TaotaoResult register(TbUser user);
	
	TaotaoResult login(String username,String password,HttpServletRequest request,HttpServletResponse response);

	TaotaoResult getUserByToken(String token);
	
	TaotaoResult deleteToken(String token);
	
}
