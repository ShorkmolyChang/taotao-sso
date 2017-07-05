package com.taotao.sso.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class PageController {
	
//	展示注册页面
	@RequestMapping("/user/showRegister")
	public String showRegister(){
		return "register";
	}
	
	@RequestMapping("/user/showLogin")
	public String showLogin(String redirectURL,Model model){
//		需要将redirectURL传递给jsp
		model.addAttribute("redirect",redirectURL);
		return "login";
	}

}
