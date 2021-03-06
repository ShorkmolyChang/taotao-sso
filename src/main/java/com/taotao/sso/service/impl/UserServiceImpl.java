package com.taotao.sso.service.impl;

import java.util.Date;


import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import com.taotao.common.pojo.TaotaoResult;
import com.taotao.common.utils.CookieUtils;
import com.taotao.common.utils.JsonUtils;
import com.taotao.mapper.TbUserMapper;
import com.taotao.pojo.TbUser;
import com.taotao.pojo.TbUserExample;
import com.taotao.pojo.TbUserExample.Criteria;
import com.taotao.sso.component.JedisClient;
import com.taotao.sso.service.UserService;

@Service
public class UserServiceImpl implements UserService {

	@Autowired
	private TbUserMapper userMapper;
	@Autowired
	private JedisClient jedisClient;
	@Value("${REDIS_SESSION_KEY}")
	private String REDIS_SESSION_KEY;
	@Value("${SESSION_EXPIRE}")
	private int SESSION_EXPIRE;

	@Override
	public boolean checkData(String param, int type) {
		// TODO Auto-generated method stub
		// 根据数据类型检查数据
		TbUserExample example = new TbUserExample();
		Criteria criteria = example.createCriteria();
		// type=1,2,3 分别代表username,phone,email
		if (type == 1) {
			criteria.andUsernameEqualTo(param);
		} else if (type == 2) {
			criteria.andPhoneEqualTo(param);
		} else if (type == 3) {
			criteria.andEmailEqualTo(param);
		} else {
			return false;
		}
		List<TbUser> users = userMapper.selectByExample(example);
		if (users == null || users.size() <= 0)
			return true;
		return false;
	}

	@Override
	public TaotaoResult register(TbUser user) {
		// TODO Auto-generated method stub

		// 校验数据
		// 用户名密码不能为空
		if (StringUtils.isBlank(user.getUsername()) || StringUtils.isBlank(user.getPassword()))
			return TaotaoResult.build(400, "用户名和密码不能为空");
		// 校验用户名、手机号、邮箱是否重复注册
		if (!checkData(user.getUsername(), 1))
			return TaotaoResult.build(400, "用户民重复");
		if (user.getPhone() != null && !checkData(user.getPhone(), 2))
			return TaotaoResult.build(400, "手机号重复");
		if (user.getEmail() != null && !checkData(user.getEmail(), 3))
			return TaotaoResult.build(400, "邮箱重复");

		// 插入数据
		user.setPassword(DigestUtils.md5DigestAsHex(user.getPassword().getBytes()));
		user.setCreated(new Date());
		user.setUpdated(new Date());
		userMapper.insert(user);
		return TaotaoResult.ok();
	}

	@Override
	public TaotaoResult login(String username, String password, HttpServletRequest request,
			HttpServletResponse response) {
		// TODO Auto-generated method stub
		// 校验用户名密码是否正确
		TbUserExample example = new TbUserExample();
		Criteria criteria = example.createCriteria();
		criteria.andUsernameEqualTo(username);
		List<TbUser> users = userMapper.selectByExample(example);
		if (users == null || users.isEmpty())
			return TaotaoResult.build(400, "用户名或密码错误");
		TbUser user = users.get(0);
		// 验证密码是否正确
		if (!user.getPassword().equals(DigestUtils.md5DigestAsHex(password.getBytes())))
			return TaotaoResult.build(400, "用户名或密码错误");

		// 登陆成功，生成token
		String token = UUID.randomUUID().toString();
		// 将用户信息写入redis缓存
		// key:REDIS_SESSION:{TOKEN}
		// value:user转json
		// 安全考虑，密码设置为空
		user.setPassword(null);
		jedisClient.set(REDIS_SESSION_KEY + ":" + token, JsonUtils.objectToJson(user));
		// 设置session的过期时间
		jedisClient.expire(REDIS_SESSION_KEY + ":" + token, SESSION_EXPIRE);
		// 写入cookie,默认过期时间:关闭浏览器cookie过期
		CookieUtils.setCookie(request, response, "TT_TOKEN", token);
		return TaotaoResult.ok(token);
	}

	@Override
	public TaotaoResult getUserByToken(String token) {
		// TODO Auto-generated method stub
		// 根据token取用户信息
		String json = jedisClient.get(REDIS_SESSION_KEY + ":" + token);
		// 判断是否为null
		if (StringUtils.isBlank(json)) {
			return TaotaoResult.build(400, "用户session已经过期");
		}

		// 查询到用户信息，将json转java对象
		TbUser user = JsonUtils.jsonToPojo(json, TbUser.class);
		// 更新session的过期时间
		jedisClient.expire(REDIS_SESSION_KEY + ":" + token, SESSION_EXPIRE);
		return TaotaoResult.ok(user);
	}

	@Override
	public TaotaoResult deleteToken(String token) {
		// TODO Auto-generated method stub
		jedisClient.del(REDIS_SESSION_KEY+":"+token);
		return TaotaoResult.ok();
	}

}
